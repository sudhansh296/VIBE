pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // SVGA gift animation player
        maven { url = uri("https://jitpack.io"); content { includeGroup("com.github.yyued") } }
    }
}

rootProject.name = "VIBE"
include(":app")
