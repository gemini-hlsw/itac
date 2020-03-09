package edu.gemini.tac.persistence.phase1

import edu.gemini.tac.persistence.Partner
import submission.{Submission, NgoSubmission}
;

/**
 * Pairs a Partner a proposal reference "number"..
 */
final case class PartnerReference(partner: Partner, ref: String, siteName : String) {
  override def toString: String = "%s(%s)%s".format(partner.getPartnerCountryKey, ref, siteName)
}

object PartnerReference {
  /**
   * Extracts a PartnerReference from the given submission if both the partner
   * and reference "number" are defined.
   */
  def extract(sub: Submission, siteName : String): Option[PartnerReference] =
    for {
      p <- Option(sub.getPartner)
      r <- Option(sub.getReceipt)
      id <- Option(r.getReceiptId)
    } yield PartnerReference(p, id, siteName)
}