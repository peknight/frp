package com.peknight.frp

import cats.data.IorT
import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.{ApplicativeError, Monad}
import com.peknight.app.AppName
import com.peknight.app.build.fatedier.frp.{url, version}
import com.peknight.cats.syntax.iorT.rLiftIT
import com.peknight.docker.Identifier.ImageRepositoryTag
import com.peknight.docker.Tag
import com.peknight.docker.build.library.alpine
import com.peknight.docker.command.run.{PortMapping, RestartPolicy, RunOptions, VolumeMount}
import com.peknight.docker.custom.service.{buildImageIfNotExists, runNetworkApp}
import com.peknight.docker.custom.{maintainer, image as customImage}
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asIT
import com.peknight.frp.appName as frpAppName
import com.peknight.fs2.io.file.path.*
import com.peknight.fs2.io.syntax.path.{createParentDirectories, writeFileIfNotExists}
import com.peknight.fs2.tar.{TarArchiveEntry, archive, unarchive}
import com.peknight.http.client.{bodyWithRedirects, path, showProgressInConsole}
import fs2.Stream
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import fs2.io.process.Processes
import fs2.text.utf8
import org.http4s.client.Client
import org.http4s.{Request, Uri}
import org.typelevel.log4cats.Logger

package object custom:
  private val fileName: Path = Path("frp.tar.gz")
  private val containerConfDirectory: Path = Root / etc / frpAppName.value

  def download[F[_]](uri: Uri = url, fileName: Option[Path] = fileName.some, directory: Option[Path] = None)
                    (using Client[F])(using Async[F], Files[F], Compression[F], Console[F]): F[Unit] =
    path(uri, fileName, directory).fold(ApplicativeError[F, Throwable].raiseError(OptionEmpty.label("fileName")))(path =>
      Monad[F].ifM(Files[F].exists(path))(().pure[F], path.createParentDirectories[F]().flatMap { _ =>
        val part: Path = Path(s"$path.part")
        bodyWithRedirects[F](Request[F](uri = uri))()(showProgressInConsole[F])
          .through(Compression[F].gunzip())
          .flatMap(gunzipResult => gunzipResult.content.through(unarchive[F]()))
          .evalMapFilter { entry =>
            val nioPath = entry.name.toNioPath
            if nioPath.getNameCount <= 1 then entry.content.compile.drain.as(none[TarArchiveEntry[F]])
            else entry.copy(name = Path.fromNioPath(nioPath.subpath(1, nioPath.getNameCount))).some.pure[F]
          }
          .through(archive[F]())
          .through(Compression[F].gzip())
          .through(Files[F].writeAll(part))
          .compile
          .drain
          .flatMap(_ => Files[F].move(part, path))
      })
    )

  def dockerfile(command: Command): String =
    val appHome: Path = appHomePath(command, Root)
    s"""
       |FROM $alpine
       |$maintainer
       |WORKDIR $appHome
       |ADD $fileName $appHome
       |RUN mkdir -p $containerConfDirectory && cp $appHome/$command.toml $containerConfDirectory/
       |VOLUME $containerConfDirectory
       |ENTRYPOINT ["./$command", "-c", "$containerConfDirectory/$command.toml"]
    """.stripMargin

  private def appHomePath(command: Command, directory: Path): Path = directory / opt / frpAppName.value / command.directory

  def runFrp[F[_]](home: Path, command: Command, toml: => String, publish: List[PortMapping] = Nil)(using Client[F])
                  (using Async[F], Files[F], Compression[F], Processes[F], Logger[F], Console[F])
  : IorT[F, Error, Boolean] =
    type G[X] = IorT[F, Error, X]
    val appName: AppName = AppName(s"${frpAppName.value}-${command.directory}")
    val image: ImageRepositoryTag = customImage(appName, tag = Tag(version).some)
    val appHome: Path = appHomePath(command, home)
    val context: Path = appHome / docker
    val configDirectory: Path = appHome / conf
    val configTomlPath: Path = configDirectory / s"$command.toml"
    for
      _ <- download[F](url, fileName.some, context.some).asIT
      _ <- configTomlPath.writeFileIfNotExists(Stream(toml).covary[F].through(utf8.encode[F])).asIT
      res <- Monad[G].ifM[Boolean](buildImageIfNotExists[F](image, dockerfile(command), context)())(
        runNetworkApp[F](appName, image)(RunOptions(
          publish = publish,
          restart = RestartPolicy.`unless-stopped`.some,
          volume = List(VolumeMount(configDirectory, containerConfDirectory))
        )),
        false.rLiftIT
      )
    yield
      res
end custom
