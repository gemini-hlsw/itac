package edu.gemini.tac.qengine.util

import edu.gemini.tac.qengine.util.CoordinateFields.Sign

/**
 * Representation of the parts of a RA or Dec specified in +/-XX:XX:XX.XX
 * format.
 */
case class CoordinateFields(sign: Sign, hd: Int, m: Int, s: Double) {
  private def rawValue: Double = sign.sgn * (hd + m / 60.0 + s / 3600.0)

  /**
   * Interprets the value as hours (such as for an RA).
   */
  def asHrs: Angle = new Angle(rawValue, Angle.Hr)

  /**
   * Interprets the value as degrees (such as for a Dec).
   */
  def asDeg: Angle = new Angle(rawValue, Angle.Deg)
}

object CoordinateFields {
  abstract sealed class Sign {
    def sgn: Int
  }

  object Sign {
    case object Neg extends Sign {
      override def sgn: Int = -1
      override def toString: String = "-"
    }

    case object Pos extends Sign {
      override def sgn: Int = 1
      override def toString: String = "+"
    }

    def parse(s: String): Option[Sign] =
      s match {
        case "-" => Some(Neg)
        case "+" => Some(Pos)
        case _ => None
      }
  }

  val Lead       = """\s*([+\-])?\s*"""
  val WholeVal   = """(\d+)"""
  val DecimalVal = """(\d+(\.\d*)?)"""
  val Sep        = """\s*[\s:]\s*"""
  val Trail      = """\s*"""

  val Full     = (Lead + WholeVal + Sep + WholeVal + Sep + DecimalVal + Trail).r
  val NoSec    = (Lead + WholeVal + Sep + DecimalVal + Trail).r
  val NoMin    = (Lead + DecimalVal + Trail).r

  private def parseSign(s: String): Sign = Option(s).flatMap(Sign.parse).getOrElse(Sign.Pos)

  private def split(timeAmt: Double): (Int, Double) = {
    val i = timeAmt.floor.toInt
    val d = 60.0 * (timeAmt - i)
    (i, d)
  }

  def parse(s: String): Option[CoordinateFields] =
    s match {
      // hours/deg min sec.ss
      case Full(signStr, hdStr, mStr, sStr, _) => {
        val sign = parseSign(signStr)
        Some(CoordinateFields(sign, hdStr.toInt, mStr.toInt, sStr.toDouble))
      }

      // hours/deg min.mm
      case NoSec(signStr, hdStr, mStr, _) => {
        val sign = parseSign(signStr)
        val (min, sec) = split(mStr.toDouble)
        Some(CoordinateFields(sign, hdStr.toInt, min, sec))
      }

      // hours.hh
      case NoMin(signStr, hdStr,_) => {
        val sign = parseSign(signStr)
        val (hrs, minD) = split(hdStr.toDouble)
        val (min, sec) = split(minD)
        Some(CoordinateFields(sign, hrs, min, sec))
      }

      case _ => None
    }
}