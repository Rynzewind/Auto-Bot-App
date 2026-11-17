plugins { id("com.android.application") }
android {
    namespace = "com.kev.fortniteautobot"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig { applicationId = "com.kev.fortniteautobot"; minSdk = 29; targetSdk = 35; versionCode = 140; versionName = "1.4.0" }
    val personalKeyAvailable = rootProject.file("signing/auto-bot.jks").exists()
    signingConfigs { if (personalKeyAvailable) create("personal") { storeFile = rootProject.file("signing/auto-bot.jks"); storePassword = "android"; keyAlias = "autobot"; keyPassword = "android" } }
    buildTypes { getByName("debug") { if (personalKeyAvailable) signingConfig = signingConfigs.getByName("personal") }; getByName("release") { isMinifyEnabled = false; if (personalKeyAvailable) signingConfig = signingConfigs.getByName("personal") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
}
