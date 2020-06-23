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
import java.time.LocalDateTime
import scala.collection.JavaConverters._

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
              rec.setTimestamp {
                val c = DatatypeFactory.newInstance.newXMLGregorianCalendar
                val now = LocalDateTime.now
                c.setYear(now.getYear)
                c.setMonth(now.getMonth.getValue)
                c.setDay(now.getDayOfMonth)
                c.setHour(now.getHour())
                c.setMinute(now.getMinute())
                c.setSecond(now.getSecond())
                c.setMillisecond(0)
                c
              }
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

      // Make copies of every observation in band 3 because some of these proposals end up there.
      // Otherwise import will fail.
      p.getObservations.getObservation.asScala.toList /* force a copy! */.foreach { o =>
        val obs = new Observation
        obs.setBand(Band.BAND_3)
        obs.setBlueprint(o.getBlueprint)
        obs.setCondition(o.getCondition)
        obs.setEnabled(o.isEnabled)
        obs.setGuide(o.getGuide)
        obs.setMeta(o.getMeta)
        obs.setPartTime(o.getPartTime)
        obs.setProgTime(o.getProgTime)
        obs.setTarget(o.getTarget)
        obs.setTime(o.getTime)
        p.getObservations.getObservation.add(obs)
      }


    } catch {
      case _: NullPointerException => throw new ItacException(s"${f.getName} does not have an LP submission.")
    }

  }