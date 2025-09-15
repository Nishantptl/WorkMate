plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.workmate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.workmate"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

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
            buildConfigField( "String", "SENDGRID_API_KEY", "\"${project.property("SENDGRID_API_KEY")}\"")
        }
        debug {
            buildConfigField ("String", "SENDGRID_API_KEY", "\"${project.property("SENDGRID_API_KEY")}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.base)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Firebase BoM
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation (platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("com.google.firebase:firebase-database:22.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("com.google.android.gms:play-services-base:18.7.2")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

}