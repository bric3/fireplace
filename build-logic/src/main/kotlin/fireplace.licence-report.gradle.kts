/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import com.github.jk1.license.filter.ExcludeDependenciesWithoutArtifactsFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.SpdxLicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import com.github.jk1.license.render.TextReportRenderer

plugins {
    id("com.github.jk1.dependency-license-report")
}

licenseReport {
    projects = arrayOf(project)
    outputDir = layout.buildDirectory.map { "$it/reports/licenses" }.get()
    renderers = arrayOf(InventoryMarkdownReportRenderer(), TextReportRenderer())
    // Dependencies use inconsistent titles for Apache-2.0, so we need to specify mappings
    filters = arrayOf(
        LicenseBundleNormalizer(
            mapOf(
                "The Apache License, Version 2.0" to "Apache-2.0",
                "The Apache Software License, Version 2.0" to "Apache-2.0",
            )
        ),
        ExcludeDependenciesWithoutArtifactsFilter(),
        SpdxLicenseBundleNormalizer()
    )
}