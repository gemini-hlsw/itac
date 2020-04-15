
publish / skip := true

inThisBuild(Seq(
  scalaVersion := "2.12.10",
  resolvers    += "Gemini Repository" at "https://github.com/gemini-hlsw/maven-repo/raw/master/releases",
  homepage := Some(url("https://github.com/gemini-hlsw/itac")),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
) ++ gspPublishSettings)

lazy val engine = project
  .in(file("modules/engine"))
  .settings(
    name := "itac-engine",
    libraryDependencies ++= Seq(
      "edu.gemini.ocs"          %% "edu-gemini-model-p1"         % "2020001.1.0",
      "edu.gemini.ocs"          %% "edu-gemini-shared-skyobject" % "2019101.1.4",
      "edu.gemini.ocs"          %% "edu-gemini-util-skycalc"     % "2019101.1.4",
      "edu.gemini.ocs"          %% "edu-gemini-util-security"    % "2019101.1.4",
      "org.scala-lang.modules"  %% "scala-xml"                   % "2.0.0-M1",
      "org.slf4j"                % "slf4j-api"                   % "1.7.28",
      "com.novocode"             % "junit-interface"             % "0.11"    % "test",
      "junit"                    % "junit"                       % "4.12"    % "test",
      "org.mockito"              % "mockito-all"                 % "1.10.19" % "test",
      "org.scalacheck"          %% "scalacheck"                  % "1.14.1"  % "test",
      "org.scalatest"           %% "scalatest"                   % "3.1.1"   % "test",
      "org.scalatestplus"       %% "scalacheck-1-14"             % "3.1.1.1" % "test"
     ),
    scalacOptions := Nil, // don't worry about warnings right now
  )

lazy val main = project
  .in(file("modules/main"))
  .dependsOn(engine)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "itac-main",
    libraryDependencies ++= Seq(
      "com.monovore"       %% "decline-effect"         % "1.0.0",
      "com.monovore"       %% "decline"                % "1.0.0",
      "edu.gemini"         %% "gsp-math"               % "0.1.10",
      "io.chrisdavenport"  %% "log4cats-slf4j"         % "1.0.1",
      "io.circe"           %% "circe-core"             % "0.11.1",
      "io.circe"           %% "circe-generic"          % "0.11.1",
      "io.circe"           %% "circe-parser"           % "0.11.1",
      "io.circe"           %% "circe-yaml"             % "0.10.0",
      "javax.mail"          % "javax.mail-api"         % "1.6.2",
      "org.apache.velocity" % "velocity-engine-core"   % "2.2",    // save me jeebus
      "org.slf4j"           % "slf4j-simple"           % "1.7.28",
      "org.tpolecat"       %% "atto-core"              % "0.7.1",
      "org.typelevel"      %% "cats-effect"            % "2.0.0",
      "org.typelevel"      %% "cats-testkit"           % "2.0.0"     % "test",
      "org.typelevel"      %% "cats-testkit-scalatest" % "1.0.0-RC1" % "test",
    )
  )

lazy val channel = project
  .in(file("modules/channel"))
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
            |    "edu.gemini:itac-main_2.12:${version.value}"
            |  ]
            |}
            |""".stripMargin)
      Seq(outFile)
    }.taskValue,

    // Don't add _2.12 to the artifact name, and also don't add a dependency to the Scala lib.
    crossPaths := false,
    autoScalaLibrary := false

  )

