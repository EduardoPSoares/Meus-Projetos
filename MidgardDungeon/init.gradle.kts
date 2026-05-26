allprojects {
    repositories {
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://maven.typewritermc.com/snapshots")
    }
    repositories.whenObjectAdded {
        if (this is MavenArtifactRepository && url.host?.contains("evokegames") == true) {
            logger.warn("Blocked unreachable evokegames repository")
            repositories.remove(this)
        }
    }
}
