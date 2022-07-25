package io.github.zjw.util.minio

import com.tallsafe.model.common.ResultMsg
import io.minio.{MakeBucketArgs, MinioClient, PutObjectArgs}

import java.io.{ByteArrayInputStream, File, FileInputStream, InputStream}

/**
 * minio扩展
 *
 * @param endpoint       链接地址
 * @param accessKey      key
 * @param secretKey      秘钥
 * @param publicEndPoint 公开地址
 */
class MinIoUtil(endpoint: String,
                accessKey: String,
                secretKey: String,
                publicEndPoint: Option[String] = None
               ) {

  private final val contents: Map[String, String] = Map(
    "txt" -> "text/plain",
    "jpg" -> "image/jpeg",
    "jpeg" -> "image/jpeg",
    "png" -> "image/png",
    "bmp" -> "image/bmp",
    "pdf" -> "application/pdf",
    "xlsx" -> "xlsx",
    "xlx" -> "xlx",
    "xls" -> "application/vnd.ms-excel",
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  )

  lazy val defaultMinIoClient: MinioClient = MinioClient.builder().endpoint(endpoint)
    .credentials(accessKey, secretKey).build()

  def createBucket(name: String): Option[String] = {
    try {
      defaultMinIoClient.makeBucket(MakeBucketArgs.builder()
        .bucket(name)
        .build())
      None
    } catch {
      case ex: Exception =>
        Some(ex.getMessage)
    }
  }

  def createFolder(path: String, bucket: String = "tpf"): Option[String] = {
    try {
      defaultMinIoClient.putObject(PutObjectArgs.builder
        .bucket(bucket)
        .`object`(path)
        .stream(new ByteArrayInputStream(new Array[Byte](0)), 0, -1).build)
      None
    } catch {
      case ex: Exception =>
        Some(ex.getMessage)
    }
  }

  def createFile(bucket: String = "tpf",
                 path: String,
                 file: File,
                 fileType: String,
                 fileName: Option[String] = None): ResultMsg[String] = {
    createFile(bucket, path, new FileInputStream(file), fileType, fileName)
  }

  def createFile(bucket: String = "tpf",
                 path: String,
                 input: InputStream,
                 fileType: String,
                 fileName: Option[String] = None): ResultMsg[String] = {
    try {
      val filePath = if (fileName.nonEmpty) {
        s"$path/${fileName.get}.$fileType"
      } else if (path.contains(".")) {
        path
      } else {
        s"$path/${getGuid()}.$fileType"
      }
      defaultMinIoClient.putObject(PutObjectArgs.builder
        .bucket(bucket)
        .`object`(filePath)
        .stream(input, -1, 20971520)
        .contentType(contents.getOrElse(fileType, "application/octet-stream")).build)
      input.close()
      ResultMsg(Data = Some(s"${if (publicEndPoint.nonEmpty) publicEndPoint.get else ""}/$bucket/$filePath"))
    } catch {
      case ex: Exception =>
        ResultMsg(Exception = Some(ex.getMessage))
    }
  }

  def createFileByByte(bucket: String = "tpf", path: String, file: Array[Byte], fileType: String,
                       fileName: Option[String] = None): ResultMsg[String] = {
    createFile(bucket, path, new ByteArrayInputStream(file), fileType, fileName)
  }
}
