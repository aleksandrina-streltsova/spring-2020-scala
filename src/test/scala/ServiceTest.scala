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

  "ServiceRest add_user" should "add new user" in new mocks {
    service.users.length shouldBe 0
    service.add_user(1)
    service.users.length shouldBe 1
    service.add_user(2)
    service.users.length shouldBe 2
    service.add_user(3)
    service.users.length shouldBe 3
    service.add_user(1)
    service.users.length shouldBe 3
  }

  "ServiceRest get_users" should "add new user" in new mocks {
    service.add_user(1)
    service.add_user(2)
    service.add_user(3)
    service.add_user(1)
    service.users.contains(1) shouldBe true
    service.users.contains(2) shouldBe true
    service.users.contains(3) shouldBe true
    service.users.contains(4) shouldBe false
  }
}