package com.example.webservice.pingpong

import akka.actor.{ Actor, ActorLogging, OneForOneStrategy, Props }
import akka.actor.SupervisorStrategy._

class PersistentPingPongSupervisor extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Killed  => Stop
    case _: Faint   => Resume
    case _: Injured => Restart
  }

  val persistentPingPong = context.actorOf(PersistentPingPong.props, PersistentPingPong.getClass.getSimpleName)

  def receive: Receive = {
    case msg @ _  =>
      log.info("forwarding message {} to child.", msg)
      persistentPingPong forward msg
  }
}

object PersistentPingPongSupervisor {
  def props: Props = Props[PersistentPingPongSupervisor]
}
