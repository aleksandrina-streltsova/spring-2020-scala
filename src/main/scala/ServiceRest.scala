import com.softwaremill.sttp._

import scala.concurrent.{ExecutionContext, Future}
import com.softwaremill.sttp.json4s._
import org.json4s.native.Serialization

import scala.collection.mutable
import scala.util.Random

case class CatResponse(data: List[Data])

case class Data(images: List[InnerData])

case class InnerData(link: String)

trait Service {
  def link(): Future[String]

  def addUser(id: Int)

  def getUsers(): String

  def sendMessage(id: Int, message: String)

  def getMessages(id: Int): String
}

trait Randomizer {
  def random(n: Int): Int
}

object RandomizerStub extends Randomizer {
  override def random(n: Int): Int = Random.nextInt(n)
}

class ServiceRest(val backend: SttpBackend[Future, Nothing])(implicit val ec: ExecutionContext, r: Randomizer = RandomizerStub) extends Service {
  implicit val serialization: Serialization.type = org.json4s.native.Serialization

  val users: mutable.MutableList[Int] = mutable.MutableList[Int]()
  val messages: mutable.MutableList[(Int, String)] = mutable.MutableList[(Int, String)]()

  override def link(): Future[String] = {
    val request = sttp
      .header("Authorization", "Client-ID e9c5a46ce98ff9a")
      .get(uri"https://api.imgur.com/3/gallery/search?q=cats")
      .response(asJson[CatResponse])
    backend.send(request).map { response =>
      val images = response.unsafeBody.data.flatMap(_.images)
      val index = r.random(images.length)
      images(index).link
    }
  }

  override def addUser(id: Int): Unit = {
    if (!users.contains(id))
      users += id
  }

  override def getUsers(): String =
    users.mkString(", ")

  override def sendMessage(id: Int, message: String): Unit = {
    messages += id -> message
  }

  override def getMessages(id: Int): String =
    messages.filter(_._1 == id).map(_._2).mkString(", ")
}