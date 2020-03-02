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
import scala.concurrent.{Await, ExecutionContext, Future}
import com.softwaremill.sttp.json4s._

class BotStarter(override val client: RequestHandler[Future]) extends TelegramBot
  with Polling
  with Commands[Future] {
  val users: mutable.MutableList[Int] = mutable.MutableList[Int]()
  val messages: mutable.MutableList[(Int, String)] = mutable.MutableList[(Int, String)]()

  onCommand("/start") { implicit msg =>
    msg.from match {
      case None => reply("Ты кто")
      case Some(usr) => users += usr.id
    }

    reply("You've been registered!").void
  }

  onCommand("/users") { implicit msg =>
    reply(users.mkString(", ")).void
  }

  onCommand("/send") { implicit msg =>
    withArgs { args =>
      val id = if (args.isEmpty) ??? else args(0).toInt
      val message = if (args.size < 2) ??? else args(1)
      messages += id -> message
      reply("Sended").void
    }
  }

  onCommand("/check") { implicit msg =>
    val id : Int = msg.from match {
      case None => ???
      case Some(usr) => usr.id
    }
    reply(messages.filter(_._1 == id).map(_._2).mkString(", ")).void
  }

}

object BotStarter {
  def main(args: Array[String]): Unit = {
    implicit val serialization = org.json4s.native.Serialization

    case class Response(data: List[Data])
    case class Data(id: String, title: String)

    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val backend = OkHttpFutureBackend(
      SttpBackendOptions.Default.socksProxy("ps8yglk.ddns.net", 11999)
    )
    val request = sttp
      .header("Authorization", "Client-ID e9c5a46ce98ff9a")
      .get(uri"https://api.imgur.com/3/gallery/search?q=cats")
      .response(asJson[Response])
    val res = backend.send(request).map { response =>
      println(response.unsafeBody)
    }

    val token = "1079914748:AAFa1jyE21HbWSQCcVoa0rMG0Awaeje6kPs"
    val bot = new BotStarter(new FutureSttpClient(token))
    Await.result(bot.run(), Duration.Inf)

  }
}
