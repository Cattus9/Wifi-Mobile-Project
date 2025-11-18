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

    buildFeatures {
        viewBinding = true
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
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.fragment)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)

    implementation("androidx.cardview:cardview:1.0.0")

    // Optional KTX libraries (it's good practice to add these to your libs.versions.toml as well, but this will work)
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.swiperefreshlayout)
//    implementation("com.midtrans:corekit:2.0.0")
//    implementation("com.midtrans:uikit:2.0.0")
    implementation("com.midtrans:java-library:3.2.1")



    // Test implementations
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.airbnb.android:lottie:6.1.0")


}

