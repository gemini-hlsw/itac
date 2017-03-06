package edu.gemini.tac.qservice.impl.persist.log

import edu.gemini.tac.qengine.p1.QueueBand.{QBand1, QBand2, QBand3, QBand4}
import edu.gemini.tac.qengine.p1.QueueBand.Category.Guaranteed
import edu.gemini.tac.qengine.api.queue.{ProposalQueue, ProposalPosition}
import edu.gemini.tac.qengine.api.QueueCalc
import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.p1._

/**
 *
 */

object QueueTimeTable {
  val headerBg = "#e4e4e4"

  val bandBg   = Map[QueueBand,String](
    QBand1 -> "#fdfdb1",
    QBand2 -> "#e7fbd9",
    QBand3 -> "#b9e7fd",
    QBand4 -> "#e6e6e6"
  )
  val jbandBg   = Map[QueueBand,String](
    QBand1 -> "#d8d897",
    QBand2 -> "#c7d8bb",
    QBand3 -> "#9ec5d8",
    QBand4 -> "#cbcbcb"
  )

  val unusedBg = "#ffbbbc"


  private def colHeader(col: (String, String)) =
    <th style={"padding:5px; text-align:%s".format(col._2)}>{col._1}</th>

  private def headerRow(colNames: (String, String)*) =
    <tr style={"background:%s".format(headerBg)}>{
      colNames.map(colHeader)
    }</tr>

  private def sortedNtacs(jp: JointProposal): List[Ntac] =
    jp.ntacs.sorted(Ntac.MasterOrdering)


  private def jointNtacProp(jp: JointProposal, n: Ntac => String): String =
   jp.ntacs match {
     case List(ntac) => n(ntac)
     case _          => ""
   }

  private def ntacProp(prop: Proposal, n: Ntac => String): String =
    prop match {
      case jp: JointProposal => jointNtacProp(jp, n)
      case _                 => n(prop.ntac)
    }

  private def piName(prop: Proposal): String =
    prop.piName.getOrElse("")

  private def country(prop: Proposal): String =
    ntacProp(prop, _.partner.id)

  private def reference(prop: Proposal): String =
    ntacProp(prop, _.reference)

  private def rank(prop: Proposal): String =
    ntacProp(prop, ntac => ntac.ranking.format)

  private def formatTime(time: Time) = "%.1f".format(time.toHours.value)
  private def formatToo(too: Too.Value) =
    too match {
      case Too.rapid    => "RT"
      case Too.standard => "ST"
      case _            => ""
    }

  private def lgs(prop: Proposal): String =
    if (prop.obsList.exists(_.lgs) || prop.band3Observations.exists(_.lgs)) "LGS" else ""

  private def summaryRow(prop: Proposal, pos: ProposalPosition) =
    <tr style={"background:%s".format(bandBg(pos.band))} class={"partner-%s".format(country(prop))}>
      <td style="text-align:right">{pos.index}</td>
      <td style="text-align:right">{formatTime(prop.time + pos.time)}</td>
      <td style="text-align:right">{formatTime(prop.time)}</td>
      <td style="text-align:left">{piName(prop)}</td>
      <td style="text-align:center">{country(prop)}</td>
      <td style="text-align:left">{reference(prop)}</td>
      <td style="text-align:right">{rank(prop)}</td>
      <td style="text-align:center">{formatToo(prop.too)}</td>
      <td style="text-align:center">{lgs(prop)}</td>
    </tr>

  private def jointCompRow(ntac: Ntac, pos: ProposalPosition) =
    <tr style={"background:%s".format(jbandBg(pos.band))}>
      <td style={"background:%s".format(bandBg(pos.band))}/>
      <td style={"background:%s".format(bandBg(pos.band))}/>
      <td style="text-align:right">{formatTime(ntac.awardedTime)}</td>
      <td style="text-alight:right">{ntac.lead.getOrElse("")}</td>
      <td style="text-align:center">{ntac.partner.id}</td>
      <td style="text-align:left">{ntac.reference}</td>
      <td style="text-align:right">{ntac.ranking.format}</td>
      <td/>
      <td/>
    </tr>

  private def jointCompRows(prop: Proposal, pos: ProposalPosition) =
    prop match {
      case jp: JointProposal =>
        if (jp.ntacs.size > 1)
          sortedNtacs(jp) map { n => jointCompRow(n, pos) }
        else
          Nil
      case _                 => Nil
    }

  private def toRows(prop: Proposal, pos: ProposalPosition) =
    summaryRow(prop, pos) :: jointCompRows(prop, pos)

  def queueTable(calc: QueueCalc): scala.xml.Elem =
    <span>
      <span>
        <span id="qtt-partner-toggle">Partner/All</span>
      </span>
      <div class="clear">
        <table id="queue-time-table">
          <thead>
          {headerRow(("Idx", "right"), ("Cuml", "right"), ("Time", "right"), ("PI", "left"), ("", "right"), ("Reference","left"), ("Rank", "right"), ("ToO", "center"), ("LGS", "center"))}
          </thead>
          <tbody>
          {
            calc.queue.zipWithPosition flatMap { case (prop, pos) => toRows(prop, pos) }
          }
          </tbody>
        </table>
      </div>
    </span>



  def keyTable(queue: ProposalQueue): scala.xml.Elem = {
    val qt       = queue.queueTime
    val bp       = qt.bandPercentages
    val bandPerc = bp.bandPercent.mapValues(_.doubleValue)

    val total    = qt.full.toHours.value
    val used     = queue.usedTime.toHours.value
    val usedPerc = (used/total) * 100.0

    val usedBandPerc: Map[QueueBand,Double] = (for {
      band <- QueueBand.values
    } yield band -> (queue.usedTime(band).toHours.value/total * 100.0)).toMap

    def title(band: QueueBand, bandToTime: QueueBand => Time, bandToPerc: QueueBand => Double): String =
      if (band.isIn(Guaranteed))
        "B%d %.1f (%f+%%)".format(band.number, bandToTime(band).toHours.value, bandToPerc(band))
      else
        ""

    def availCell(band: QueueBand) = {
      <td width={"%f%%".format(bandPerc(band))} style={"background:%s".format(bandBg(band))}>
        {title(band, qt.apply, bandPerc)}
      </td>
    }

    def usedCell(band: QueueBand) = {
      <td width={"%f%%".format(usedBandPerc(band))} style={"background:%s".format(bandBg(band))}>
        {title(band, queue.usedTime, usedBandPerc)}
      </td>
    }

    val availBands = if (queue.queueTime.bandPercentages(QBand4) > Percent.Zero) QueueBand.values else QueueBand.values.init
    val usedBands  = QueueBand.values filter (band => usedBandPerc(band) > 0)

    val unusedPerc = (100.0 - usedPerc)
    def usedColumnCount = usedBands.size + (if (unusedPerc > 0.0) 1 else 0)

    <span>
      <table width="100%">
        <thead>
          <tr style={"background:%s".format(headerBg)}><td colspan={availBands.size.toString}>{"Available %.1f hrs".format(total)}</td></tr>
        </thead>
        <tr>{for {band <- availBands} yield availCell(band)}</tr>
      </table>

      <table width="100%">
        <thead>
          <tr style={"background:%s".format(headerBg)}>
            <td colspan={usedColumnCount.toString}>
              {"Used %.1f hrs (%d%%)".format(used, usedPerc.toInt)}
            </td>
          </tr>
        </thead>
        <tr>
          {for {band <- usedBands} yield usedCell(band)}
          {if (unusedPerc > 0)
            <td width={"%d%%".format(unusedPerc.toInt)} style={"background:%s".format(unusedBg)}/>
          }
        </tr>
      </table>
    </span>
  }

}
