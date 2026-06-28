import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val frp = (project in file("."))
  .settings(name := "frp")
  .aggregate(frpCore.projectRefs *)
  .aggregate(frpCustom.projectRefs *)

lazy val frpCore = (projectMatrix in file("frp-core"))
  .settings(name := "frp-core")
  .settings(libraryDependencies ++= dependencies(
    peknight.auth,
    peknight.app,
    comcast.ip4s,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val frpCustom = (projectMatrix in file("frp-custom"))
  .dependsOn(frpCore)
  .settings(name := "frp-custom")
  .settings(libraryDependencies ++= dependencies(
    peknight.app.build,
    peknight.docker.build,
    peknight.docker.custom,
    peknight.http.client,
    peknight.fs2.tar,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))
