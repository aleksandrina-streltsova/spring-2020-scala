import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.SttpBackendOptions
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.softwaremill.sttp._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

class BotStarter(override val client: RequestHandler[Future],
                 val service: Service) extends TelegramBot with Polling with Commands[Future] {
  implicit val ec: ExecutionContext = ExecutionContext.global

  onCommand("/start") { implicit msg =>
    msg.from match {
      case None => reply("Ты кто")
      case Some(usr) => service.add_user(usr.id)
    }
    reply("You've been registered!").void
  }

  onCommand("/users") { implicit msg =>
    reply(service.get_users()).void
  }

  onCommand("/send") { implicit msg =>
    withArgs { args =>
      val id = if (args.isEmpty) ??? else args(0).toInt
      val message = if (args.size < 2) ??? else args(1)
      service.send_message(id, message)
      reply("Sended").void
    }
  }

  onCommand("/check") { implicit msg =>
    val id : Int = msg.from match {
      case None => ???
      case Some(usr) => usr.id
    }
    reply(service.get_messages(id)).void
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

    val token = "1079914748:AAFa1jyE21HbWSQCcVoa0rMG0Awaeje6kPs"
    val bot = new BotStarter(new FutureSttpClient(token), new ServiceRest(backend))

    Await.result(bot.run(), Duration.Inf)
  }
}
