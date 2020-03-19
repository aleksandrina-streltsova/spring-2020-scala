import com.softwaremill.sttp.{Response, SttpBackend}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object RandomMock extends Randomizer {
  override def random(n: Int): Int = 1
}

class ServiceRestTest extends AnyFlatSpec with Matchers with MockFactory {
  trait mocks {
    implicit val ec = ExecutionContext.global
    implicit val sttpBackend = mock[SttpBackend[Future, Nothing]]
    implicit val r = RandomMock

    val service = new ServiceRest(sttpBackend)
  }

  "ServiceRest" should "return cat link" in new mocks {
    (sttpBackend.send[String] _).expects(*).returning(Future.successful(
      Response.ok("""{data:[{ images: [{ link : "foo" }, { link : "bar" }] }] }""")
    ))

    val result = Await.result(service.link(), Duration.Inf)

    result shouldBe """bar"""
  }
}