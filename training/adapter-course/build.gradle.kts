plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    implementation(project(":training:infrastructure"))
    implementation(project(":course:contract"))
}
