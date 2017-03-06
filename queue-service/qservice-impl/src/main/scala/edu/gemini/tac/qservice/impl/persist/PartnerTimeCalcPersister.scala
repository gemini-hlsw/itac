package edu.gemini.tac.qservice.impl.persist

import edu.gemini.tac.persistence.{Partner => PsPartner}
import edu.gemini.tac.persistence.phase1.{TimeAmount => PsTime}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.queues.partnerCharges.{AvailablePartnerTime      => PsAvailableTime}
import edu.gemini.tac.persistence.queues.partnerCharges.{ClassicalPartnerCharge    => PsClassicalTime}
import edu.gemini.tac.persistence.queues.partnerCharges.{RolloverPartnerCharge     => PsRolloverTime}
import edu.gemini.tac.persistence.queues.partnerCharges.{PartnerCharge             => PsPartnerCharge}
import edu.gemini.tac.psconversion.{Partners, TimeConverter}

import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, PartnerTimeCalc}
import edu.gemini.tac.qengine.ctx.Partner

import scala.collection.JavaConverters._

/**
 * Persists partner time calculation.
 */
object PartnerTimeCalcPersister {
  private case class Persister(partnerMap: Map[Partner, PsPartner], queue: PsQueue) {
    val partners = Partners.fromQueue(queue)

    def persist[T <: PsPartnerCharge](
        partnerTime: PartnerTime,
        create: (PsQueue, PsPartner, PsTime) => T,
        update: (PsQueue, java.util.Map[PsPartner, T]) => Unit): Unit = {

      partners.foreach { parts =>
        val timeKv   = mapTimes(parts, partnerTime, partnerMap)
        val chargeKv = timeKv.map {
          case (psPartner, psTime) => (psPartner, create(queue, psPartner, psTime))
        }

        update(queue, chargeKv.toMap.asJava)
      }
    }
  }

  // Turns a PartnerTime object into a sequence (PsPartner, PsTime) in order to
  // store it in the persistence model.
  private def mapTimes(partners : Partners,  partnerTime: PartnerTime, partnerMap: Map[Partner, PsPartner]): Traversable[(PsPartner, PsTime)] =
    partners.values.map {
      p => (partnerMap(p), TimeConverter.toPsTimeHours(partnerTime(p)))
    }

  def persist(ptc: PartnerTimeCalc, partnerMap: Map[Partner, PsPartner], queue: PsQueue) {
    val persister = Persister(partnerMap, queue)

    val av: (PsQueue, PsPartner, PsTime) => PsAvailableTime = new PsAvailableTime(_, _, _)
    persister.persist[PsAvailableTime](ptc.base, av, _.setAvailablePartnerTimes(_))

    val cp: (PsQueue, PsPartner, PsTime) => PsClassicalTime = new PsClassicalTime(_, _, _)
    persister.persist[PsClassicalTime](ptc.classical, cp, _.setClassicalPartnerCharges(_))

    val ro: (PsQueue, PsPartner, PsTime) => PsRolloverTime = new PsRolloverTime(_, _, _)
    persister.persist[PsRolloverTime](ptc.rollover, ro, _.setRolloverPartnerCharges(_))
  }
}