import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val neonUrl = localProperties.getProperty("neon.url") ?: "postgresql://neondb_owner:npg_kSW0XNpZs9lj@ep-little-dew-ac6nnjk5-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
val googleWebClientId = localProperties.getProperty("google.web.client.id") ?: "941133838706-nt9dupnr2ovrs4p4vr9gibk49fa4315b.apps.googleusercontent.com"

android {
    namespace = "com.example.breathingapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.breathingapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "NEON_URL", "\"$neonUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  
  // Navigation Compose
  implementation(libs.androidx.navigation.compose)

  // DataStore for local storage
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)

  // Image Loading
  implementation(libs.coil.compose)

  // Neon PostgreSQL HTTP & Ktor
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.play.services.auth)
}
