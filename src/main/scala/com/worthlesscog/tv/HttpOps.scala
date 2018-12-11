package com.worthlesscog.tv

import java.net.HttpURLConnection.HTTP_OK

import scalaj.http.{Http, HttpResponse}
import spray.json.{JsonParser, JsValue}

import scala.util.Try

object HttpOps {

    val encoding = ("Accept-Charset", "utf-8")
    val jsonContent = ("Content-Type", "application/json")

    def getBytes(url: String, parameters: Pairs = Nil, headers: Pairs = Nil) =
        Try { get(url, parameters, headers).asBytes |> either } |> tail(identity, url, parameters)

    private def get(url: String, parameters: Pairs, headers: Pairs) =
        Http(url) headers (headers :+ encoding) params parameters

    private def either[T](response: HttpResponse[T]): Either[Int, T] =
        Either.cond(response.code == HTTP_OK, response.body, response.code)

    private def tail[I, O](convert: I => O, url: String, parameters: Pairs = Nil)(t: Try[Either[Int, I]]) =
        t fold(
            exception(url, parameters),
            _ fold(
                lr,
                convert(_) |> rr))

    private def exception(url: String, parameters: Pairs)(t: Throwable) =
        t.toString |> fail(url, parameters)

    private def fail(url: String, parameters: Pairs)(s: String) =
        s + " - " + fullUrl(url, parameters) + "\n" |> asLeft

    private def fullUrl(url: String, parameters: Pairs) =
        if (parameters isEmpty)
            url
        else
            url + (parameters.map { case (n, v) => n + "=" + v } mkString("?", "&", ""))

    def getJson(url: String, parameters: Pairs = Nil, headers: Pairs = Nil) =
        Try { get(url, parameters, headers).asString |> either } |> tail(json, url, parameters)

    private def json(j: String) = JsonParser(j)

    def httpCode(url: String, parameters: Pairs = Nil)(code: Int) =
        "HTTP " + code.toString |> fail(url, parameters)

    def postJsonToJson(url: String, value: JsValue, headers: Pairs = Nil) =
        Try { post(url, value.toString, headers :+ jsonContent).asString |> either } |> tail(json, url)

    private def post(url: String, data: String, headers: Pairs) =
        Http(url) headers (headers :+ encoding) postData data

}
