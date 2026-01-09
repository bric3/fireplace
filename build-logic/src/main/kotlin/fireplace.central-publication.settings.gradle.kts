import com.javiersc.semver.project.gradle.plugin.extensions.isNotSnapshot

// Maven Central Portal publication support via nmcp plugin

plugins {
    id("com.gradleup.nmcp.settings")
    id("com.javiersc.semver") apply false
}

// Note: nmcpSettings accessor is not available in the settings convention plugin
// use direct API access instead
// see 
configure<nmcp.internal.DefaultNmcpSettings>() {
    centralPortal {
        // Can't use roviders.environmentVariable("...") here due to bug with gradle configuration cache issue
        // See https://github.com/gradle/gradle/issues/36229
        // Workaround use a provider calling System.getenv(...) instead
        username = providers.gradleProperty("mavenCentralUsername")
            .orElse(providers.provider { System.getenv("MAVENCENTRAL_USERNAME") })
            .orElse("")

        password = providers.gradleProperty("mavenCentralPassword")
            .orElse(providers.provider { System.getenv("MAVENCENTRAL_PASSWORD") })
            .orElse("")

        // publish manually from the portal
        publishingType = "USER_MANAGED"

        // the following have an effect only with publishingType = "AUTOMATIC"
        // validationTimeout = Duration.of(30, ChronoUnit.MINUTES)
        // publishingTimeout = Duration.of(30, ChronoUnit.MINUTES)
    }
}

gradle.lifecycle.afterProject {
    if (project.rootProject == project) {
        project.tasks {
            register(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
                if (project.isNotSnapshot.getOrElse(false)) {
                    dependsOn(tasks.named("publishAggregationToCentralPortal"))
                } else {
                    dependsOn(tasks.named("publishAggregationToCentralPortalSnapshots"))
                }
            }

            named("nmcpPublishAggregationToCentralPortal") {
                onlyIf {
                    providers.gradleProperty("publish.central").orNull.toBoolean()
                }
            }

            named("publishAggregationToCentralPortalSnapshots") {
                onlyIf {
                    providers.gradleProperty("publish.central").orNull.toBoolean()
                }
            }
        }
    }
}
