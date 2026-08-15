import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * 컨벤션 플러그인에서 루트 버전 카탈로그(gradle/libs.versions.toml)를 읽는다.
 *
 * 생성된 `libs` 접근자는 precompiled script plugin 안에서는 보이지 않으므로
 * 공개 API 인 VersionCatalogsExtension 으로 접근한다.
 */
internal val Project.alignerLibs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** 오타를 빌드 실패로 만든다. 조용히 빈 의존성이 되는 것보다 낫다. */
internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("gradle/libs.versions.toml 에 라이브러리 '$alias' 가 없습니다")
    }
