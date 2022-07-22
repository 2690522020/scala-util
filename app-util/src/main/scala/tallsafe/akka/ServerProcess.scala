package tallsafe.akka

import java.lang.management.ManagementFactory
import java.util.Properties

class ServerProcess(val args: Seq[String] = Nil) {

  def classLoader: ClassLoader = Thread.currentThread.getContextClassLoader

  // These properties are used in Prod mode and for the Server in Dev Mode (not the Application).
  def properties: Properties = System.getProperties

  def pid: Option[String] = {
    ManagementFactory.getRuntimeMXBean.getName.split('@').headOption
  }

  def addShutdownHook(hook: => Unit): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = hook
    })
  }

  def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
    System.err.println(message)
    cause.foreach(_.printStackTrace())
    // this System.exit is using a return code and could also cause CoordinatedShutdown to run.
    System.exit(returnCode)
    // Code never reached, but throw an exception to give a type of Nothing
    throw new Exception("SystemProcess.exit called")
  }
}