package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.persistence.Partner


/**
 * Groups a partner and its ranking.
 */
case class PartnerRanking(partner: Partner, rank: java.math.BigDecimal, siteName : String) {
  override def toString = "%s(%.1f%s)".format(partner.getName, rank, siteName)
}

