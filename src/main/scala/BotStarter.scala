import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.SttpBackendOptions
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.softwaremill.sttp._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class BotStarter(override val client: RequestHandler[Future],
                 val service: Service) extends TelegramBot with Polling with Commands[Future] {
  implicit val ec: ExecutionContext = ExecutionContext.global

  onCommand("/start") { implicit msg =>
    msg.from match {
      case None => reply("Ты кто").map(_ => Unit)
      case Some(usr) => service.addUser(usr.id).map(_ => reply("You've been registered!")).map(_ => Unit)
    }
  }

  onCommand("/users") { implicit msg =>
    service.getUsers().flatMap(reply(_)).void
  }

  onCommand("/send") { implicit msg =>
    withArgs { args =>
      val id = if (args.isEmpty) ??? else args.head.toInt
      val message = if (args.size < 2) ??? else args(1)
      service.sendMessage(id, message).map(_ => reply("Sent")).map(_ => Unit)
    }
  }

  onCommand("/check") { implicit msg =>
    val id: Int = msg.from match {
      case None => ???
      case Some(usr) => usr.id
    }
    service.getMessages(id).flatMap(reply(_)).void
  }

  onCommand("/cats") { implicit msg =>
    service.link().flatMap(reply(_)).void
  }
}

object BotStarter {
  def main(args: Array[String]): Unit = {

    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val backend: SttpBackend[Future, Nothing] = OkHttpFutureBackend(
      SttpBackendOptions.Default.socksProxy("ps8yglk.ddns.net", 11999)
    )

    implicit val db: Database = Database.forConfig("h2mem1")

    val token = "1079914748:AAFa1jyE21HbWSQCcVoa0rMG0Awaeje6kPs"
    val serviceRest = new ServiceRest(backend)
    serviceRest.init()
    val bot = new BotStarter(new FutureSttpClient(token), serviceRest)

    Await.result(bot.run(), Duration.Inf)
  }
}
