package com.tallsafe.util.common

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

class LocalDateTimeUtil()

object LocalDateTimeUtil {
  /**
   * 时间隐式转换类
   *
   * @param time 时间
   */
  implicit class LocalDateTimeEnhancements(val time: LocalDateTime) {
    /**
     * 时间格式化
     *
     * @param patten 格式化格式
     * @return
     */
    def toString(patten: String): String = {
      val localDateTimeFormatter = DateTimeFormatter.ofPattern(patten)
      time.format(localDateTimeFormatter)
    }

    /**
     * 时间转毫秒
     *
     * @return
     */
    def getMillis: Long = {
      time.toInstant(ZoneOffset.ofHours(8)).toEpochMilli
    }
  }

  /**
   * 时间隐式转换类
   *
   * @param num 时间戳 毫秒
   */
  implicit class LocalDateTimeLongEnhancements(val num: Long) {
    /**
     * 毫秒转时间
     *
     * @return
     */
    def toLocalDateTime: LocalDateTime = {
      Instant.ofEpochMilli(num.longValue).atZone(ZoneOffset.ofHours(8)).toLocalDateTime
    }
  }

  /**
   * 获取当日时间
   *
   * @return [[LocalDateTime]]
   */
  def getDateTimeNowDay: LocalDateTime = {
    val now = LocalDateTime.now()
    LocalDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, 0, 0, 0)
  }

  /**
   * 获取当日当时时间
   *
   * @return [[LocalDateTime]]
   */
  def getDateTimeNowHour: LocalDateTime = {
    val now = LocalDateTime.now()
    LocalDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, now.getHour, 0, 0)
  }
}
