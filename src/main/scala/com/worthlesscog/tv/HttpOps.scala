package com.worthlesscog.tv

import spray.json.JsValue

trait HttpOps {

    def getBytes(url: String, parameters: Pairs = Nil, headers: Pairs = Nil): Either[String, Either[Int, Array[Byte]]]

    def getJson(url: String, parameters: Pairs = Nil, headers: Pairs = Nil): Either[String, Either[Int, JsValue]]

    def httpCode(url: String, parameters: Pairs = Nil)(code: Int): Left[String, Nothing]

    def maybe[T](convert: JsValue => Maybe[T])(url: String, parameters: Pairs = Nil, headers: Pairs = Nil): Maybe[T]

    def pages[T](convert: JsValue => Seq[T], continue: (Int, JsValue) => Boolean)(url: String, parameters: Pairs = Nil, headers: Pairs = Nil): Maybe[Seq[T]]

    def postJsonToJson(url: String, value: JsValue, headers: Pairs = Nil): Either[String, Either[Int, JsValue]]

}
