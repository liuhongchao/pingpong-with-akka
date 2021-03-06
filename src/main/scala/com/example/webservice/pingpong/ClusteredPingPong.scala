package com.example.webservice.pingpong

import java.net.InetAddress

import akka.actor.{Actor, Props, RootActorPath}
import akka.cluster.{Cluster, MemberStatus}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ClusteredPingPong extends Actor {
  val log = Logging(context.system, this)

  implicit val executionContext = context.dispatcher
  implicit val askTimeout = Timeout(5.second)

  private val cluster = Cluster.get(context.system)
  private val hostname = InetAddress.getLocalHost.getHostName

  var ballsSeen = 0
  override def preRestart(reason: Throwable, message: Option[Any]) = {
    log.info(s"${ClusteredPingPong.getClass.getSimpleName} is restarted because of $reason")
  }

  override def receive: Receive = {
    case ball: Ball => {
      ballsSeen += 1
      ball match {
        case PingPongball => sender ! PingPongball
        case Basketball   => sender ! Status("What a big ball, I am fainted..."); throw new Faint()
        case Fireball     => sender ! Status("Got fire, I am severely injured..."); throw new Injured()
        case Bullet       => sender ! Status("Seriouly, a bullet? I am dead... :("); throw new Killed()
      }
    }
    case BallsSeen  => sender ! Status(s"seen $ballsSeen balls", Some(hostname))

    case ToAll(msg) =>
      val s = sender
      sendToAll(msg).onComplete {
        case Success(statuses) => s ! statuses
        case Failure(ex)       => throw ex
      }
  }

  private def sendToAll(msg: Payload): Future[List[Payload]] = {
    val nodeAddrs = cluster.state.members
      .filter(_.status == MemberStatus.Up)
      .map(_.address)
      .toList

    val responses = nodeAddrs.map { addr =>
      val nodePath = RootActorPath(addr)
      val actor = context.actorSelection(nodePath / "user" / "ClusteredPingPongSupervisor")
      log.info(s"sending to $actor")
      (actor ? msg).mapTo[Payload]
    }

    Future.sequence(responses)
  }
}

object ClusteredPingPong {
  def props: Props = Props[ClusteredPingPong]
}
