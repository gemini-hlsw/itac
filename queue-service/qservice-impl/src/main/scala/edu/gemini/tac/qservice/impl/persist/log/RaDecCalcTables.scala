package edu.gemini.tac.qservice.impl.persist.log

import edu.gemini.qengine.skycalc.{RaDecBinCalc, RaBinSize}
import edu.gemini.tac.qengine.api.config.{DecBinGroup, RaBinGroup, SiteSemesterConfig}
import edu.gemini.tac.qengine.ctx.Context
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qservice.impl.shutdown.ShutdownCalc

import collection.JavaConverters._

/**
 * Formats RA/Dec bin information into a couple of tables.
 */
case class RaDecCalcTables(siteSemester: SiteSemesterConfig) {
  val headerBg = "#e4e4e4"
  val dataBg   = "white"
  val footerBg = "white"
  val mins     = siteSemester.raLimits.sizeMin
  val binSize  = new RaBinSize(mins)

  val site     = siteSemester.site
  val semester = siteSemester.semester
  val ctx      = new Context(site, semester)

  val startMins = for {
    m <- (0 to RaBinGroup.TotalMin by mins).init
  } yield m

  val raTimes       = siteSemester.raLimits.bins.toList
  val shutdownTimes =  ShutdownCalc.trim(siteSemester.shutdowns.sorted, ctx).map {
      shutdown =>  shutdown -> ShutdownCalc.timePerRa(shutdown, binSize)
  }

  case class RaRow(title: String, bg: String, align: String, data: List[Double]) {
    def split: List[RaRow] =
      if (data.size <= 12)
        List(this)
      else {
        val (lst0, lst1) = data.splitAt(data.size/2)
        val res0 = RaRow(title, bg, align, lst0).split
        val res1 = RaRow(title, bg, align, lst1).split
        res0 ++ res1
      }

    def trimIfAllIntegral(nums: List[String]): List[String] =
      if (nums.forall(_.endsWith(".0")))
        nums map { s => s.substring(0, s.length-2) }
      else
        nums

    def formatData: List[String] = trimIfAllIntegral(data map { d => "%.1f".format(d) })
  }

  def timesToHours(lst: List[Time]): List[Double] = lst map {_.toHours.value }
  def nominalTimes: List[Time] =
    RaDecBinCalc.RA_CALC.calc(site, semester.getStartDate(site), semester.getEndDate(site), binSize).asScala.toList map {
      hrs => Time.hours(hrs.getHours)
    }

  // Nominal RA times before adjustment for shutdowns.
  def nominalRows: List[RaRow] =
    RaRow("Nominal " + semester.toString, "white", "right", timesToHours(nominalTimes)).split

  val headerRows = RaRow("RA", headerBg, "center", startMins.toList map { m => m/60.0 }).split
  val shutdowns  = shutdownTimes map {
    case (sd, times) => RaRow(sd.toDateString, dataBg, "right", timesToHours(times)).split
  }
  val finalRows  = RaRow("Available", footerBg, if (shutdowns.nonEmpty) "right" else "center", timesToHours(raTimes)).split

  val dataRows = if (shutdowns.isEmpty) Nil else nominalRows :: shutdowns
  val allRows  = (headerRows :: dataRows) ++ List(finalRows)

  val raTable = {
    <table>
      <tr><th colspan={allRows.head.head.data.length.toString}>Time Limit Per RA Bin (Hours)</th></tr>
      {
        allRows.transpose.flatten.map {
          case (row) =>
            <tr style={"background:%s".format(row.bg)}>
              <td style="text-align:right">{row.title}</td>
              {row.formatData map { d => <td style={"text-align:%s".format(row.align)}>{d}</td>}}
            </tr>
        }
      }
    </table>
  }

  val decPercs  = siteSemester.decLimits.bins.toList.map(bin => bin.binValue.value.min(100))
  val degs      = DecBinGroup.TotalDeg / decPercs.length

  val decRanges =
    for {
      dec0 <- (-90 to 90 by degs).toList.init
      dec1 = dec0 + degs
    } yield "(%s, %s)".format(dec0, dec1)


  val decTable =
    <table>
      <tr><th colspan="3">Percentage Per Dec Bin</th></tr>
      <tr style={"background:%s".format(headerBg)}><th>Dec&nbsp;Range</th><th>Percentage</th><th width="100%"/></tr>
      {
        decRanges.zip(decPercs).reverse map {
          case (range, perc) => <tr><td>{range}</td><td style="text-align:right">{perc}</td><td/></tr>
        }
      }
    </table>
}