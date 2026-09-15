// app/build.gradle.kts (Dentro da pasta app)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Gerencia o Jetpack Compose no Kotlin 2.0
}

android {
    namespace = "br.com.ysondantas.rotina"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.ysondantas.rotina"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Alinhado com as exigências modernas
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Bibliotecas bases atualizadas para compatibilidade
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    
    // Conjunto de bibliotecas do Jetpack Compose (BOM)
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
}
