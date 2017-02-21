package sample.blog

import akka.actor.{ActorLogging, PoisonPill, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.{PersistentActor, RecoveryCompleted}
import sample.blog.AccountEntity._

object AccountEntity {
  def props:Props = Props(new AccountEntity)

  def name(accountId: Long) = accountId.toString
  case class Account(name:String, owner:String, actype:String, members:List[String])
  case class MyState(account: Option[Account], noOfReincarnation:Int, entityState:EntityState = Servable)

  trait Event
  case class AccountCreated(act:Account) extends Event
  case object AccountServable extends Event
  case class NameUpdated(name:String) extends Event
  case class OwnerUpdated(owner:String) extends Event
  case class MemberUpdated(members:List[String]) extends Event
  case object IncreaseIncarnation extends Event

  //Does not affect state
  case class AccountAlreadyPresent(name:String) extends Event
  case class CanNotPerform(reason:String) extends Event

  //commands
  trait Command {
    def accountId: String
  }
  case class Create(act:Account, accountId:String) extends Command
  case class UpdateName(name:String, accountId:String)  extends Command
  case class UpdateOwner(owner:String, accountId:String)  extends Command
  case class UpdateMember(members:List[String], accountId:String)  extends Command

  trait EntityState
  case object Servable extends EntityState
  case object NonServable extends EntityState

  val shardName: String = "Account"

  val idExtractor: ShardRegion.ExtractEntityId = {
    case cmd: Command => ("account-" + cmd.accountId, cmd)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.accountId.foldLeft(0){(a,b) => a+b} % 5).toString
  }
}

class AccountEntity extends PersistentActor with ActorLogging {
  override def persistenceId: String = self.path.parent.name + "<-->" + self.path.name

  var state = MyState(None,0)

  def update(ev:Event) = ev match {
    case x:AccountCreated => state = state.copy(account=Some(x.act))
    case NameUpdated(nm) => {
      val tst = state.account.get
      state = state.copy(account = Some(tst.copy(name = nm)))
    }
    case OwnerUpdated(owr) =>{
      val tst = state.account.get
      state = state.copy(account = Some(tst.copy(owner = owr)))
    }
    case MemberUpdated(mbr) =>{
      val tst = state.account.get
      state = state.copy(account = Some(tst.copy(members = mbr)))
    }
    case AccountServable => state = state.copy(entityState = Servable)
    case IncreaseIncarnation => state = state.copy(noOfReincarnation = state.noOfReincarnation + 1)
    case _ => //No Action on
  }

  override def receiveRecover = {
    case event:Event => update(event)
    case RecoveryCompleted => update(IncreaseIncarnation)
  }

  override def receiveCommand = {
    case account@ Create(x,y) => {
      log.info("Create Message Received")
      val eventToPersist = if (state.entityState == Servable) List(AccountCreated(account.act), AccountServable)
      else List(AccountAlreadyPresent(state.account.get.name))

      persistAll(eventToPersist)(update)
    }
    case name@ UpdateName(x,y) => {
      log.info("Update Name Message Received")
      val eventToPersist = if (state.entityState == Servable) NameUpdated(name.name)
      else CanNotPerform("Account not created")

      persist(eventToPersist)(update)
    }
    case owner@ UpdateOwner(x,y) => {
      log.info("Create Owner Message Received")
      val eventToPersist = if (state.entityState == Servable) OwnerUpdated(owner.owner)
      else CanNotPerform("Account not created")

      persist(eventToPersist)(update)
    }
    case member@ UpdateMember(x,y) => {
      val eventToPersist = if (state.entityState == Servable) MemberUpdated(member.members)
      else CanNotPerform("Account not created")

      persist(eventToPersist)(update)
    }
  }

  override def unhandled(msg: Any): Unit = msg match {
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = PoisonPill)
    case _              => super.unhandled(msg)
  }
}
