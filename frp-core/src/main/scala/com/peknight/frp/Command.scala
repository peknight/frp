package com.peknight.frp

enum Command(val directory: String):
  case frpc extends Command("client")
  case frps extends Command("server")
end Command
