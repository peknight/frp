package com.peknight.frp

import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.{ApplicativeError, Monad}
import com.peknight.app.build.fatedier.frp.url
import com.peknight.error.option.OptionEmpty
import com.peknight.fs2.io.syntax.path.createParentDirectories
import com.peknight.fs2.tar.{TarArchiveEntry, archive, unarchive}
import com.peknight.http.client.{bodyWithRedirects, path, showProgressInConsole}
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import org.http4s.client.Client
import org.http4s.{Request, Uri}

package object custom:
  def download[F[_]](uri: Uri = url, fileName: Option[Path] = None, directory: Option[Path] = None)
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
end custom
