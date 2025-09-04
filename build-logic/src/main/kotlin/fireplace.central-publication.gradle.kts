import com.javiersc.semver.project.gradle.plugin.extensions.isNotSnapshot

plugins {
    publishing
    id("com.gradleup.nmcp.aggregation")
}

nmcpAggregation {
    centralPortal {
        // intentional fallback to enable taskTree ./gradlew taskTree publishAggregationToCentralPortal
        val failsafe = providers.provider { throw GradleException("value not set") }
        username = providers.environmentVariable("MAVENCENTRAL_USERNAME").orElse(failsafe)
        password = providers.environmentVariable("MAVENCENTRAL_PASSWORD").orElse(failsafe)
        // publish manually from the portal
        publishingType = "USER_MANAGED"

        // the following have an effect only with publishingType = "AUTOMATIC"
        // validationTimeout = Duration.of(30, ChronoUnit.MINUTES)
        // publishingTimeout = Duration.of(30, ChronoUnit.MINUTES)
    }
}

tasks {
    named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
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
