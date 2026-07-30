plugins {
    `kotlin-dsl`
}

// 컨벤션 플러그인이 apply 하는 Gradle 플러그인의 구현체를 클래스패스에 올린다.
// 여기 없는 플러그인은 aligner.* 안에서 id(...) 로 적용할 수 없다.
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
}
