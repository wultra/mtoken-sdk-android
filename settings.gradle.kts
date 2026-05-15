dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
    }
}
include(":library")
