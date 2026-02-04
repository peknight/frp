package com.peknight.frp.client.config

import com.comcast.ip4s.{Host, Port}
import com.peknight.auth.token.Token.Token

case class FrpClientConfig(serverAddr: Option[Host] = None,
                           serverPort: Option[Port] = None,
                           authToken: Option[Token] = None,
                           proxies: List[ProxyConfig] = Nil):
  def toml: String =
    val configs: List[String] = List(
      serverAddr.map(addr => s"""serverAddr = "$addr""""),
      serverPort.map(port => s"""serverPort = $port"""),
      authToken.map(token => s"""auth.token = "${token.token}"""")
    ).collect { case Some(line) => line }
    (configs ::: proxies.map(_.toml)).mkString("\n")
end FrpClientConfig
