package com.github.jimmydivvy.http4s.undertow

import java.util.concurrent.Executors

import cats.effect.syntax.effect._
import cats.effect.{Effect, IO}
import io.undertow.server.{HttpHandler, HttpServerExchange}


/**
  * Functional wrapper around Undertow HttpHandler
  */
trait UndertowHandler[F[_]] {

  /**
    * Handle the specified request
    */
  def handleRaw( exchange: HttpServerExchangeF[F] ):F[Unit]
}

/**
  * Base class for Undertow wrappers using cats-effect
  */
abstract class LowLevelUndertowHandler[F[_] : Effect] extends UndertowHandler[F] {

  /**
    * Handle the specified request
    */
  def handleRaw( exchange: HttpServerExchangeF[F] ):F[Unit]

  //val pool = Executors.newCachedThreadPool()

  /**
    * Build a native undertow HttpHandler
    */
  def build():HttpHandler = new HttpHandler {
    override def handleRequest(exchange: HttpServerExchange): Unit = {

      // Mark this exchange as `dispatched`
      // This lets is write to it asynchronously from other threads without
      // undertow automatically closing everything down once this method etxits
      exchange.dispatch()

      // Create a wrapped HttpExchange
      val fpExchange = new HttpServerExchangeImpl[F]( exchange )

      // Execute the actual effect.
      handleRaw( fpExchange ).runAsync {
        case Right(_) => IO.unit
        case Left(e)  => IO.raiseError(e)
      }.unsafeRunSync()
    }
  }
}
