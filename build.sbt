lazy val akkaHttpVersion = "10.1.7"
lazy val akkaVersion    = "2.5.21"

mainClass in Compile := Some("Server")

lazy val root = (project in file("."))
  .aggregate(backend)

lazy val backend = project
  .settings(
    inThisBuild(List(
      scalaVersion    := "2.12.6"
    )),
    name := "dotadictionary",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "org.reactivestreams" % "reactive-streams" % "1.0.0",
      "ch.megard" %% "akka-http-cors" % "0.4.0",
      "de.heikoseeberger" %% "akka-http-circe" % "1.20.1",

      "io.circe" %% "circe-generic" % "0.9.3",

      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
      

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    )
  )

enablePlugins(JavaAppPackaging)