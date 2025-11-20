pluginManagement {
    repositories {
        // 优先使用阿里云镜像获取Gradle插件
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }

        // 保留官方源作为备用，并精确控制特定组从Google官方拉取[citation:4]
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 优先使用阿里云镜像获取项目依赖库
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }

        // 保留官方源作为备用[citation:4]
        google()
        mavenCentral()
    }
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
