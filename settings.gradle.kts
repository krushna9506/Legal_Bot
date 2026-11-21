try {
    val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
    val envField = processEnvClass.getDeclaredField("theCaseInsensitiveEnvironment")
    envField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val envMap = envField.get(null) as? MutableMap<String, String>
    envMap?.remove("ANDROID_PREFS_ROOT")
    
    val envMap2Field = processEnvClass.getDeclaredField("theEnvironment")
    envMap2Field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val envMap2 = envMap2Field.get(null) as? MutableMap<Any, Any>
    envMap2?.keys?.removeIf { it.toString() == "ANDROID_PREFS_ROOT" }
} catch (_: Throwable) {
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LegalChatbot"
include(":app")
