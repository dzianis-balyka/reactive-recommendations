package reactive.recommendations.server

import _root_.akka.pattern.ask
//import _root_.akka.actor.ActorSystem
//import _root_.akka.io.IO
//import _root_.akka.util.Timeout

import _root_.akka.actor.ActorSystem
import _root_.akka.io.IO
import _root_.akka.util.Timeout
import org.slf4j.LoggerFactory
import reactive.recommendations.server.akka.ServiceUI
import scopt.OptionParser
import spray.can.Http

import scala.concurrent.duration._

/**
 * Created by d_balyka on 19.11.2014.
 */
object ServerRunner {

  val log = LoggerFactory.getLogger(ServerRunner.getClass)

  val parser = new OptionParser[ServerConfig](ServerRunner.getClass.getName) {
    head("Runs recomendations server")
    help("help") text ("prints this usage text")
    opt[String]('h', "host") action {
      (conf, c) =>
        c.copy(host = conf)
    } text ("host to bind")
    opt[Int]('p', "port") action {
      (conf, c) =>
        c.copy(port = conf)
    } text ("port to bind")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, ServerConfig()) map {
      config =>
        log.info("processing with {}", config)

        //start akka with spray
        implicit val system = ActorSystem("RecommendationSystem")
        val serviceUi = system.actorOf(ServiceUI.props(), "serviceUi")

        implicit val timeout = Timeout(5.seconds)

        IO(Http) ? Http.Bind(serviceUi, interface = config.host, port = config.port)

    } getOrElse {
      // arguments are bad, error message will have been displayed
    }
  }

}

case class ServerConfig(host: String = "localhost", port: Int = 8080)