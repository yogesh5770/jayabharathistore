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
    service_account_json = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
    if not service_account_json:
        print("FIREBASE_SERVICE_ACCOUNT not found in environment. Skipping Firebase updates.")
        return None, None
    
    cred_dict = json.loads(service_account_json)
    cred = credentials.Certificate(cred_dict)
    firebase_admin.initialize_app(cred, {
        'storageBucket': f"{cred_dict['project_id']}.appspot.com"
    })
    return firestore.client(), storage.bucket()

def update_app_branding(app_name, icon_url):
    print(f"--- Modifying source code for: {app_name} ---")
    
    # 1. Update strings.xml
    if os.path.exists(STRINGS_PATH):
        with open(STRINGS_PATH, "r", encoding="utf-8") as f:
            content = f.read()
        
        import re
        new_content = re.sub(r'<string name="app_name">.*?</string>', f'<string name="app_name">{app_name}</string>', content)
        
        with open(STRINGS_PATH, "w", encoding="utf-8") as f:
            f.write(new_content)
    
    # 2. Update Icons (Download and replace)
    if icon_url:
        print(f"Downloading icon from: {icon_url}")
        try:
            response = requests.get(icon_url)
            if response.status_code == 200:
                img = Image.open(BytesIO(response.content))
                
                # Update across common densities
                densities = {
                    "mipmap-mdpi": 48,
                    "mipmap-hdpi": 72,
                    "mipmap-xhdpi": 96,
                    "mipmap-xxhdpi": 144,
                    "mipmap-xxxhdpi": 192
                }
                
                for folder, size in densities.items():
                    folder_path = os.path.join(RES_PATH, folder)
                    if not os.path.exists(folder_path):
                        os.makedirs(folder_path)
                    
                    resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
                    resized_img.save(os.path.join(folder_path, "ic_launcher.png"))
                    resized_img.save(os.path.join(folder_path, "ic_launcher_round.png"))
                
                print("Icons replaced in all densities.")
        except Exception as e:
            print(f"Failed to update icons: {e}")

def run_gradle_build():
    print("--- Running Gradle Build (assembleDebug) ---")
    gradle_cmd = "gradlew.bat" if os.name == 'nt' else "./gradlew"
    try:
        subprocess.run([gradle_cmd, "assembleDebug"], check=True)
        return True
    except Exception as e:
        print(f"Build failed: {e}")
        return False

def upload_and_update(store_id, db, bucket):
    print(f"--- Uploading APKs for Store: {store_id} ---")
    
    flavors = {
        "customer": "userAppDownloadUrl",
        "delivery": "deliveryAppDownloadUrl",
        "store": "storeAppDownloadUrl"
    }
    
    updates = {"approvalStatus": "APPROVED"}
    
    for flavor, field in flavors.items():
        apk_path = os.path.join(PROJECT_ROOT, f"app/build/outputs/apk/{flavor}/debug/app-{flavor}-debug.apk")
        if os.path.exists(apk_path):
            blob = bucket.blob(f"apks/{store_id}/{flavor}.apk")
            blob.upload_from_filename(apk_path)
            blob.make_public()
            updates[field] = blob.public_url
            print(f"Uploaded {flavor} APK: {blob.public_url}")
    
    if db:
        db.collection("stores").document(store_id).update(updates)
        print("Firestore updated with APK links.")

def main():
    import sys
    if len(sys.argv) < 3:
        print("Usage: python white_label_factory.py <store_id> <store_name> [icon_url]")
        return

    store_id = sys.argv[1]
    store_name = sys.argv[2]
    icon_url = sys.argv[3] if len(sys.argv) > 3 else None

    db, bucket = init_firebase()
    
    update_app_branding(store_name, icon_url)
    
    if run_gradle_build():
        upload_and_update(store_id, db, bucket)
    else:
        if db:
            db.collection("stores").document(store_id).update({"approvalStatus": "REJECTED"})
        print("Build failed. Status set to REJECTED.")

if __name__ == "__main__":
    main()
