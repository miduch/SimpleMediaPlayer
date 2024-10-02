buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.52")
    }
}

plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}