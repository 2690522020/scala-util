import Dependencies._

ThisBuild / version := "2.0"

lazy val `tas` = (project in file("."))
  .aggregate(
    `app-util`,`common-util`, `redis-util`,`minio-util`,`datahub-util`
  )

/**
 * 自定义App扩展
 */
lazy val `app-util` = (project in file("app-util"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.11"),
    commonSettings
  )
/**
 * 公共扩展方法依赖库
 */
lazy val `common-util` = (project in file("common-util")).settings(
  commonSettings)
/**
 * redis 扩展
 */
lazy val `redis-util` = (project in file("redis-util")).settings(
  libraryDependencies ++= Seq(jedis),
  commonSettings
).dependsOn( `common-util`)
/**
 * minio 扩展
 */
lazy val `minio-util` = (project in file("minio-util")).settings(
  libraryDependencies ++= Seq(minio),
  commonSettings
).dependsOn( `common-util`)
/**
 * dataHub 扩展
 */
lazy val `datahub-util` = (project in file("datahub-util")).settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    datahub),
  commonSettings
)

lazy val commonSettings = Def.settings(
  organization := "zjw",
  organizationName := "io.github.zjw",
  maintainer := "2690522020@tallsafe.com",
  licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
  scalaVersion := "2.13.8",
  crossScalaVersions := Seq("2.13.8", "3.1.3"),
  headerLicense := {
    Some(
      HeaderLicense.Custom(
        "Copyright (C) Lightbend Inc. <https://www.lightbend.com>"
      )
    )
  },
  assembly / assemblyMergeStrategy := {
    case PathList("javax", "servlet", _*) => MergeStrategy.first
    case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
    case PathList(ps@_*) if ps.last startsWith "module-info." => MergeStrategy.first
    case "application.conf" => MergeStrategy.concat
    case "unwanted.txt" => MergeStrategy.discard
    case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val akkaAppSettings = Def.settings(
  Universal / mappings ++= {
    import sbt.Path._
    val resourceMappings = contentOf(sourceDirectory.value / "main" / "resources")
    resourceMappings.map {
      case (resource, path) => resource -> ("conf/" + path)
    }
  },
  Compile / packageBin / mappings ~= { (ms: Seq[(File, String)]) =>
    val resourceNames = Seq("application.conf", "logback.xml")
    ms filter {
      case (_, toPath) => !resourceNames.contains(toPath)
    }
  },
  scriptClasspath := {
    val scriptClasspathValue = scriptClasspath.value
    "../conf/" +: scriptClasspathValue
  }
)
val akkaVersion = "2.6.19"