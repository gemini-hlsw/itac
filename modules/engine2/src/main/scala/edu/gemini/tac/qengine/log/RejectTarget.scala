package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.{Observation, Proposal, QueueBand}
import edu.gemini.tac.qengine.util.Time
import xml.Elem

object RejectTarget extends TimeBinMessageFormatter {
  trait RaDecType {
    def toXML : Elem
  }

  case object Ra extends RaDecType {
    override def toString = "RA"
    def toXML = <RaLimit/>
  }
  case object Dec extends RaDecType {
    override def toString = "Dec"
    def toXML = <DecLimit/>
  }

  private val reasonTemplate = "%s Limit"
  def reason(raDecType: RaDecType): String = reasonTemplate.format(raDecType)
}

final case class RejectTarget(prop: Proposal, obs: Observation, band: QueueBand, raDecType: RejectTarget.RaDecType, cur: Time, max: Time) extends ObsRejectMessage {
  def reason: String = RejectTarget.reason(raDecType)
  def detail: String = RejectTarget.detail(prop, obs, band, cur, max)

  override def toXML = <RejectTarget>
    <Reason>{ raDecType.toXML }</Reason>
    <Proposal id={ prop.id.toString }/>
    { obs.target.toXml }
    </RejectTarget>
}
