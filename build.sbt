ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val code = (project in file("code"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.16",
      "dev.zio" %% "zio-streams" % "2.0.16"
    )
  )


lazy val scalatest = (project in file("scalatest"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.16"
    )
  )
  .dependsOn(code)

lazy val zioTest = (project in file("zio-test"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % "2.0.16" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.16" % Test,
      "dev.zio" %% "zio-test-magnolia" % "2.0.16" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(code)
