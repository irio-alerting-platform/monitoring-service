name := "monitoring-service"
version := "0.1"
scalaVersion := "2.13.4"

scalacOptions := Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding",
  "utf8",
  "-Ywarn-macros:after",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:privates",
  "-Ymacro-annotations",
  "-language:higherKinds"
)

libraryDependencies ++= {
  val akkaV           = "2.6.11"
  val akkaHttpV       = "10.2.2"
  val logbackV        = "1.2.3"
  val courierV        = "2.0.0"
  val jedisV          = "3.5.0"
  val loggingLogbackV = "0.119.4-alpha"
  val specsV          = "4.10.6"
  val mockitoV        = "1.16.15"

  Seq(
    "com.typesafe.akka"     %% "akka-actor-typed"            % akkaV,
    "com.typesafe.akka"     %% "akka-stream"                 % akkaV,
    "com.typesafe.akka"     %% "akka-http"                   % akkaHttpV,
    "com.typesafe.akka"     %% "akka-http-spray-json"        % akkaHttpV,
    "com.typesafe.akka"     %% "akka-slf4j"                  % akkaV,
    "ch.qos.logback"        % "logback-classic"              % logbackV,
    "com.github.daddykotex" %% "courier"                     % courierV,
    "redis.clients"         % "jedis"                        % jedisV,
    "com.google.cloud"      % "google-cloud-logging-logback" % loggingLogbackV,
    "org.specs2"            %% "specs2-core"                 % specsV % Test,
    "org.mockito"           %% "mockito-scala"               % mockitoV % Test,
    "com.typesafe.akka"     %% "akka-stream-testkit"         % akkaV % Test,
    "com.typesafe.akka"     %% "akka-http-testkit"           % akkaHttpV % Test
  )
}

scalafmtConfig := file(".scalafmt.conf")
scalafmtOnCompile := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

packageName := "monitoring-service"
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
daemonUser in Docker := "root"
dockerUpdateLatest := true
