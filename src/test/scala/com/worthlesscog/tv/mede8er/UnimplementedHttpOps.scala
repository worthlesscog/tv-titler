package com.worthlesscog.tv.mede8er

import com.worthlesscog.tv.{HttpOps, Maybe, Pairs}
import spray.json.JsValue

trait UnimplementedHttpOps extends HttpOps {

    def getBytes(url: String, parameters: Pairs, headers: Pairs) = ???

    def getJson(url: String, parameters: Pairs, headers: Pairs) = ???

    def httpCode(url: String, parameters: Pairs)(code: Int) = ???

    def maybe[T](convert: JsValue => Maybe[T])(url: String, parameters: Pairs, headers: Pairs) = ???

    def pages[T](convert: JsValue => Seq[T], continue: (Int, JsValue) => Boolean)(url: String, parameters: Pairs, headers: Pairs) = ???

    def postJsonToJson(url: String, value: JsValue, headers: Pairs) = ???

}
