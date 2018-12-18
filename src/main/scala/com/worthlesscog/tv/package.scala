package com.worthlesscog

import java.io.IOException
import java.nio.file.Paths

import com.typesafe.scalalogging.Logger
import spray.json.{JsString, JsValue, RootJsonFormat}

package object tv {

    type Maybe[T] = Either[String, T]
    type Pairs = Seq[(String, String)]

    implicit class Approx(d: Option[Double]) {
        def ~=(e: Double) = d match {
            case Some(d) => (d - e).abs <= 0.02
            case None    => false
        }
    }

    object Optionally {
        def unapply[T](t: T) = if (t == null) Some(None) else Some(Some(t))
    }

    implicit class Or[T](t: Option[T]) {
        def or(t2: Option[T]) = if (t nonEmpty) t else t2
        def or(t2: T) = t getOrElse t2
    }

    implicit class Pipe[A](a: A) {
        def |>[B](f: A => B): B = f(a)
    }

    implicit object optionStringFormat extends RootJsonFormat[Option[String]] {
        def read(v: JsValue): Option[String] = v match {
            case JsString(s) => Some(s)
            case _           => None
        }

        def write(s: Option[String]) = ???
    }

    lazy val home = Paths.get(System.getProperty("user.home"))
    lazy val log = Logger("Titler")

    def asInt(s: String) =
        try {
            s.toInt |> asRight
        } catch {
            case _: NumberFormatException => s"$s is not a valid id\n" |> asLeft
        }

    def asLeft[T](t: T) = Left(t)
    def asRight[T](t: T) = Right(t)

    def lr[T](t: T) = t |> asLeft |> asRight
    def rr[T](t: T) = t |> asRight |> asRight

    def info(s: String) = log.info(s)

    def leftException(x: Exception) = x.toString + "\n" |> asLeft

    def maybeIO[A](f: => Maybe[A]) =
        try
            f
        catch {
            case x: IOException => leftException(x)
        }

    def using[A <: {def close() : Unit}, B](closeable: A)(f: A => B): B =
        try f(closeable) finally closeable.close()

}
