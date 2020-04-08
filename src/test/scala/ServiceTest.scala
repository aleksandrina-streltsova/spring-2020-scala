import com.softwaremill.sttp.{Response, SttpBackend}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

object RandomMock extends Randomizer {
  override def random(n: Int): Int = 1
}

object DateGetterMock extends DateGetter {
  override def date(): String = "1"
}

class ServiceRestTest extends AnyFlatSpec with Matchers with MockFactory {

  trait mocks {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val sttpBackend: SttpBackend[Future, Nothing] = mock[SttpBackend[Future, Nothing]]
    implicit val r: RandomMock.type = RandomMock
    implicit val t: DateGetterMock.type = DateGetterMock

    implicit val db: Database = Database.forConfig("h2mem1")

    val service = new ServiceRest(sttpBackend)(ec, r, t, db)

    Await.result(service.init(), Duration.Inf)
  }

  "ServiceRest link" should "return cat link" in new mocks {
    (sttpBackend.send[CatResponse] _).expects(*).returning(Future.successful(
      Response.ok(CatResponse(List(Data(List(InnerData("foo"), InnerData("bar"), InnerData("baz"))))))
    ))

    val result: String = Await.result(service.link(0), Duration.Inf)

    result shouldBe """bar"""
  }

  "ServiceRest addUser, getUsers" should "correctly process users" in new mocks {
    service.addUser(1)
    Await.result(service.getUsers().map(x => x.split(", ").sorted), Duration.Inf) shouldBe Array("1")
    service.addUser(2)
    Await.result(service.getUsers().map(x => x.split(", ").sorted), Duration.Inf) shouldBe Array("1", "2")
    service.addUser(3)
    Await.result(service.getUsers().map(x => x.split(", ").sorted), Duration.Inf) shouldBe Array("1", "2", "3")
    service.addUser(1)
    Await.result(service.getUsers().map(x => x.split(", ").sorted), Duration.Inf) shouldBe Array("1", "2", "3")
  }

  "ServiceRest getMessages, sendMessage" should "correctly process messages" in new mocks {
    service.sendMessage(1, "Hello")
    service.sendMessage(1, "Hello")
    service.sendMessage(2, "Hello")
    service.sendMessage(2, "World!")
    service.sendMessage(3, "World")
    service.sendMessage(4, "")
    Await.result(service.getMessages(1), Duration.Inf) shouldBe "Hello, Hello"
    Await.result(service.getMessages(2), Duration.Inf) shouldBe "Hello, World!"
    Await.result(service.getMessages(3), Duration.Inf) shouldBe "World"
    Await.result(service.getMessages(4), Duration.Inf) shouldBe ""
  }
}