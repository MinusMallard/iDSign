plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.idsign"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.idsign"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("commons-io:commons-io:2.13.0")

    // Adding the paths of jPBC jar files
    implementation(files("libs/jpbc-api-2.0.0.jar","libs/jpbc-plaf-2.0.0.jar"))

    // Adding the Bouncy Castle Dependency for HKDF function
    implementation(files("libs/bcpkix-jdk15on-1.70.jar","libs/bcprov-jdk15on-1.70.jar"))

    // Adding iText Library to manipulate PDF documents, excluding Bouncy Castle (v1.67)
    implementation("com.itextpdf:itext7-core:7.1.14") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
    }


}