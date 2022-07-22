package tallsafe.akka

final case class ServerStartException(message: String, cause: Option[Throwable] = None)
  extends Exception(message, cause.orNull)
