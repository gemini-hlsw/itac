resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("edu.gemini"       % "sbt-lucuma"                        % "0.6-16bd6e7-SNAPSHOT")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"                       % "0.5.1")
addSbtPlugin("org.typelevel"    % "sbt-typelevel-sonatype-ci-release" % "0.4.3")
addSbtPlugin("org.typelevel"    % "sbt-typelevel-ci-signing"          % "0.4.3")
addSbtPlugin("com.dwijnand"     % "sbt-dynver"                        % "4.1.1")
