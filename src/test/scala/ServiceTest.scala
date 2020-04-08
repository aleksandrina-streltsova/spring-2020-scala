import com.softwaremill.sttp.{Response, SttpBackend}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

object RandomMock extends Randomizer {
  override def random(n: Int): Int = 1
}

class ServiceRestTest extends AnyFlatSpec with Matchers with MockFactory {

  trait mocks {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val sttpBackend: SttpBackend[Future, Nothing] = mock[SttpBackend[Future, Nothing]]
    implicit val r: RandomMock.type = RandomMock

    val service = new ServiceRest(sttpBackend)
  }

  "ServiceRest link" should "return cat link" in new mocks {
    (sttpBackend.send[CatResponse] _).expects(*).returning(Future.successful(
      Response.ok(CatResponse(List(Data(List(InnerData("foo"), InnerData("bar"), InnerData("baz"))))))
    ))

    val result: String = Await.result(service.link(), Duration.Inf)

    result shouldBe """bar"""
  }

  "ServiceRest addUser" should "add new user" in new mocks {
    service.users.length shouldBe 0
    service.addUser(1)
    service.users.length shouldBe 1
    service.addUser(2)
    service.users.length shouldBe 2
    service.addUser(3)
    service.users.length shouldBe 3
    service.addUser(1)
    service.users.length shouldBe 3

    service.users.contains(1) shouldBe true
    service.users.contains(2) shouldBe true
    service.users.contains(3) shouldBe true
    service.users.contains(4) shouldBe false
  }

  "ServiceRest getUsers" should "return all users" in new mocks {
    service.getUsers() shouldBe ""
    service.users += 1
    service.getUsers() shouldBe "1"
    service.users ++= List(2, 3, 4)
    service.getUsers() shouldBe "1, 2, 3, 4"
    service.users.clear()
    service.getUsers() shouldBe ""
  }

  "ServiceRest getMessages" should "return messages from user" in new mocks {
    service.messages += 1 -> "Hello"
    service.messages += 1 -> "Hello"
    service.messages += 2 -> "Hello"
    service.messages += 2 -> "World!"
    service.messages += 3 -> "World"
    service.messages += 4 -> ""
    service.getMessages(1) shouldBe "Hello, Hello"
    service.getMessages(2) shouldBe "Hello, World!"
    service.getMessages(3) shouldBe "World"
    service.getMessages(4) shouldBe ""
  }

  "ServiceRest sendMessage" should "send message to user" in new mocks {
    service.users ++= List(1, 2, 3)
    service.sendMessage(2, "msg2")
    service.sendMessage(1, "msg1")
    service.sendMessage(2, "msg22")
    service.getMessages(2) shouldBe "msg2, msg22"
    service.getMessages(3) shouldBe ""
    service.getMessages(1) shouldBe "msg1"
  }
}