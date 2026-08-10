plugins {
    id("aligner.repository-jdbc")
}

dependencies {
    api(project(":training:model"))
    implementation(project(":training:infrastructure"))

    testImplementation(project(":training:schema"))
}
