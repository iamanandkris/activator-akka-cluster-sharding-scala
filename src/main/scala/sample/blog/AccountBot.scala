package sample.blog

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ClusterSharding

/**
  * Created by anand on 21/02/17.
  */
class AccountBot extends Actor with ActorLogging {
  val postRegion = ClusterSharding(context.system).shardRegion(AccountEntity.shardName)

  def receive ={
    case an:Any => println ("The message received - " + an)
  }
}
