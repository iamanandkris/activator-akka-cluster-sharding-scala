package sample.blog

import akka.actor.{ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.ConfigFactory


object AccountApp {
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
      val system = ActorSystem("ClusterSystem", config)

      ClusterSharding(system).start(
        typeName = AccountEntity.shardName,
        entityProps = AccountEntity.props,
        settings = ClusterShardingSettings(system),
        extractEntityId = AccountEntity.idExtractor,
        extractShardId = AccountEntity.shardResolver)

    }
  }
}
