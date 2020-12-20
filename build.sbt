scalaVersion := "2.13.4"
organization := "com.lihaoyi"
name := "mainargs"

libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.7.1" % Test
libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.3" % Test
testFrameworks := Seq(new TestFramework("utest.runner.Framework"))

Compile / unmanagedSourceDirectories += baseDirectory.value / "mainargs" / "src"
Test / unmanagedSourceDirectories += baseDirectory.value / "mainargs" / "test" / "src"
Compile / scalacOptions ++= Seq("-feature", "-deprecation")
