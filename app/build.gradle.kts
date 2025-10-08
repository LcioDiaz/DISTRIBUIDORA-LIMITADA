plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.distribuidora.distribuidoralimitada"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.distribuidora.distribuidoralimitada"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX y Material Design (Usando el catálogo de versiones 'libs')
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation("androidx.cardview:cardview:1.0.0")

    // Lifecycles (Componentes de Arquitectura de Android)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Coroutines para operaciones asíncronas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- Firebase y Google ---
    // 1. Se importa el BoM (Bill of Materials) de Firebase.
    // Esto gestionará las versiones de todas las librerías de Firebase por nosotros.
    implementation(enforcedPlatform("com.google.firebase:firebase-bom:34.2.0"))

    // 2. Se declaran las librerías de Firebase que necesitas SIN especificar la versión.
    // Se usa el sufijo '-ktx' para las extensiones de Kotlin.
    // PRUEBA TEMPORAL: quita luego
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-analytics-ktx:22.0.2")


    // 3. Dependencias de Google Play Services (estas sí necesitan su propia versión)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")


    // Dependencias para Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}