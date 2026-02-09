plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    kotlin("kapt")
}

android {
    namespace = "com.jayabharathistore.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jayabharathistore.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("customer") {
            dimension = "version"
            applicationIdSuffix = ".customer"
            versionNameSuffix = "-customer"
            resValue("string", "app_name", "@string/app_name")
        }
        create("store") {
            dimension = "version"
            applicationIdSuffix = ".store"
            versionNameSuffix = "-store"
            resValue("string", "app_name", "@string/app_name")
        }
        create("delivery") {
            dimension = "version"
            applicationIdSuffix = ".delivery"
            versionNameSuffix = "-delivery"
            resValue("string", "app_name", "@string/app_name")
        }
        create("creator") {
            dimension = "version"
            applicationIdSuffix = ".creator"
            versionNameSuffix = "-creator"
            resValue("string", "app_name", "App Creator Studio")
        }
        create("admin") {
            dimension = "version"
            applicationIdSuffix = ".admin"
            versionNameSuffix = "-admin"
            resValue("string", "app_name", "Store Verification Admin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.storage.ktx)

    // Cashfree
    implementation(libs.cashfree.pg)

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coil for image loading
    implementation(libs.coil.compose)

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}