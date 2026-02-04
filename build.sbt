import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val frp = (project in file("."))
  .settings(name := "frp")
  .aggregate(
    frpCore.jvm,
    frpCore.js,
    frpCustom.jvm,
    frpCustom.js,
  )

lazy val frpCore = (crossProject(JVMPlatform, JSPlatform) in file("frp-core"))
  .settings(name := "frp-core")
  .settings(crossDependencies(
    peknight.auth,
    peknight.app,
    comcast.ip4s,
  ))

lazy val frpCustom = (crossProject(JVMPlatform, JSPlatform) in file("frp-custom"))
  .dependsOn(frpCore)
  .settings(name := "frp-custom")
  .settings(crossDependencies(
    peknight.app.build,
    peknight.docker.build,
    peknight.docker.custom,
    peknight.http.client,
    peknight.fs2.tar,
  ))
