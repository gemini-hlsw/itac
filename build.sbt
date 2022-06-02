
publish / skip := true

inThisBuild(Seq(
  scalaVersion := "2.13.3",
  resolvers    += "Gemini Repository" at "https://github.com/gemini-hlsw/maven-repo/raw/master/releases",
  homepage := Some(url("https://github.com/gemini-hlsw/itac")),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
) ++ gspPublishSettings)

val commonSettings = Seq(
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
)

// probably unnecessary
commonSettings

lazy val engine = project
  .in(file("modules/engine"))
  .settings(commonSettings)
  .settings(
    name := "itac-engine",
    libraryDependencies ++= Seq(
      "edu.gemini.ocs"          %% "edu-gemini-model-p1"         % "2022102.1.1",
      "edu.gemini.ocs"          %% "edu-gemini-shared-skyobject" % "2022001.1.2",
      "edu.gemini.ocs"          %% "edu-gemini-util-skycalc"     % "2022001.1.2",
      "org.scala-lang.modules"  %% "scala-xml"                   % "2.0.0-M2",
      "org.slf4j"                % "slf4j-api"                   % "1.7.30",
      "javax.xml.bind"           % "jaxb-api"                    % "2.3.1",
      "com.sun.xml.bind"         % "jaxb-impl"                   % "2.3.3",
      "com.sun.xml.bind"         % "jaxb-core"                   % "2.3.0.1",
      "com.github.sbt"             % "junit-interface"             % "0.13.3"    % "test",
      "junit"                    % "junit"                       % "4.13"    % "test",
      "org.mockito"              % "mockito-all"                 % "1.10.19" % "test",
      "org.scalacheck"          %% "scalacheck"                  % "1.14.1"  % "test",
      "org.scalatest"           %% "scalatest"                   % "3.1.1"   % "test",
      "org.scalatestplus"       %% "scalacheck-1-14"             % "3.1.1.1" % "test",
      "org.slf4j"                % "slf4j-simple"                % "1.7.30"  % "test",
     ),
    Test / compile / scalacOptions := Nil, // don't worry about warnings right now
  )

lazy val main = project
  .in(file("modules/main"))
  .settings(commonSettings)
  .dependsOn(engine)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "itac-main",
    libraryDependencies ++= Seq(
      "com.monovore"                 %% "decline-effect"         % "1.3.0",
      "com.monovore"                 %% "decline"                % "1.3.0",
      "edu.gemini"                   %% "gsp-math"               % "0.1.16", // <--- don't change to lucuma!
      "io.chrisdavenport"            %% "log4cats-slf4j"         % "1.1.1",
      "io.circe"                     %% "circe-core"             % "0.13.0",
      "io.circe"                     %% "circe-generic"          % "0.13.0",
      "io.circe"                     %% "circe-parser"           % "0.13.0",
      "io.circe"                     %% "circe-yaml"             % "0.13.1",
      "com.sun.mail"                  % "javax.mail"             % "1.6.2",
      "org.apache.velocity"           % "velocity-engine-core"   % "2.2",    // save me jeebus
      "org.slf4j"                     % "slf4j-simple"           % "1.7.30",
      "org.tpolecat"                 %% "atto-core"              % "0.8.0",
      "org.typelevel"                %% "cats-effect"            % "2.2.0",
      "com.github.davidmoten"         % "word-wrap"              % "0.1.6",
      "org.apache.poi"                % "poi"                    % "4.1.2",  // over here too jeebus
      "com.softwaremill.sttp.client" %% "core"                   % "2.2.9",
      "org.typelevel"                %% "cats-testkit"           % "2.2.0" % "test",
      "org.typelevel"                %% "cats-testkit-scalatest" % "2.0.0" % "test",
    ),
    sourceGenerators in Compile += Def.task {
      val outDir = (sourceManaged in Compile).value / "scala" / "itac"
      val outFile = new File(outDir, "BuildInfo.scala")
      outDir.mkdirs
      val v = version.value
      val t = System.currentTimeMillis
      IO.write(outFile,
        s"""|package itac
            |
            |/** Auto-generated build information. */
            |object BuildInfo {
            |  /** Current version of ITAC ($v). */
            |  val version = "$v"
            |  /** Build date (${new java.util.Date(t)}). */
            |  val date    = new java.util.Date(${t}L)
            |}
            |""".stripMargin)
      Seq(outFile)
    }.taskValue
  )

lazy val channel = project
  .in(file("modules/channel"))
  .settings(commonSettings)
  .settings(
    name := "itac-channel",

    // Create the app manifest such that it includes the version string.
    resourceGenerators in Compile += Def.task {
      val outDir = resourceManaged.value
      val outFile = new File(outDir, "itac.json")
      outDir.mkdirs
      val v = version.value
      IO.write(outFile,
        s"""|{
            |  "repositories": [
            |    "central",
            |    "sonatype:public",
            |    "https://github.com/gemini-hlsw/maven-repo/raw/master/releases",
            |    "ivy2Local"
            |  ],
            |  "dependencies": [
            |    "edu.gemini:itac-main_2.13:${version.value}"
            |  ],
            |  "javaOptions": [
            |    "-Djava.awt.headless=true"
            |  ]
            |}
            |""".stripMargin)
      Seq(outFile)
    }.taskValue,

    // Don't add _2.12 to the artifact name, and also don't add a dependency to the Scala lib.
    crossPaths := false,
    autoScalaLibrary := false

  )

