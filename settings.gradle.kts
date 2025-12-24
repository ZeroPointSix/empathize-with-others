pluginManagement {
    repositories {
        // 腾讯云 Maven 镜像 (优先使用)
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/google/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/gradle/") }

        // 阿里云镜像 (备用)
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // JitPack (第三方库)
        maven { url = uri("https://jitpack.io") }

        // 官方仓库 (最后备用)
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
        // 腾讯云 Maven 镜像 (优先使用)
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/google/") }

        // 阿里云镜像 (备用)
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }

        // JitPack (第三方库)
        maven { url = uri("https://jitpack.io") }

        // 官方仓库 (最后备用)
        google()
        mavenCentral()
    }
}

rootProject.name = "Give Love"

// 模块配置
include(":app")
include(":domain")
include(":data")
include(":presentation")
