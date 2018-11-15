package com.github.jimmydivvy.http4s.undertow

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset

import cats.effect.Async
import io.undertow.io.{IoCallback, Sender}
import io.undertow.server.HttpServerExchange

trait SenderF[F[_]] {
  def send(byteBuffer: ByteBuffer):F[Unit]
  def send(byteBuffers: Array[ByteBuffer]):F[Unit]

  def send(string:String):F[Unit]
  def send(string:String, charset: Charset):F[Unit]

  def close():F[Unit]
}


class SenderImpl[F[_] : Async]( sender: Sender ) extends SenderF[F] {

  def wrap1[T]( fn: Function2[T, IoCallback, Unit] )(t:T):F[Unit] = {
    wrap { iocallback =>
      fn(t, iocallback)
    }
  }

  def wrap( fn: IoCallback => Unit):F[Unit] = {
    Async[F].async { cb =>
      val iocb = new IoCallback {
        override def onComplete(
            exchange: HttpServerExchange,
            sender: Sender): Unit = cb(Right(()))

        override def onException(exchange: HttpServerExchange, sender: Sender, exception: IOException): Unit = {
          cb(Left(exception))
        }
      }

      fn( iocb )
    }
  }

  override def send(byteBuffer: ByteBuffer): F[Unit] = {
    wrap { ioCallback =>
      sender.send( byteBuffer, ioCallback )
    }
  }

  override def send(byteBuffers: Array[ByteBuffer]): F[Unit] = {
    wrap { ioCallback =>
      sender.send( byteBuffers, ioCallback )
    }
  }


  override def send(string: String): F[Unit] = {
    wrap { ioCallback =>
      sender.send( string, ioCallback )
    }
  }

  override def send(string: String, charset: Charset): F[Unit] = {
    wrap { ioCallback =>
      sender.send( string, charset, ioCallback )
    }
  }

  override def close(): F[Unit] = {
    wrap { ioCallback =>
      //println("Closing....")
      sender.close( ioCallback )
    }
  }
}
