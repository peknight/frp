package com.peknight.frp.client.config

import com.comcast.ip4s.{IpAddress, Port}
import com.peknight.frp.ProxyType

case class ProxyConfig(name: String, `type`: ProxyType, localIP: IpAddress, localPort: Port, remotePort: Port):
  def toml: String =
    s"""
       |[[proxies]]
       |name = "$name"
       |type = "${`type`}"
       |localIP = "$localIP"
       |localPort = $localPort
       |remotePort = $remotePort
       |
    """.stripMargin

end ProxyConfig
