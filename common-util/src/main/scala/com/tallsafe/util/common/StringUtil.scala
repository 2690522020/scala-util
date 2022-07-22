package com.tallsafe.util.common

import java.security.MessageDigest
import java.sql.{Date, Timestamp}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{LocalDateTime, ZoneOffset}
import java.util.regex.Pattern
import java.util.{Base64, UUID}
import scala.collection.mutable

class StringUtil {

}

/**
 * Created by zjw on 2018/7/19.
 */
object StringUtil {
  /**
   * 字符串增强类，隐式扩展为String类型增加数据类型转换扩展方法，返回Option类型对象。
   *
   * 适用于字符串，到以下类型的转换：
   *
   * Int，Long，Boolean，BigDecimal，Joda DateTime，java.sql.Timestamp，java.sql.Date
   *
   * @param s 扩展的字符串实例对象
   */
  implicit class StringEnhancements(val s: String) {

    import scala.util.control.Exception._

    def toDoubleOpt: Option[Double] = catching(classOf[NumberFormatException]) opt s.toDouble

    def toFloatOpt: Option[Float] = catching(classOf[NumberFormatException]) opt s.toFloat

    def toIntOptZero: Option[Int] = {
      val i = toIntOpt
      if (i.isDefined) {
        i
      } else {
        Some(0)
      }
    }

    def toIntOpt: Option[Int] = catching(classOf[NumberFormatException]) opt s.toInt

    def toBooleanOpt: Option[Boolean] = catching(classOf[IllegalArgumentException]) opt s.toBoolean

    def toBigDecimalOpt: Option[BigDecimal] = catching(classOf[NumberFormatException]) opt new BigDecimal(new java.math.BigDecimal(s))

    def toTimestampOpt: Option[Timestamp] = {
      val l = toLongOpt
      if (l.isDefined && l.get > 0) {
        Some(new java.sql.Timestamp(l.get))
      } else {
        None
      }
    }

    def toSqlDateOpt: Option[Date] = {
      val l = toLongOpt
      if (l.isDefined) {
        Some(new java.sql.Date(l.get))
      } else {
        None
      }
    }

    def toLongOpt: Option[Long] = catching(classOf[NumberFormatException]) opt s.toLong

    def toNone: Option[String] = {
      if (s.nonEmpty)
        Some(s)
      else
        None
    }

    def toMd5: String = {
      val md5 = MessageDigest.getInstance("MD5")
      val bytes = md5.digest(s.getBytes())
      val stringBuffer = new StringBuffer()
      for (b <- bytes) {
        val bt = b & 0xff
        if (bt < 16) {
          stringBuffer.append(0)
        }
        stringBuffer.append(Integer.toHexString(bt))
      }
      stringBuffer.toString
    }

    def toSha1: String = {
      //指定sha1算法
      val digest = MessageDigest.getInstance("SHA-1")
      digest.update(s.getBytes())
      //获取字节数组
      val messageDigest = digest.digest
      // Create Hex String
      val hexString = new mutable.StringBuilder()
      // 字节数组转换为 十六进制 数
      var i = 0
      while ( {
        i < messageDigest.length
      }) {
        val shaHex = Integer.toHexString(messageDigest(i) & 0xff)
        if (shaHex.length < 2) hexString.append(0)
        hexString.append(shaHex) {
          i += 1
          i - 1
        }
      }
      // 转换为全大写
      hexString.toString.toUpperCase
    }

    def toBase64: String = {
      Base64.getEncoder.encodeToString(s.getBytes()).replace("\r\n", "").replace("\n", "")
    }

    def fromBase64: Array[Byte] = {
      try {
        Base64.getDecoder.decode(s)
      } catch {
        case ex: Exception =>
          println(ex)
          Array[Byte]()
      }
    }

    def stringFromBase64: String = {
      (catching(classOf[Exception]) opt new String(Base64.getDecoder.decode(s))).getOrElse("")
    }


    def isContainChinese: Boolean = {
      val regEx = "[\\u4E00-\\u9FA5]+"
      val p = Pattern.compile(regEx)
      val m = p.matcher(s)
      if (m.find) true
      else false
    }

    def isContainNum: Boolean = {
      val regEx = ".*\\d+.*"
      val p = Pattern.compile(regEx)
      val m = p.matcher(s)
      if (m.find) true
      else false
    }

    def isContainLetter: Boolean = {
      val regEx = ".*[a-z]+.*"
      val p = Pattern.compile(regEx)
      val m = p.matcher(s)
      if (m.find) true
      else false
    }

    def isContainCapitalizeLetter: Boolean = {
      val regEx = ".*[A-Z]+.*"
      val p = Pattern.compile(regEx)
      val m = p.matcher(s)
      if (m.find) true
      else false
    }

    def toChineseWeek: String = {
      s match {
        case "1" =>
          "一"
        case "2" =>
          "二"
        case "3" =>
          "三"
        case "4" =>
          "四"
        case "5" =>
          "五"
        case "6" =>
          "六"
        case "7" =>
          "日"
        case _ =>
          ""
      }
    }

    def toEncryption: String = {
      if (s.length == 18) {
        s.replaceAll("(\\S{6})\\S{8}(\\S{4})", "$1*****$2")
      } else {
        "未知"
      }
    }

    def toLocalDateTime: Option[LocalDateTime] = {
      try {
        Some(LocalDateTime.parse(s))
      } catch {
        case _: Any =>
          None
      }
    }

    def isCardId: Boolean = {
      val ValCodeArr = Seq[String]("1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2")
      val Wi = Seq[String]("7", "9", "10", "5", "8", "4", "2", "1", "6", "3", "7", "9", "10", "5", "8", "4", "2")
      var Ai = ""
      // ================ 号码的长度 15位或18位 ================
      if (s.length != 15 && s.length != 18) {
        false
      } else {
        // ================ 数字 除最后一位都为数字 ================
        if (s.length == 18) {
          Ai = s.substring(0, 17)
        } else {
          Ai = s.substring(0, 6) + "19" + s.substring(6, 15)
        }
        if (isNum(Ai)) {
          false
        } else {
          val strYear = Ai.substring(6, 10) // 年份
          val strMonth = Ai.substring(10, 12) // 月份
          val strDay = Ai.substring(12, 14) //天
          val time = s"$strYear-$strMonth-$strDay 00:00:00".toLocalDateTime("yyyy-MM-dd HH:mm:ss")
          if (time.isEmpty) {
            false
          } else {
            val t = time.get.toInstant(ZoneOffset.ofHours(8)).toEpochMilli
            if (t < -2209017943000L && t > LocalDateTime.now().toInstant(ZoneOffset.ofHours(8)).toEpochMilli) {
              false
            } else {
              var totalMulAiWi = 0
              for (i <- 0 to 16) {
                totalMulAiWi = totalMulAiWi + String.valueOf(Ai.charAt(i)).toInt * Wi(i).toInt
              }
              val modValue = totalMulAiWi % 11
              val strVerifyCode = ValCodeArr(modValue)
              Ai = Ai + strVerifyCode
              if (s.length() == 18) {
                Ai == s
              } else {
                true
              }
            }
          }
        }
      }
    }

    def toLocalDateTime(patten: String): Option[LocalDateTime] = {
      val localDateTimeFormatter = if (patten.contains(":")) {
        DateTimeFormatter.ofPattern(patten)
      } else {
        if (s.length > 14) new DateTimeFormatterBuilder()
          // 解析date+time
          .appendPattern(patten)
          // 解析毫秒数
          .appendValue(ChronoField.MILLI_OF_SECOND, 3)
          .toFormatter() else
          DateTimeFormatter.ofPattern(patten)
      }
      try {
        Some(LocalDateTime.parse(s, localDateTimeFormatter))
      } catch {
        case ex: Exception =>
          println(ex)
          None
        case _: Any =>
          None
      }
    }

    def isNum(str: String): Boolean = {
      val regEx = "^[-\\\\+]?[\\\\d]*$"
      Pattern.matches(regEx, str)
    }

    def wildcardToRegex(rules: Seq[JWildcardRule], strict: Boolean): Option[Pattern] = {
      if (s.nonEmpty) {
        val regex = new mutable.StringBuilder()
        val listOfOccurrences = (if (rules.nonEmpty) {
          rules
        } else {
          Seq(new JWildcardRule("?", "."), new JWildcardRule("*", ".*"))
        }).map {
          jWildcardRuleWithIndex =>
            new JWildcardRuleWithIndex(jWildcardRuleWithIndex, s.indexOf(jWildcardRuleWithIndex.Source))
        }.filter(_.index > -1).sortBy(_.index)
        var cursor = 0
        for (jWildcardRuleWithIndex <- listOfOccurrences) {
          val index = jWildcardRuleWithIndex.index
          if (index != 0) regex.append(Pattern.quote(s.substring(cursor, index)))
          regex.append(jWildcardRuleWithIndex.rule.Target)
          cursor = index + jWildcardRuleWithIndex.rule.Source.length
        }
        if (cursor <= s.length - 1) regex.append(Pattern.quote(s.substring(cursor, s.length)))
        val r = regex.toString
        Some(Pattern.compile(if (strict) "^" + r + "$" else r))
      } else {
        None
      }
    }

    def checkFileName(d: String): String = {
      val matcher = Pattern.compile("[\\s\\\\/:*?\"<>|]").matcher(s)
      matcher.replaceAll(d)
    }

    private class JWildcardRule(val Source: String, val Target: String)

    private class JWildcardRuleWithIndex(val rule: JWildcardRule, val index: Int)

  }

  implicit class OptionStringEnhancements(val s: Option[String]) {

    def toRealNone: Option[String] = {
      s match {
        case Some(str) =>
          if (str.nonEmpty) {
            s
          } else {
            None
          }
        case None =>
          None
      }
    }

    def toOptDouble: Option[Double] = {
      s match {
        case Some(str) =>
          if (str.nonEmpty) {
            str.toDoubleOpt
          } else {
            None
          }
        case None =>
          None
      }
    }

    def toOptInt: Option[Int] = {
      s match {
        case Some(str) =>
          if (str.nonEmpty)
            str.toIntOpt
          else {
            None
          }
        case None =>
          None
      }
    }

    def toOptBigDecimal: Option[BigDecimal] = {
      s match {
        case Some(str) =>
          if (str.nonEmpty) {
            str.toDoubleOpt match {
              case Some(dbl) => Option(BigDecimal(dbl))
              case None => None
            }
          } else {
            None
          }
        case None =>
          None
      }
    }

    def toEncryption: Option[String] = {
      s match {
        case Some(str) =>
          Some(str.toEncryption)
        case None =>
          None
      }
    }

    def toSeq: Seq[String] = {
      s match {
        case Some(value) => value.split(",")
        case None => Nil
      }
    }
  }

  def getGuid(isUpper: Boolean = false, isRep: Boolean = false): String = {
    val uuid = UUID.randomUUID().toString
    val d1 = if (isUpper) {
      uuid.toUpperCase()
    } else {
      uuid
    }
    if (isRep) {
      d1.replace("-", "")
    } else {
      d1
    }
  }
}
