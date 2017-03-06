package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.check.impl.core.StatelessProposalCheckFunction
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import edu.gemini.tac.service.check.ProposalIssue
import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues
import edu.gemini.shared.skycalc.Angle
import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.phase1._

/**
 * UX-1346: Flag non-ToO proposals with RA=0.00 AND Dec=0.00
 *
 */

object ZeroRaAndDecInNonTooCheck extends StatelessProposalCheckFunction {
  val name        = "Zero RA/Dec in non-ToO"
  val description = "Zero RA/Dec is usually indicator of ToO."

  def zeroMessage = "Proposal contains a non-ToO target with coordinates RA/Dec = 0."
  def zeroTargets(p: Proposal) = singleWarning(p, zeroMessage, ProposalIssueCategory.Structural)

  def coordsAreZeroZero(c : Coordinates) = {
    c.getRa.compareToAngle(Angle.ANGLE_0DEGREES) == 0 &&
    c.getDec.compareToAngle(Angle.ANGLE_0DEGREES) == 0
  }

  def apply(p: Proposal): Set[ProposalIssue] = {
    val ts = p.getTargets.asScala
    val rs = ts.map{t : Target =>
     t match {
       case st : SiderealTarget => coordsAreZeroZero(st.getCoordinates)
       case nst : NonsiderealTarget => nst.getEphemeris.asScala.map{ ee : EphemerisElement =>
                coordsAreZeroZero(ee.getCoordinates)
              }.contains(true)
       case too : TooTarget => false
       case _ => throw new IllegalArgumentException("Unknown target type received by proposal checker [%s]".format(t.getClass))
     }
   }
   rs.contains(true) match {
      case true => zeroTargets(p)
      case false => noIssues
   }
  }
}