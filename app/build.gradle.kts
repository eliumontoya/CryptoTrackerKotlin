plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
 }

configurations.matching { it.name.contains("androidTest", ignoreCase = true) }.configureEach {
    exclude(group = "org.junit.jupiter")
    exclude(group = "org.junit.platform")
}

android {
    namespace = "info.eliumontoyasadec.cryptotracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "info.eliumontoyasadec.cryptotracker"
        minSdk = 33
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
        debug {
            // Coverage para tests instrumentados (src/androidTest)
            enableAndroidTestCoverage = true

            // Coverage para unit tests locales (src/test)
            enableUnitTestCoverage = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    // Kotlin 1.9.22 friendly Compose compiler extension.
    // If your project uses a different Kotlin version, we can align this.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    testOptions {

        unitTests {
            isReturnDefaultValues = true

        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Compose (use BOM so versions stay aligned)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")


    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Material (non-compose; safe to keep if used by theme/resources)
    implementation("com.google.android.material:material:1.13.0")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.13.10")


    // Instrumentation tests
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
     androidTestImplementation("androidx.test:runner:1.5.2")

    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    //androidTestImplementation("androidx.tracing:tracing:1.2.0")
   // androidTestImplementation("io.mockk:mockk-android:1.13.12")
}