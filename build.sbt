name := "spring-2020-scala"

version := "0.1"

scalaVersion := "2.12.9"

// Core with minimal dependencies, enough to spawn your first bot.
libraryDependencies += "com.bot4s" %% "telegram-core" % "4.4.0-RC2"

// Extra goodies: Webhooks, support for games, bindings for actors.
libraryDependencies += "com.bot4s" %% "telegram-akka" % "4.4.0-RC2"

libraryDependencies += "com.softwaremill.sttp" %% "json4s" % "1.7.2"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.0"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.0"

libraryDependencies += "org.scalamock" %% "scalamock" % "4.4.0" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % Test