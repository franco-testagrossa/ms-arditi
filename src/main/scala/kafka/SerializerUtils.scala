package kafka

import java.time.{ LocalDateTime }
import java.time.format.DateTimeFormatter

import io.circe.Json

object SerializerUtils {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  implicit def optionStringToDate(date: Option[String]): Option[LocalDateTime] = date.map(stringToDate)
  implicit def stringToDate(date: String): LocalDateTime = LocalDateTime.parse(date, formatter)

  implicit def fromOptionDate(a: Option[LocalDateTime]): Json = fromDate(a.getOrElse(LocalDateTime.now()))

  implicit def fromDate(a: LocalDateTime) = Json.fromString(formatter.format(a))

  implicit def fromOptionString(a: Option[String]) = fromString(a.getOrElse(""))

  implicit def fromString(a: String) = Json.fromString(a)

  implicit def fromOptionBigInt(a: Option[BigInt]) = fromBigInt(a.getOrElse(0))

  implicit def fromBigInt(a: BigInt) = Json.fromBigInt(a)

  implicit def fromOptionBigDecimal(a: Option[BigDecimal]) = fromBigDecimal(a.getOrElse(0))

  implicit def fromBigDecimal(a: BigDecimal) = Json.fromBigDecimal(a)

  implicit def fromOptioChar(a: Option[Char]) = fromChar(a.getOrElse(' '))

  implicit def fromChar(a: Char) = Json.fromString(a.toString)

}

