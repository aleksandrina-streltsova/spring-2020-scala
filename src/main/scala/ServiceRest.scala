import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicInteger

import com.softwaremill.sttp._

import scala.concurrent.{ExecutionContext, Future}
import com.softwaremill.sttp.json4s._
import org.json4s.native.Serialization
import slick.jdbc.H2Profile.api._
import java.util.{Calendar, Date}

import scala.util.Random

case class CatResponse(data: List[Data])

case class Data(images: List[InnerData])

case class InnerData(link: String)

trait Service {
  def init(): Future[Unit]

  def link(userid: Int): Future[String]

  def addUser(id: Int): Future[Unit]

  def getUsers(): Future[String]

  def sendMessage(id: Int, message: String): Future[Unit]

  def getMessages(id: Int): Future[String]

  def getStats(id: Int): Future[String]
}

trait Randomizer {
  def random(n: Int): Int
}

trait DateGetter {
  def date(): String
}

object RandomizerStub extends Randomizer {
  override def random(n: Int): Int = Random.nextInt(n)
}

object DateGetterStub extends DateGetter {
  val format = new SimpleDateFormat("d-M-y")

  override def date(): String = format.format(Calendar.getInstance().getTime())
}

class ServiceRest(val backend: SttpBackend[Future, Nothing])(implicit val ec: ExecutionContext, r: Randomizer = RandomizerStub, t: DateGetter = DateGetterStub, db: Database) extends Service {

  val users = TableQuery[Users]
  val messages = TableQuery[Messages]
  val linksToUsers = TableQuery[LinksToUsers]

  private val max_id = new AtomicInteger(0)

  implicit val serialization: Serialization.type = org.json4s.native.Serialization

  override def init(): Future[Unit] =
    for {
      _ <- db.run(users.schema.createIfNotExists)
      _ <- db.run(messages.schema.createIfNotExists)
      _ <- db.run(linksToUsers.schema.createIfNotExists)
    } yield ()

  override def link(userid: Int): Future[String] = {
    val request = sttp
      .header("Authorization", "Client-ID e9c5a46ce98ff9a")
      .get(uri"https://api.imgur.com/3/gallery/search?q=cats")
      .response(asJson[CatResponse])
    val dt = t.date()
    for {
      catlink <- backend.send(request).map { response =>
        val images = response.unsafeBody.data.flatMap(_.images)
        val index = r.random(images.length)
        images(index).link
      }
      _ <- clearOldLinks()
      _ <- db.run(linksToUsers.insertOrUpdate(max_id.getAndIncrement(), catlink, userid, dt)).map(_ => Unit)
    } yield catlink
  }

  override def addUser(id: Int): Future[Unit] =
    db.run(users.insertOrUpdate(id)).map(_ => Unit)

  override def getUsers(): Future[String] =
    db.run(users.result).map(x => x.mkString(", "))

  override def sendMessage(id: Int, message: String): Future[Unit] =
    db.run(messages.insertOrUpdate(max_id.getAndIncrement(), id, message)).map(_ => Unit)

  override def getMessages(id: Int): Future[String] =
    db.run(messages.filter(_.to_id === id).map(_.text).result).map(x => x.mkString(", "))

  def clearOldLinks(): Future[Unit] =
    db.run(linksToUsers.filter(_.date =!= t.date()).delete).map(_ => Unit)

  override def getStats(id: Int): Future[String] =
    for {
      _ <- clearOldLinks()
      res <- db.run(linksToUsers.filter(_.userid === id).map(_.link).result).map(x => x.mkString(", "))
    } yield res
}


class Users(tag: Tag) extends Table[(Int)](tag, "USERS") {
  def id = column[Int]("ID", O.PrimaryKey)
  def * = (id)
}

class Messages(tag: Tag) extends Table[(Int, Int, String)](tag, "MESSAGES") {
  def id = column[Int]("ID", O.PrimaryKey)
  def to_id = column[Int]("TO_ID")
  def text = column[String]("TEXT")
  def * = (id, to_id, text)
}

class LinksToUsers(tag: Tag) extends Table[(Int, String, Int, String)](tag, "LINKSTOUSERS") {
  def id = column[Int]("ID", O.PrimaryKey)
  def link = column[String]("LINK")
  def userid = column[Int]("USER_ID")
  def date = column[String]("DATE")
  def * = (id, link, userid, date)
}