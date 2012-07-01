package de.freeky.web.lib

import org.apache.xmlrpc._
import java.util.Vector

class ManiaPlanetServer(host: String, port: Int, user: String, pass: String) {
	val server = new XmlRpcClient("http://%s:%d".format(host, port))
	println(server.execute("system.listMethods", new Vector()))
}

object ManiaPlanetServer {
  var servers = List[ManiaPlanetServer]()
}