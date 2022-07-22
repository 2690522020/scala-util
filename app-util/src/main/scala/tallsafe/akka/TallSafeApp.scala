package tallsafe.akka

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.Logger

import java.io.File
import java.nio.file.{FileAlreadyExistsException, Files, StandardOpenOption}
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * 应用安全的启动程序类
 */
trait TallSafeApp extends App {
  val process: ServerProcess = new ServerProcess(if(args==null) Nil else args)
  val actorSystem = ActorSystem("TallSafe-App")
  val config: Config = ConfigFactory.load()
  // Read settings
  val configuration: Configuration = readServerConfigSettings(process)
  implicit val ex: ExecutionContextExecutor = actorSystem.dispatcher
  // Create a PID file before we do any real work
  val pidFile = createPidFile(process, configuration)
  val coordinatedShutdown = CoordinatedShutdown(actorSystem)
  //应用程序关闭任务池
  val applicationLifecycle = new ApplicationLifecycle()
  var pidFileP: String = _

  def logger: Logger

  private def readServerConfigSettings(process: ServerProcess): Configuration = {
    val rootDirArg = process.args.headOption.map(new File(_))
    val rootDirConfig = rootDirArg.fold(Map.empty[String, String])(f => Map("play.server.dir" -> f.getAbsolutePath))
    Configuration.load(process.classLoader, process.properties, rootDirConfig, allowMissingApplicationConf = false)
  }
  coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseBeforeActorSystemTerminate,
    "remove-pid-file") { () =>
    // Must delete the PID file after stopping the server not before...
    // In case of unclean shutdown or failure, leave the PID file there!
    pidFile.foreach(_.delete())
    assert(pidFile.forall(!_.exists), "PID file should not exist!")
    Future.successful(Done)
  }

  private def createPidFile(process: ServerProcess, config: Configuration): Option[File] = {
    val pidFilePath = config
      .getOptional[String]("akka.server.pidfile.path")
      .getOrElse(throw ServerStartException("Pid file path not configured"))
    pidFileP = pidFilePath
    if (pidFilePath == "/dev/null") None
    else {
      val pidFile = new File(pidFilePath).getAbsoluteFile
      val pid = process.pid.getOrElse(throw new Exception("服务启动失败:获取进程号异常", null))
      val out =
        try Files.newOutputStream(pidFile.toPath, StandardOpenOption.CREATE_NEW)
        catch {
          case _: FileAlreadyExistsException =>
            throw new Exception(s"This application is already running (or delete ${pidFile.getPath} file).")
        }
      try out.write(pid.getBytes)
      finally out.close()
      Some(pidFile)
    }
  }

  coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseServiceStop,
    "application-lifecycle-stop-hook")(() => {
    applicationLifecycle.stop().map(_ => Done)
  })

}
