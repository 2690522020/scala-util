package io.github.zjw.akka

import org.slf4j.LoggerFactory

import java.io.{ByteArrayOutputStream, Closeable, IOException, InputStream}
import java.net.URL
import java.nio.file.{Files, Path}
import scala.io.Codec

object PlayIO {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Read the file into a byte array.
   */
  def readFile(file: Path): Array[Byte] = {
    readStream(Files.newInputStream(file))
  }

  /**
   * Read the URL as a String.
   */
  def readUrlAsString(url: URL)(implicit codec: Codec): String = {
    readStreamAsString(url.openStream())
  }

  /**
   * Read the given stream into a String.
   *
   * Closes the stream.
   */
  def readStreamAsString(stream: InputStream)(implicit codec: Codec): String = {
    new String(readStream(stream), codec.name)
  }

  /**
   * Read the given stream into a byte array.
   *
   * Closes the stream.
   */
  private def readStream(stream: InputStream): Array[Byte] = {
    try {
      val buffer = new Array[Byte](8192)
      var len    = stream.read(buffer)
      val out    = new ByteArrayOutputStream() // Doesn't need closing
      while (len != -1) {
        out.write(buffer, 0, len)
        len = stream.read(buffer)
      }
      out.toByteArray
    } finally closeQuietly(stream)
  }

  /**
   * Close the given closeable quietly.
   *
   * Logs any IOExceptions encountered.
   */
  def closeQuietly(closeable: Closeable): Unit = {
    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case e: IOException => logger.warn("Error closing stream", e)
    }
  }

  /**
   * Read the file as a String.
   */
  def readFileAsString(file: Path)(implicit codec: Codec): String = {
    readStreamAsString(Files.newInputStream(file))
  }
}
