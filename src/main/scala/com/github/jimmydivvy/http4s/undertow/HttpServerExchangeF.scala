package com.github.jimmydivvy.http4s.undertow

import cats.effect.Async
import io.undertow.server.HttpServerExchange
import io.undertow.util.HeaderMap

/**
  * Pure wrapper around HttpServerExchange.
  *
  * TODO - Lots of methods to wrap.
  */
trait HttpServerExchangeF[F[_]] {

  // FIXME - Remove this
  // This is only here so I can prototype without needing to wrap everything
  def inner : HttpServerExchange

  def endExchange():F[Unit]

  def getResponseSender():F[SenderF[F]]

  def getRequestHeaders():HeaderMap
}

class HttpServerExchangeImpl[F[_] : Async]( val inner: HttpServerExchange ) extends HttpServerExchangeF[F] {

  override def getRequestHeaders(): HeaderMap = inner.getRequestHeaders

  override def endExchange(): F[Unit] = Async[F].delay {
    inner.endExchange()
  }

  override def getResponseSender(): F[SenderF[F]] = Async[F].delay {
    new SenderImpl[F](inner.getResponseSender)
  }


}
