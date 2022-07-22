package com.tallsafe.util.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.params.SetParams
import redis.clients.jedis.{Connection, HostAndPort, JedisCluster, JedisPooled}

import java.lang
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

/**
 * redis 扩展
 * @param source 模式
 * @param host 地址
 * @param port 端口
 */
class JRedisClient(source: String = "standalone",
                        host: String,
                        port: Int,
                        database: Option[Int] = None
                       )(implicit ec: ExecutionContext) {

  val poolConfig = new GenericObjectPoolConfig[Connection]()
  private val logger = LoggerFactory.getLogger(classOf[JRedisClient])
  poolConfig.setMaxTotal(200)
  poolConfig.setMaxIdle(50)
  poolConfig.setMaxWait(java.time.Duration.ofSeconds(10))
  var jc: JedisCluster = _
  var redis: JedisPooled = _
  var isInit: Boolean = false

  def get[T: ClassTag](key: String): Future[Option[T]] = Future {
    getSync[T](key)
  }.recover {
    case ex: Exception =>
      logger.error("redis取值失败:", ex)
      isInit = false
      None
  }

  def getSync[T: ClassTag](key: String): Option[T] = {
    def getValue: Option[T] = {
      source match {
        case "standalone" =>
          if (redis.exists(key)) {
            Some(redis.get(key).asInstanceOf[T])
          } else {
            None
          }
        case "cluster" =>
          if (jc.exists(key)) {
            Some(jc.get(key).asInstanceOf[T])
          } else {
            None
          }
        case _ =>
          None
      }
    }

    try {
      if (isInit) {
        getValue
      } else {
        init()
        if (isInit) {
          getValue
        } else {
          None
        }
      }
    } catch {
      case ex: Exception =>
        logger.error("redis取值失败:", ex)
        isInit = false
        None
    }
  }

  def set[T: ClassTag](key: String, value: T, expiration: Duration): Future[Option[String]] = Future {
    def setValue(): Option[String] = {
      source match {
        case "standalone" =>
          Some(redis.set(key, value.toString, new SetParams().ex(expiration.toSeconds)))
        case "cluster" =>
          Some(jc.set(key, value.toString, new SetParams().ex(expiration.toSeconds)))
        case _ =>
          None
      }
    }

    initClient()
    setValue()
  }.recover {
    case ex: Exception =>
      logger.error("redis保存值失败:", ex)
      isInit = false
      None
  }

  def del(key: String): Future[Option[lang.Long]] = Future {
    def setValue(): Option[lang.Long] = {
      source match {
        case "standalone" =>
          Some(redis.del(key))
        case "cluster" =>
          Some(jc.del(key))
        case _ =>
          None
      }
    }

    initClient()
    setValue()
  }.recover {
    case ex: Exception =>
      logger.error("redis保存值失败:", ex)
      isInit = false
      None
  }

  def expire(key: String, expiration: Duration): Future[Option[lang.Long]] = Future {
    def expireValue: Option[lang.Long] = {
      source match {
        case "standalone" =>
          Some(redis.expire(key, expiration.toSeconds))
        case "cluster" =>
          Some(jc.expire(key, expiration.toSeconds))
        case _ =>
          None
      }
    }

    initClient()
    expireValue
  }.recover {
    case ex: Exception =>
      logger.error("redis设置过期时间失败:", ex)
      isInit = false
      None
  }

  def mapGet[T: ClassTag](key: String, filed: String): Future[Option[T]] = Future {
    //noinspection DuplicatedCode
    def getValue: Option[T] = {
      source match {
        case "standalone" =>
          if (redis.hexists(key, filed)) {
            Some(redis.hget(key, filed).asInstanceOf[T])
          } else {
            None
          }
        case "cluster" =>
          if (jc.hexists(key, filed)) {
            Some(jc.hget(key, filed).asInstanceOf[T])
          } else {
            None
          }
        case _ =>
          None
      }
    }

    initClient()
    getValue
  }.recover {
    case ex: Exception =>
      logger.error("redis取值失败:", ex)
      isInit = false
      None
  }

  def mapAdd[T: ClassTag](key: String, filed: String, value: T): Future[Option[lang.Long]] = Future {
    def setValue(): Option[lang.Long] = {
      source match {
        case "standalone" =>
          Some(redis.hset(key, filed, value.toString))
        case "cluster" =>
          Some(jc.hset(key, filed, value.toString))
        case _ =>
          None
      }
    }

    initClient()
    setValue()
  }.recover {
    case ex: Exception =>
      logger.error("redis保存值失败:", ex)
      isInit = false
      None
  }

  private def initClient(): Unit = {
    if (!isInit) {
      init()
    }
  }

  def init(): Unit = {
    try {
      if (redis != null) {
        redis.close()
      }
      if (jc != null) {
        jc.close()
      }
      source match {
        case "standalone" =>
          redis = new JedisPooled(host, port)
          isInit = true
        case "cluster" =>
          jc = new JedisCluster(new HostAndPort(host, port), poolConfig)
          isInit = true
        case _ =>
      }
    } catch {
      case ex: Exception =>
        logger.error("redis初始化失败:", ex)
    }
  }

  def mapRemove(key: String, filed: String): Future[Option[lang.Long]] = Future {
    def setValue(): Option[lang.Long] = {
      source match {
        case "standalone" =>
          Some(redis.hdel(key, filed))
        case "cluster" =>
          Some(jc.hdel(key, filed))
        case _ =>
          None
      }
    }

    initClient()
    setValue()
  }.recover {
    case ex: Exception =>
      logger.error("redis保存值失败:", ex)
      isInit = false
      None
  }

  def mapList(key: String): Future[collection.Map[String, String]] = Future {
    def getValue: collection.Map[String, String] = {
      source match {
        case "standalone" =>
          redis.hgetAll(key).asScala.toMap
        case "cluster" =>
          jc.hgetAll(key).asScala.toMap
        case _ =>
          Map[String, String]()
      }
    }

    initClient()
    getValue
  }.recover {
    case ex: Exception =>
      logger.error("redis取值失败:", ex)
      isInit = false
      Map[String, String]()
  }
}
