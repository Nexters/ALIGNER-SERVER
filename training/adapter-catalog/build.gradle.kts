plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    implementation(project(":training:infrastructure"))
    implementation(project(":catalog:contract"))
}
