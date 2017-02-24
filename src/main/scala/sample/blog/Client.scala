package sample.blog

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorPaths, ActorRef, Props}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import sample.blog.AccountEntity.Account
import sample.blog.Client.Tick

import scala.concurrent.duration._


object Client{
  def props:Props = Props(new Client)
  private case object Tick
}
class Client extends Actor with ActorLogging{
  import context.dispatcher

  val tickTask = context.system.scheduler.schedule(3.seconds, 3.seconds, self, Tick)

  override def postStop(): Unit = {
    super.postStop()
    tickTask.cancel()
  }

  def initialContacts = {
    Set(/*ActorPaths.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/system/receptionist"),*/
        ActorPaths.fromString("akka.tcp://ClusterSystem@127.0.0.1:2552/system/receptionist"))
  }

  val clientOfReceptionist = context.actorOf(ClusterClient.props(
    ClusterClientSettings.create(context.system).withInitialContacts(initialContacts)),"client");


  def receive = create

  val create: Receive = {
    case Tick =>
      val accountId = UUID.randomUUID().toString


      val createMessage = AccountEntity.Create(Account(s"name-${accountId}",s"chs-${accountId}","ROOT",List("Anand","Nathan","Marek")),accountId)
      log.info(s"Create from client - ${createMessage}")
      clientOfReceptionist tell(new ClusterClient.Send("/user/receptionist", createMessage, false), ActorRef.noSender)

      context.become(edit(accountId))
  }

  def edit(accountId: String): Receive = {
    case Tick =>
      val updateNameMsg = AccountEntity.UpdateName(s"NewName-${accountId}",accountId)
      log.info(s"Update Name from client - ${updateNameMsg}")
      clientOfReceptionist tell(new ClusterClient.Send("/user/receptionist", updateNameMsg, false), ActorRef.noSender)
      context.become(publish(accountId))
  }

  def publish(accountId: String): Receive = {
    case Tick =>
      val updateOwnerMsg = AccountEntity.UpdateOwner(s"NewOwner-${accountId}",accountId)
      log.info(s"Update Owner from client - ${updateOwnerMsg}")
      clientOfReceptionist tell(new ClusterClient.Send("/user/receptionist", updateOwnerMsg, false), ActorRef.noSender)
      context.become(create)
  }
}
