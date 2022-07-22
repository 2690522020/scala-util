resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

/**
 * play 服务框架
 */
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.16")
/**
 * sbt 打包
 */
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")

/**
 * 使用Scalafmt进行代码格式化
 */
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

/**
 * Scala库的二进制兼容性管理
 */
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.0")
/**
 * sbt-version-policy 帮助库维护者遵循推荐的版本控制方案
 * 配置MiMa以检查二进制或源代码不兼容性，
 * 确保您的任何依赖项都不会以不兼容的方式被碰撞或删除。
 */
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "2.0.1")
/**
 * 自动生成源代码文件头(例如版权声明)
 */
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.7.0")

/**
 * 创建胖的JAR
 */
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")


