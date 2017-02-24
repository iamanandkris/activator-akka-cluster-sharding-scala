package sample.blog

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
  * Created by anand on 24/02/17.
  */
object ClientApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2551", "2552", "0"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    println("The ports are - " + ports )
    ports foreach { port =>
      //val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load())

      val config = ConfigFactory.parseString(s"""akka {
          remote {
              netty.tcp {
              port = ${port}
            }
          }
          cluster {
            roles = ["MyWorker","AccountEntity"]
          }
        }""").withFallback(ConfigFactory.load())

      // Create an Akka system
      val system = ActorSystem("AnotherActorSystem", config)
      if (port != "2551" && port != "2552")
        system.actorOf(Client.props, "client")
    }
  }
}
