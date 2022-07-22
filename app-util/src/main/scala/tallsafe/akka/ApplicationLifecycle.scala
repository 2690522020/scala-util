package tallsafe.akka

import akka.Done
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success, Try}

class ApplicationLifecycle()(implicit ex: ExecutionContextExecutor) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val hooks = new ConcurrentLinkedDeque[() => Future[_]]()
  private val stopPromise: Promise[Done] = Promise()
  private val started = new AtomicBoolean(false)

  def addStopHook(hook: () => Future[_]): Unit = hooks.push(hook)

  /**
   * Call to shutdown the application.
   *
   * @return A future that will be redeemed once all hooks have executed.
   */
  def stop(): Future[_] = {
    logger.debug(s"Executing tallsafe.akka.ApplicationLifecycle.stop() with ${hooks.size()} stop hook(s) registered")
    // run the code only once and memoize the result of the invocation in a Promise.future so invoking
    // the method many times causes a single run producing the same result in all cases.
    if (started.compareAndSet(false, true)) {
      // Do we care if one hook executes on another hooks redeeming thread? Hopefully not.

      @tailrec def clearHooks(previous: Future[Any] = Future.successful[Any](())): Future[Any] = {
        val hook = hooks.poll()
        if (hook != null) clearHooks(previous.flatMap { _ =>
          val hookFuture = Try(hook()) match {
            case Success(f) => f
            case Failure(e) => Future.failed(e)
          }
          hookFuture.recover {
            case e => logger.error("Error executing stop hook", e)
          }
        })
        else previous
      }

      stopPromise.completeWith(clearHooks().map(_ => Done))
    }
    stopPromise.future
  }
}
