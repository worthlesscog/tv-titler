package com.worthlesscog.tv.data

case class Credentials(
    user: Option[String] = None,
    password: Option[String] = None,
    token: Option[String] = None
)
