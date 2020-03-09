// package edu.gemini.tac.qengine.p2.rollover

// import edu.gemini.tac.qengine.p2.ObservationId
// import edu.gemini.tac.qengine.p1._
// import edu.gemini.tac.qengine.util.{Time, CoordinateFields}
// import edu.gemini.tac.qengine.p1.CloudCover._
// import edu.gemini.tac.qengine.p1.SkyBackground._
// import edu.gemini.tac.qengine.p1.ImageQuality._
// import edu.gemini.tac.qengine.p1.WaterVapor._
// import edu.gemini.tac.qengine.ctx.TestPartners

// object RolloverFixture {
//   val parser = new RolloverReportParser(TestPartners.All)

//   val lineNormal = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,SB20,WV20,1.0"
//   val obsNormal = RolloverObservation(
//     TestPartners.AR,
//     ObservationId.parse("GS-2011A-Q-1-10").get,
//     Target(
//       CoordinateFields.parse("10:10:10").get.asHrs,
//       CoordinateFields.parse("20:20:20").get.asDeg
//     ),
//     ObservingConditions(CC50, IQ20, SB20, WV20),
//     Time.hours(1.0)
//   )

//   val lineSpaces = "AU , GS-2011A-Q-2-20 ,12:11:10, 09:08:07, CC70, IQ70, SB50, WV50, 2"
//   val obsSpaces = RolloverObservation(
//     TestPartners.AU,
//     ObservationId.parse("GS-2011A-Q-2-20").get,
//     Target(
//       CoordinateFields.parse("12:11:10").get.asHrs,
//       CoordinateFields.parse("09:08:07").get.asDeg
//     ),
//     ObservingConditions(CC70, IQ70, SB50, WV50),
//     Time.hours(2.0)
//   )

//   val lineAnyConds = "BR,GS-2010A-Q-3-30,03:04:05,06:07:08,CCAny,IQAny,SBAny,WVAny,3"
//   val obsAnyConds = RolloverObservation(
//     TestPartners.BR,
//     ObservationId.parse("GS-2010A-Q-3-30").get,
//     Target(
//       CoordinateFields.parse("03:04:05").get.asHrs,
//       CoordinateFields.parse("06:07:08").get.asDeg
//     ),
//     ObservingConditions(CCAny, IQAny, SBAny, WVAny),
//     Time.hours(3.0)
//   )

//   val obsGN = RolloverObservation(
//     TestPartners.CA,
//     ObservationId.parse("GN-2011A-Q-4-40").get,
//     Target(
//       CoordinateFields.parse("01:01:01").get.asHrs,
//       CoordinateFields.parse("02:02:02").get.asDeg
//     ),
//     ObservingConditions(CCAny, IQAny, SBAny, WVAny),
//     Time.hours(4.0)
//   )

// }
