name := "monitoring-service"
version := "0.1"
scalaVersion := "2.13.4"

libraryDependencies ++= {
  val akkaV     = "2.6.11"
  val akkaHttpV = "10.2.2"
  val logbackV  = "1.2.3"
  val courierV  = "2.0.0"
  val redisV    = "3.30"
  val specsV    = "4.10.6"
  val mockitoV  = "1.16.15"

  Seq(
    "com.typesafe.akka"     %% "akka-actor-typed"     % akkaV,
    "com.typesafe.akka"     %% "akka-stream"          % akkaV,
    "com.typesafe.akka"     %% "akka-testkit"         % akkaV    % Test,
    "com.typesafe.akka"     %% "akka-http"            % akkaHttpV,
    "com.typesafe.akka"     %% "akka-http-spray-json" % akkaHttpV,
    "ch.qos.logback"         % "logback-classic"      % logbackV,
    "com.github.daddykotex" %% "courier"              % courierV,
    "net.debasishg"         %% "redisclient"          % redisV,
    "org.specs2"            %% "specs2-core"          % specsV   % Test,
    "org.mockito"           %% "mockito-scala"        % mockitoV % Test
  )
}

scalafmtConfig := file(".scalafmt.conf")
scalafmtOnCompile := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

packageName := "irio-monitoring-service"
dockerBaseImage := "hseeberger/scala-sbt:11.0.9.1_1.4.6_2.13.4"
daemonUser in Docker := "root"
dockerUpdateLatest := true
