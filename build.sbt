import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val frp = (project in file("."))
  .settings(name := "frp")
  .aggregate(
    frpCore.jvm,
    frpCore.js,
  )

lazy val frpCore = (crossProject(JVMPlatform, JSPlatform) in file("frp-core"))
  .settings(name := "frp-core")
  .settings(crossDependencies(
    peknight.app,
  ))
