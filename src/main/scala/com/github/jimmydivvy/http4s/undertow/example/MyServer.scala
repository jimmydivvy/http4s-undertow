package com.github.jimmydivvy.http4s.undertow.example

import cats.effect._

import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.syntax.kleisli._
import org.http4s.server.Router

import io.undertow.Undertow

import com.github.jimmydivvy.http4s.undertow.UndertowHttp4sHandler

object MyServer extends IOApp {

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root => Ok("Boop")
    case GET -> Root / "hello" => Ok("world")
  }

  val router = Router("/" -> helloWorldService).orNotFound

  val handler = new UndertowHttp4sHandler[IO](router)

  override def run(args: List[String]): IO[ExitCode] = {
    val server = Undertow.builder()
      .addHttpListener(8080, "localhost")
      .setHandler( handler.build() )
      .build()


    println("Starting server")
    server.start()

    IO(ExitCode.Success)
  }

}
