plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.appit"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.appit"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // SỬA LỖI: Bật tính năng BuildConfig
    buildFeatures {
        buildConfig = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // SỬA LỖI: Thêm Firebase Bill of Materials (BoM) để quản lý phiên bản
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    // Khai báo các thư viện Firebase mà không cần phiên bản
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    
    // App Check dependencies
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    
    // Google Sign In SDK (Cần thiết cho LoginActivity và RegisterActivity)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // QR Code Generator
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Animation Library
    implementation("com.airbnb.android:lottie:5.2.0")

    // Circle Image View for Profile
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // MPAndroidChart for charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Circle Indicator for Banner (ViewPager2)
    implementation("me.relex:circleindicator:2.1.6")

}
