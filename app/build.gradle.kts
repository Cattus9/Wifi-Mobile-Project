plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.project.inet_mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.project.inet_mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    // Use the version catalog (libs) for all your main dependencies

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)

    // Optional KTX libraries (it's good practice to add these to your libs.versions.toml as well, but this will work)
    implementation("androidx.core:core-ktx:1.13.1")


    // Test implementations
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



}
