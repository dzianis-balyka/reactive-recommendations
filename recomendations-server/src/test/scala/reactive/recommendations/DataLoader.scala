package reactive.recommendations

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import spray.http._
import spray.client.pipelining._

import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Failure, Success}


/**
 * Created by denik on 09.12.2014.
 */
object DataLoader {

  val log = LoggerFactory.getLogger(DataLoader.getClass)

  def main(args: Array[String]) = {

    implicit val system = ActorSystem()
    import system.dispatcher
    // execution context for futures

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val succeded = new AtomicLong()
    val failed = new AtomicLong()
    val rate = 100
    var startedAt = System.currentTimeMillis()

    implicit val timeout = Timeout( 20 seconds)


    while (true) {

      val uri = Uri("http://localhost:8080/item").withQuery(("id", "i" + System.nanoTime()), ("tag", "2"), ("tag", "3"), ("category", "sciense"))
      val response: Future[HttpResponse] = pipeline(Get(uri))
      response.andThen {
        case Success(x) => {
          val counter = succeded.incrementAndGet()
          if (counter % rate == 0) {
            log.info("Success %1$s with last speed %2$s".format(counter, (rate * 1000 / (System.currentTimeMillis() - startedAt))))
            startedAt = System.currentTimeMillis()
          }
        }
        case Failure(x) => {
          val counter = succeded.incrementAndGet()
          if (counter % rate == 0) {
            log.info("Failed ", x)
          }
        }
      }
    }


  }

}
