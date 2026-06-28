package com.peknight.frp.server.config

import com.comcast.ip4s.Port
import com.peknight.auth.Token

case class FrpServerConfig(bindPort: Option[Port] = None, authToken: Option[Token] = None):
  def toml: String =
    List(
      bindPort.map(port => s"""bindPort = $port"""),
      authToken.map(token => s"""auth.token = "${token.token}"""")
    ).collect { case Some(line) => line }.mkString("\n")
end FrpServerConfig
