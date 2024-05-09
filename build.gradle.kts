buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
    }
}

plugins {
    id("com.android.application") version "8.4.0" apply false
    id("com.android.library") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}