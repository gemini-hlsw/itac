package edu.gemini.tac.qengine.log

/**
 * A trait for all proposal messages.  It includes what is intended to
 * be a brief "reason" message and a longer "detail" message.
 */
trait ProposalDetailMessage extends LogMessage {
  def reason: String
  def detail: String

  override def subToXML = <ProposalDetailMessage>
      <Reason>{reason}</Reason>
      <Detail>{detail}</Detail>
    </ProposalDetailMessage>
}