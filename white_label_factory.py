import os
import json
import requests
import subprocess
import firebase_admin
from firebase_admin import credentials, firestore, storage
from PIL import Image
from io import BytesIO

# --- Configuration ---
PROJECT_ROOT = os.getcwd()
STRINGS_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/values/strings.xml")
RES_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res")

def init_firebase():
    print("--- Initializing Firebase Admin SDK ---")
    service_account_json = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
    if not service_account_json:
        print("CRITICAL: FIREBASE_SERVICE_ACCOUNT not found in environment variables!")
        print("Please add it to GitHub Secrets. Skipping Firestore updates.")
        return None, None
    
    try:
        cred_dict = json.loads(service_account_json)
        print(f"Service account found for project: {cred_dict.get('project_id')}")
        cred = credentials.Certificate(cred_dict)
        firebase_admin.initialize_app(cred, {
            'storageBucket': f"{cred_dict['project_id']}.appspot.com"
        })
        print("Firebase Admin SDK initialized successfully.")
        return firestore.client(), storage.bucket()
    except Exception as e:
        print(f"ERROR: Failed to initialize Firebase: {e}")
        return None, None

def update_app_config(store_id):
    print(f"--- Injecting Store ID: {store_id} into config.xml ---")
    config_dir = os.path.join(RES_PATH, "values")
    if not os.path.exists(config_dir):
        os.makedirs(config_dir)
    
    config_path = os.path.join(config_dir, "config.xml")
    config_content = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="target_store_id">{store_id}</string>
</resources>
"""
    with open(config_path, "w", encoding="utf-8") as f:
        f.write(config_content)

def update_app_name(app_name):
    if not app_name:
        return
    print(f"--- Updating app_name to: {app_name} ---")
    if os.path.exists(STRINGS_PATH):
        with open(STRINGS_PATH, "r", encoding="utf-8") as f:
            content = f.read()
        
        import re
        new_content = re.sub(r'<string name="app_name">.*?</string>', f'<string name="app_name">{app_name}</string>', content)
        
        with open(STRINGS_PATH, "w", encoding="utf-8") as f:
            f.write(new_content)

def run_gradle_build(user_app_name, delivery_app_name, store_app_name):
    print("--- Running Selected Gradle Builds (Customer, Store, Delivery) ---")
    gradle_cmd = "gradlew.bat" if os.name == 'nt' else "./gradlew"
    
    tasks = [
        (":app:assembleCustomerDebug", user_app_name),
        (":app:assembleStoreDebug", store_app_name),
        (":app:assembleDeliveryDebug", delivery_app_name)
    ]
    
    try:
        for task, app_name in tasks:
            print(f"Assigning name '{app_name}' for task: {task}...")
            update_app_name(app_name)
            subprocess.run([gradle_cmd, task, "--no-daemon", "--stacktrace"], check=True)
        return True
    except subprocess.CalledProcessError as e:
        print(f"Build failed during task execution. Error code: {e.returncode}")
        return False
    except Exception as e:
        print(f"Unexpected error during build: {e}")
        return False

def upload_and_update(store_id, db, bucket):
    print(f"--- Uploading APKs for Store: {store_id} to GitHub Releases ---")
    
    github_token = os.environ.get("GITHUB_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY") # Automatically set by Actions
    
    if not github_token or not repo:
        print("GITHUB_TOKEN or REPO not found. Falling back to local logging.")
        return

    # 1. Create a Tag/Release name
    tag_name = f"build-{store_id}-{int(os.path.getmtime(RES_PATH))}"
    
    # 2. Create Release via GitHub API
    headers = {
        "Authorization": f"token {github_token}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    release_data = {
        "tag_name": tag_name,
        "name": f"Build for Store {store_id}",
        "body": f"Automated white-label build for {store_id}",
        "draft": False,
        "prerelease": False
    }
    
    try:
        rel_resp = requests.post(f"https://api.github.com/repos/{repo}/releases", json=release_data, headers=headers)
        if rel_resp.status_code != 201:
            print(f"Failed to create release: {rel_resp.text}")
            return
        
        # Update Firestore early to 'APPROVED' so user isn't stuck waiting
        if db:
            print(f"Updating Firestore status to APPROVED for: {store_id}...")
            db.collection("stores").document(store_id).update({"approvalStatus": "APPROVED"})
            print("Status updated to APPROVED. Proceeding with uploads...")

        release_id = rel_resp.json()["id"]
        upload_url_template = rel_resp.json()["upload_url"].split("{")[0]

        flavors = {
            "customer": "userAppDownloadUrl",
            "delivery": "deliveryAppDownloadUrl",
            "store": "storeAppDownloadUrl"
        }
        
        updates = {}
        
        for flavor, field in flavors.items():
            apk_path = os.path.join(PROJECT_ROOT, f"app/build/outputs/apk/{flavor}/debug/app-{flavor}-debug.apk")
            if os.path.exists(apk_path):
                file_name = f"{flavor}-{store_id}.apk"
                with open(apk_path, "rb") as f:
                    upload_resp = requests.post(
                        f"{upload_url_template}?name={file_name}",
                        data=f,
                        headers={**headers, "Content-Type": "application/vnd.android.package-archive"}
                    )
                
                if upload_resp.status_code == 201:
                    download_url = upload_resp.json()["browser_download_url"]
                    updates[field] = download_url
                    print(f"SUCCESS: Uploaded {flavor} to GitHub: {download_url}")
                else:
                    print(f"FAILED: Could not upload {flavor} to GitHub. Status: {upload_resp.status_code}")

        if db and updates:
            db.collection("stores").document(store_id).update(updates)
            print("Firestore links updated.")
            
    except Exception as e:
        print(f"CRITICAL ERROR during GitHub upload or Firestore update: {e}")

def main():
    import sys
    if len(sys.argv) < 3:
        print("Usage: python white_label_factory.py <store_id> <store_name> [icon_url] [user_app_name] [delivery_app_name] [store_app_name]")
        return

    store_id = sys.argv[1]
    store_name = sys.argv[2]
    icon_url = sys.argv[3] if len(sys.argv) > 3 else None
    
    # Optional specific app names
    user_app_name = sys.argv[4] if len(sys.argv) > 4 and sys.argv[4] else store_name
    delivery_app_name = sys.argv[5] if len(sys.argv) > 5 and sys.argv[5] else f"Delivery {store_name}"
    store_app_name = sys.argv[6] if len(sys.argv) > 6 and sys.argv[6] else f"Store Manager {store_name}"

    db, bucket = init_firebase()
    
    # Inject Store ID for white-labeling
    update_app_config(store_id)
    
    # Update branding (Icons are global for now, but we can specialize later if needed)
    # We update icons first
    if icon_url:
        print(f"Downloading icon from: {icon_url}")
        try:
            response = requests.get(icon_url)
            if response.status_code == 200:
                img = Image.open(BytesIO(response.content))
                densities = {"mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96, "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192}
                for folder, size in densities.items():
                    folder_path = os.path.join(RES_PATH, folder)
                    if not os.path.exists(folder_path): os.makedirs(folder_path)
                    resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
                    resized_img.save(os.path.join(folder_path, "ic_launcher.png"))
                    resized_img.save(os.path.join(folder_path, "ic_launcher_round.png"))
                print("Icons replaced.")
        except Exception as e:
            print(f"Failed to update icons: {e}")
    
    if run_gradle_build(user_app_name, delivery_app_name, store_app_name):
        upload_and_update(store_id, db, bucket)
    else:
        if db:
            db.collection("stores").document(store_id).update({"approvalStatus": "REJECTED"})
        print("Build failed. Status set to REJECTED.")

if __name__ == "__main__":
    main()
