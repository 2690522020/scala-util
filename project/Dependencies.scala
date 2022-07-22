import sbt._

object Dependencies {

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.12" % Test
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.9.2"
  lazy val playSlick = "com.typesafe.play" %% "play-slick" % "5.0.2"
  lazy val jedis = "redis.clients" % "jedis" % "4.2.3"
  lazy val mysql = "mysql" % "mysql-connector-java" % "8.0.29"
  lazy val spring = "org.springframework.cloud" % "spring-cloud-starter-security" % "2.2.5.RELEASE"
  lazy val cats = "org.typelevel" %% "cats-core" % "2.8.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.12"
  lazy val jdk15on: Seq[ModuleID] = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.70",
    "org.bouncycastle" % "bcprov-ext-jdk15on" % "1.70"
  )
  lazy val datahub = "com.aliyun.datahub" % "aliyun-sdk-datahub" % "2.12.1-public" exclude("org.slf4j", "slf4j-log4j12")
  lazy val minio = "io.minio" % "minio" % "8.4.2"
  lazy val silhouette: Seq[ModuleID] = Seq(
    "io.github.honeycomb-cheesecake" %% "play-silhouette" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-cas" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-password-argon2" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-totp" % silhouetteVersion,
    "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % silhouetteVersion % Test
  ).map {
    r =>
      r excludeAll ExclusionRule("com.fasterxml.jackson.core")
  }
  val resolvers: Seq[MavenRepository] = Seq[MavenRepository](elems =
    "Atlassian Releases" at "https://packages.atlassian.com/public/"
  )
  val silhouetteVersion: String = "8.0.2"
}
