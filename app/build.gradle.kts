plugins {
    alias(libs.plugins.android.application)
    // Aplica o plugin do Google Services neste módulo
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.a3_teste_paineldevotao"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.a3_teste_paineldevotao"
        minSdk = 24
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

    // Firebase BoM (controla versões de todos os módulos Firebase)
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Firebase Auth (para login anônimo)
    implementation("com.google.firebase:firebase-auth")

    // Firebase Firestore (banco de dados em nuvem)
    implementation("com.google.firebase:firebase-firestore")

    // Dependências padrão do template
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}