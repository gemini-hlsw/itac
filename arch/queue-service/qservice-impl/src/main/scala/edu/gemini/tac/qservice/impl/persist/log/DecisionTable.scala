package edu.gemini.tac.qservice.impl.persist.log

import edu.gemini.tac.qengine.p1.{Proposal, QueueBand}
import edu.gemini.tac.qengine.api.QueueCalc
import edu.gemini.tac.qengine.log._

/**
 * Builds a formatted proposal log decision table.  Shows what decisions were
 * made as the queue was being built.
 */
case class DecisionTable(calc: QueueCalc) {
  val log = calc.proposalLog
  val pwProposals = Proposal.expandJoints(calc.queue.bandedQueue(QueueBand.QBand4))

  val headerBg = "#8a8a8a"
  val acceptBg = "#e7fbd9"
  val rejectBg = "#ffbbbc"

  val entryList = log.toList filterNot {
    // ITAC-440: Filter out category over allocation messages for bands 1 and 2.
    _.msg match {
      case RejectCategoryOverAllocation(_, QueueBand.Category.B1_2) => true
      case _ =>false
    }
  }

  val (b12, b3) = entryList.span(_.key.cat == QueueBand.Category.B1_2)

  private def color(pe: ProposalLog.Entry): String =
    pe.msg match {
      case ac: AcceptMessage => acceptBg
      case _ => rejectBg
    }

  private def decision(pe: ProposalLog.Entry): String =
    pe.msg match {
      case dt: ProposalDetailMessage => dt.reason
      case _ => ""
    }

  private def rank(prop: Proposal): String = prop.ntac.ranking.format

  private def rank(pe: ProposalLog.Entry): String =
    pe.msg match {
      case dt: ProposalDetailMessage => rank(dt.prop)
      case _ => ""
    }

  private def pi(pe: ProposalLog.Entry): String =
    pe.msg match {
      case dt: ProposalDetailMessage => dt.prop.piName.getOrElse("")
      case _                         => ""
    }

  private def accetedDetail(ac: AcceptMessage) = {
    <td>
      <table>
        <tr>
          <td>{"%s: %s".format(ac.prop.ntac.partner, LogMessage.formatBoundedTime(ac.partnerBounds))}</td>
          <td>{"%s".format(LogMessage.formatBoundedTime(ac.totalBounds))}</td>
        </tr>
      </table>
    </td>
  }

  private def rejectDetail(rj: RejectMessage) =
    <td>{rj.detail}</td>

  def row(pe: ProposalLog.Entry, index: Int) =
    <tr style={"background:%s".format(color(pe))} class={"partner-%s".format(pe.key.id.partner)}>
      <td>{index}</td>
      <td>{pe.key.id.partner}</td>
      <td style="text-align:right">{rank(pe)}</td>
      <td>{pe.key.id.reference}</td>
      <td>{pi(pe)}</td>
      <td>{decision(pe)}</td>
      {
        pe.msg match {
          case ac: AcceptMessage => accetedDetail(ac)
          case rj: RejectMessage => rejectDetail(rj)
        }
      }
    </tr>

  def pwRow(prop: Proposal, index: Int) =
    <tr style={"background:%s".format(acceptBg)} class={"partner-%s".format(prop.ntac.partner)}>
      <td>{index}</td>
      <td>{prop.ntac.partner}</td>
      <td style="text-align:right">{rank(prop)}</td>
      <td>{prop.ntac.reference}</td>
      <td>{prop.piName.getOrElse("")}</td>
      <td>Accepted</td>
      <td>Poor weather proposal.</td>
    </tr>


  val table =
    <span>
      <span>
        <a href="#queue-decision-log-b12">Band 1/2</a>
        <a href="#queue-decision-log-b3">Band 3</a>
        <a href="#queue-decision-log-pw">Poor Weather</a>
        <span id="qdl-partner-toggle">Partner/All</span>
      </span>
      <div class="clear">
        <table id="queue-decision-log-b12" width="100%">
          <caption>Queue Decision Log</caption>
          <thead style={"background:%s".format(headerBg)}>
            <tr><th>Idx</th><th>P</th><th>Rk</th><th>Reference</th><th>PI</th><th>Decision</th><th>Detail (Partner/All)</th></tr>
          </thead>
          <tbody>
          {
            b12.zipWithIndex.map{ case (element, index) => row(element, index) }
          }
          </tbody>
        </table>
        <table id="queue-decision-log-b3" width="100%">
          <caption style={"background:%s".format(headerBg)}>Band 3 Phase</caption>
          <thead style={"background:%s".format(headerBg)}>
            <tr><th>Idx</th><th>P</th><th>Rk</th><th>Reference</th><th>PI</th><th>Decision</th><th>Detail (Partner/All)</th></tr>
          </thead>
          <tbody>
            {
            b3.zipWithIndex.map{ case (element, index) => row(element, index) }
            }
          </tbody>
        </table>
        <table id="queue-decision-log-pw" width="100%">
          <caption style={"background:%s".format(headerBg)}>Poor Weather Phase</caption>
          <thead style={"background:%s".format(headerBg)}>
            <tr><th>Idx</th><th>P</th><th>Rk</th><th>Reference</th><th>PI</th><th>Decision</th><th>Detail (Partner/All)</th></tr>
          </thead>
          <tbody>
            {
            pwProposals.zipWithIndex.map{ case (element, index) => pwRow(element, index) }
            }
          </tbody>
        </table>
      </div>
    </span>

}
