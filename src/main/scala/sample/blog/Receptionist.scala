package sample.blog

import akka.actor.{Actor, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.ClusterSharding

/**
  * Created by anand on 24/02/17.
  */

object Receptionist{
  def props:Props = Props(new Receptionist)

}

class Receptionist extends Actor {
  val accountRegion = ClusterSharding(context.system).shardRegion(AccountEntity.shardName)

  ClusterClientReceptionist.get(context.system).registerService(self)

  override def receive = {
    case x:AccountEntity.Command => {
      println(s"Sending the message - ${x} to account")
      accountRegion ! x
    }
    case x @ _ => println(s"Cant send the message - ${x}")
  }
}
