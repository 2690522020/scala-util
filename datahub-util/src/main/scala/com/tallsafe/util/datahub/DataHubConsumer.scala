package com.tallsafe.util.datahub

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import com.aliyun.datahub.client.auth.AliyunAccount
import com.aliyun.datahub.client.common.DatahubConfig
import com.aliyun.datahub.client.exception.DatahubClientException
import com.aliyun.datahub.client.model._
import com.aliyun.datahub.client.{DatahubClient, DatahubClientBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

//noinspection ScalaUnusedSymbol
/**
 * dataHub 消费扩展 继承这个trait 就可以
 */
trait DataHubConsumer extends Actor {
  lazy val dataHubClient: DatahubClient = DatahubClientBuilder.newBuilder()
    .setDatahubConfig(new DatahubConfig(endpoint, new AliyunAccount(accessId, accessKey), false)).build()
  lazy val subID: String = if (subscriptionID == null || subscriptionID.isEmpty) getSubID else subscriptionID
  lazy val listShardResult: Seq[String] = dataHubClient.listShard(projectName, topicName)
    .getShards.iterator().asScala.toSeq.map(_.getShardId)
  lazy val subSession: OpenSubscriptionSessionResult = dataHubClient
    .openSubscriptionSession(projectName, topicName, subID, listShardResult.asJava)
  lazy val schema: RecordSchema = dataHubClient.getTopic(projectName, topicName).getRecordSchema
  private val logger = LoggerFactory.getLogger(this.getClass)
  var subConsumers: Seq[ActorRef] = Nil

  /**
   * datahub 相关配置
   */
  def accessId: String

  def accessKey: String

  def endpoint: String

  def projectName: String

  def topicName: String

  def topicDesc: String

  /**
   * 订阅id 不实现会随机生成一个
   *
   * @return
   */
  def subscriptionID: String

  /**
   * 订阅  Comment
   *
   * @return
   */
  def subscriptionComment: String

  /**
   * 数据处理方法
   *
   * @param data 数据(TupleRecordData)
   * @return
   */
  def handleData(data: TupleRecordData): Any

  /**
   * 服务关闭时 通知关闭这个actor
   * appLifecycle.addStopHook {
   * () =>
   * subConsumers.foreach {
   * s =>
   * s ! "stop"
   * }
   * Future.successful(None)
   * }
   */
  override def preStart(): Unit = {
    logger.info(s"${self.path.address}/${self.path.name}:消费程序启动")
    context.system.scheduler.scheduleOnce(5.seconds) {
      subConsumers = listShardResult.map {
        value =>
          context.actorOf(Consumer.props(value), s"client-$value")
      }
    }(context.dispatcher)
    super.preStart()
  }

  override def postStop(): Unit = {
    logger.info(s"${self.path.address}/${self.path.name}:消费程序停止")
    subConsumers.foreach {
      sub =>
        sub ! "stop"
    }
    super.postStop()
  }

  private def getSubID: String = {
    dataHubClient.createSubscription(projectName, topicName, subscriptionComment).getSubId
  }

  class Consumer(shardId: String) extends Actor {
    var isStop = false
    var cursor: String = _

    override def preStart(): Unit = {
      logger.info(s"$self:start --> $shardId")
      start
      super.preStart()
    }

    def start: Cancellable = {
      context.system.scheduler.scheduleOnce(3.seconds) {
        try {
          var subOffset = subSession.getOffsets.get(shardId)
          cursor = if (subOffset.getSequence > 0) {
            // 获取下一条记录的Cursor
            val nextSequence = subOffset.getSequence + 1
            // 备注：如果按照SEQUENCE getCursor报SeekOutOfRange错误，需要回退到按照SYSTEM_TIME或者OLDEST/LATEST进行getCursor
            dataHubClient.getCursor(projectName, topicName, shardId, CursorType.SEQUENCE, nextSequence).getCursor
          } else { // 获取最旧数据的Cursor
            if (cursor == null || cursor.isEmpty) {
              dataHubClient.getCursor(projectName, topicName, shardId, CursorType.LATEST).getCursor
            } else {
              cursor
            }
          }
          var recordCount: Long = 0
          while (!isStop) {
            try {
              val result = dataHubClient.getRecords(projectName, topicName, shardId, schema, cursor, 100)
              // 如果有数据则处理，无数据需sleep后重新读取
              if (result.getRecordCount > 0) {
                for (entry <- result.getRecords.asScala) {
                  val data = entry.getRecordData.asInstanceOf[TupleRecordData]
                  handleData(data)
                  subOffset.setSequence(entry.getSequence)
                  subOffset.setTimestamp(entry.getSystemTime)
                  if (recordCount % 100 == 0) {
                    recordCount = 0L
                    val offsetMap = new java.util.HashMap[String, SubscriptionOffset]()
                    offsetMap.put(shardId, subOffset)
                    dataHubClient.commitSubscriptionOffset(projectName, topicName, subID, offsetMap)
                    logger.debug(s"shard($shardId): commit offset successful")
                  }
                }
                // 拿到下一个游标
                cursor = result.getNextCursor
              } else {
                Thread.sleep(10)
              }
            } catch {
              case e: DatahubClientException =>
                logger.error(s"DataHub Consumer:${e.getMessage}", e)
                subOffset = subSession.getOffsets.get(shardId)
                val nextSequence = subOffset.getSequence + 1
                cursor = dataHubClient.getCursor(projectName, topicName, shardId, CursorType.SEQUENCE, nextSequence).getCursor
              case e: Exception =>
                // 非法游标或游标已过期，建议重新定位后开始消费
                logger.error(s"DataHub Consumer:${e.getMessage}", e)
            }
          }
        } catch {
          case ex: Exception =>
            logger.error(s"DataHub Consumer Init:${ex.getMessage}", ex)
        }
      }(this.context.dispatcher)
    }

    override def postStop(): Unit = {
      logger.info(s"${self.path.address}/${self.path.name}:stop")
      super.postStop()
    }

    override def receive: Receive = {
      case "stop" =>
        logger.info(s"$self:stop --> $shardId")
        isStop = true
      case msg: String =>
        logger.info(s"${self.path.address}/${self.path.name}:$msg")
    }

  }

  object Consumer {
    def props(shardId: String): Props = Props(new Consumer(shardId))
  }
}
