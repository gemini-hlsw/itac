// // Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// // For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

// package edu.gemini.tac.qengine.p2.rollover

// import edu.gemini.tac.qengine.p2.ObservationId
// import edu.gemini.tac.qengine.p1._
// import edu.gemini.tac.qengine.util.{QEngineUtil, Time, CoordinateFields}
// import edu.gemini.tac.qengine.ctx.Partner

// /**
//  * Parses a rollover report CSV string into a RolloverReport.
//  */
// object RolloverReportParser {
//   def LINE_FORMAT(n: Int, s: String): String =
//     "Line %d: Expected 'partner,obs-id,ra,dec,CC,IQ,SB,WV,hours', not %s".format(n, s)

//   def BAD_FIELD(n: Int, name: String, value: String): String =
//     "Line %d: Could not parse %s value of '%s'".format(n, name, value)
// }

// import RolloverReportParser._

// class RolloverReportParser(partners: List[Partner]) {

//   private class FieldParser[T](val name: String, val toOption: (String => Option[T])) {
//     def apply(n: Int, s: String): RolloverParseError Either T =
//       toOption(s).toRight(error(n, s))

//     def error(n: Int, s: String): RolloverParseError =
//       RolloverParseError(BAD_FIELD(n, name, s))
//   }

//   private val partner = new FieldParser("partner", s => partners.find(_.id == s))
//   private val obsId   = new FieldParser("obs-id", s => ObservationId.parse(s))
//   private val ra      = new FieldParser("RA", s => CoordinateFields.parse(s).map { _.asHrs })
//   private val dec     = new FieldParser("dec", s => CoordinateFields.parse(s).map { _.asDeg })
//   private val cc      = new FieldParser("CC", s => CloudCover.values.find(_.toString == s))
//   private val iq      = new FieldParser("IQ", s => ImageQuality.values.find(_.toString == s))
//   private val sb      = new FieldParser("SB", s => SkyBackground.values.find(_.toString == s))
//   private val wv      = new FieldParser("WV", s => WaterVapor.values.find(_.toString == s))
//   private val hrs = new FieldParser(
//     "hours",
//     s =>
//       try {
//         val d = s.toDouble
//         if (d < 0) None else Some(Time.hours(d))
//       } catch {
//         case _: NumberFormatException => None
//       }
//   )

//   private def toRollover(
//     n: Int,
//     rolloverObsStr: String
//   ): RolloverParseError Either RolloverObservation =
//     rolloverObsStr.split(',').map(_.trim).toList match {
//       case List(partnerStr, obsIdStr, raStr, decStr, ccStr, iqStr, sbStr, wvStr, hrsStr) =>
//         for {
//           partner <- partner(n, partnerStr).right
//           obsId   <- obsId(n, obsIdStr).right
//           ra      <- ra(n, raStr).right
//           dec     <- dec(n, decStr).right
//           cc      <- cc(n, ccStr).right
//           iq      <- iq(n, iqStr).right
//           sb      <- sb(n, sbStr).right
//           wv      <- wv(n, wvStr).right
//           hr      <- hrs(n, hrsStr).right
//         } yield RolloverObservation(
//           partner,
//           obsId,
//           Target(ra, dec),
//           ObservingConditions(cc, iq, sb, wv),
//           hr
//         )
//       case _ => Left(LINE_FORMAT(n, rolloverObsStr))
//     }

//   // Breaks the report into lines, groups them with the line number, filters
//   // out comments and empty lines.
//   private def toNumberedLines(reportStr: String): List[(Int, String)] =
//     reportStr.lines.toList.zipWithIndex map {
//       case (s, n) => (n + 1, s.trim)
//     } filterNot {
//       case (_, s) => s.isEmpty || s.startsWith("#")
//     }

//   private def toParsedRollovers(
//     reportStr: String
//   ): List[RolloverParseError Either RolloverObservation] =
//     toNumberedLines(reportStr) map {
//       case (n, line) => toRollover(n, line)
//     }

//   def parse(reportStr: String): RolloverParseError Either RolloverReport =
//     for {
//       obsList <- QEngineUtil.promoteEither(toParsedRollovers(reportStr)).right
//     } yield RolloverReport(obsList)
// }
