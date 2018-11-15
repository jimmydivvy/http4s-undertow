package com.github.jimmydivvy.http4s.undertow

import cats.effect.Effect
import io.undertow.server.HttpServerExchange
import io.undertow.util.{HttpString, Methods}
import org.http4s.{Headers, HttpApp, Method, Request, Uri}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.syntax.effect._

/**
  * Creates an Undertow handler for Http4s Services.
  */
class UndertowHttp4sHandler[F[_] : Effect](
    service: HttpApp[F]
) extends LowLevelUndertowHandler[F] {

  /**
    * Create an HTTP4s Request from the undertow exchange
    */
  protected def createHttp4sRequest( exchangeF: HttpServerExchangeF[F] ):Request[F] = {

    // TODO - Most things

    /*
    method: Method = Method.GET,
      uri: Uri = Uri(path = "/"),
      httpVersion: HttpVersion = HttpVersion.`HTTP/1.1`,
      headers: Headers = Headers.empty,
      body: EntityBody[F] = EmptyBody,
      attributes: AttributeMap = AttributeMap.empty
     */


    val method = exchangeF.inner.getRequestMethod match {
      case Methods.GET     => Method.GET
      case Methods.POST    => Method.POST
      case Methods.PUT     => Method.PUT
      case Methods.HEAD    => Method.HEAD
      case Methods.OPTIONS => Method.OPTIONS
      // TODO - The rest
      // FIXME - This is probably going to be slow.
    }

    Request.apply[F](
      method = method,
      uri = Uri(path=exchangeF.inner.getRequestURI),
    )
  }

  /**
    * Handle the undertow request by passing it to the Http4s app.
    */
  override def handleRaw(exchange: HttpServerExchangeF[F]): F[Unit] = {

    // TODO - Error handling. resource cleanup.

    // Build an Http4s compatible request
    // FIXME - This should probalby be inside F[_] for error tracking
    val request = createHttp4sRequest( exchange )

    for {
      // Call our http4s service and get a response
      response <- service.run( request )


      // HACKHACK FIXME - Set headers properly
      _ = {
        val responseHeaders = exchange.inner.getResponseHeaders

        response.headers.foreach { header =>
          responseHeaders.add( new HttpString(header.name.toString()), header.value )
        }
      }

      // Get a reference to the Undertow response sender
      sender  <- exchange.getResponseSender()

      // Write each chunk to the undertow sender.
      _  <- response.body.chunks
        .evalTap { chunk => sender.send( chunk.toByteBuffer ) }
        .compile.drain

      // Close the sender, and end the exchange.
      _ <- sender.close()
      _ <- exchange.endExchange()
    } yield ()

  }
}
