// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import edu.gemini.model.p1.mutable.Proposal
import java.io.File
import edu.gemini.model.p1.mutable._
import edu.gemini.model.p1.mutable.SubmissionReceipt
import edu.gemini.model.p1.mutable.SubmissionAccept
import javax.xml.datatype.DatatypeFactory
import edu.gemini.model.p1.mutable.QueueProposalClass

/**
 * One-off hack for 2020B to make non-submitted GT programs with accidental LP requests kind-of
 * work. We replace the mistaken LP request with a Queue request asking for CFH exchange time,
 * since we don't have any actual CFH time in 20B. At Phase 2 we'll change this to be GT time.
 * For 21A we need a way to actually request GT time so this doesn't happen again.
 */
object NonSubmitted {

  def addAcceptance(f: File, p: Proposal): Unit =
    try {
      val oldSub = p.getProposalClass.getLarge.getSubmission
      p.getProposalClass.setLarge(null)
      p.getProposalClass.setQueue {
        val qpc = new QueueProposalClass
        qpc.setExchange {
          val es = new ExchangeSubmission
          es.setPartner(ExchangePartner.CFH) // just because it's unused in 2020B
          es.setPartnerLead(p.getInvestigators.getPi)
          es.setRequest(oldSub.getRequest)
          es.setResponse {
            val res = new SubmissionResponse
            res.setReceipt {
              val rec = new SubmissionReceipt
              rec.setTimestamp(DatatypeFactory.newInstance.newXMLGregorianCalendar)
              rec.setId(f.getName.takeWhile(_ != '.').replace('_', '-'))
              rec
            }
            res.setAccept {
              val acc = new SubmissionAccept
              acc.setMinRecommend(oldSub.getRequest.getMinTime)
              acc.setRecommend(oldSub.getRequest.getTime)
              acc.setRanking(new java.math.BigDecimal(99))
              acc
            }
            res
          }
          es
        }
        qpc
      }
    } catch {
      case _: NullPointerException => throw new ItacException(s"${f.getName} does not have an LP submission.")
    }

}