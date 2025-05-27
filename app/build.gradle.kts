plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id ("com.google.gms.google-services")
}

android {
    namespace = "com.business.techassist"
    compileSdk = 35

    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
        }
    }


    defaultConfig {
        applicationId = "com.business.techassist"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    
    // Firebase dependencies (without version numbers, managed by BoM)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    
    // Remove the direct Firebase implementation
    // implementation("com.google.firebase:firebase-firestore:24.10.2")
    // implementation(libs.firebase.storage)
    // implementation(libs.play.services.cast.framework)
    // implementation(libs.firebase.messaging)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.generativeai)
    implementation(libs.androidx.navigation.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.dhaval2404:imagepicker:2.1")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.18.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation ("com.google.ai.client.generativeai:generativeai:0.9.0")
    debugImplementation(libs.ui.tooling)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

        implementation ("androidx.compose.material:material-icons-extended:1.6.0")
        implementation ("androidx.compose.material3:material3:1.2.0")


    implementation ("androidx.compose.ui:ui:1.6.0")
    implementation ("androidx.compose.material:material:1.7.8")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation (libs.androidx.lifecycle.runtime.ktx.v262)
    implementation (libs.androidx.activity.compose.v182)
    implementation ("androidx.compose.runtime:runtime-livedata:1.7.8")

    debugImplementation(libs.ui.test.manifest)
}