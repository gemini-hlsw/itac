// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

// import cats.implicits._
import io.circe._
import io.circe.syntax._
import edu.gemini.tac.qengine.api.QueueCalc
import edu.gemini.tac.qengine.log._
import edu.gemini.tac.qengine.log.ProposalLog.Entry
import edu.gemini.tac.qengine.p1.Proposal

trait QueueCalcEncoder {

  implicit val QueueCalcEncoder: Encoder[QueueCalc] =
    Encoder { qc =>

      val accepted: List[Json] =
        qc.queue.zipWithPosition.map { case (p, pos) =>
          Json.obj(
            "id"     -> p.id.reference.asJson,
            "ngo"    -> p.id.partner.id.asJson,
            "comment" -> "TBD".asJson,
            "pid"    -> s"${p.site.abbreviation}-${qc.context.semester.toString}-${p.mode.programId}-${pos.programNumber}".asJson,
            "accept" -> Json.obj(
              "contact" -> "TBD".asJson,
              "email"   -> "TBD".asJson,
              "band"    -> pos.band.number.asJson,
              "award"   -> p.ntac.awardedTime.toString.asJson,
            )
          )
        }

      val rejectedPids: List[Proposal.Id] = {
        val acceptedPids = qc.queue.toList.map(_.id).toSet
        qc.proposalLog.proposalIds.filterNot(acceptedPids).toList
      }

      val rejected: List[Json] =
        rejectedPids.map { pid =>
          Json.obj(
            "id"       -> pid.reference.toString.asJson,
            "ngo"      -> pid.partner.id.asJson,
            "comment" -> "TBD".asJson,
            "messages" -> qc.proposalLog.toList(pid).map {
                            case Entry(_, AcceptMessage(_, _, _))         => sys.error("Unpossible")
                            case Entry(k, _: RejectPartnerOverAllocation) => s"${k.cat.name}: partner over-allocation".asJson
                            case Entry(k, _: RejectNotBand3)              => s"${k.cat.name}: not allowed".asJson
                            case Entry(k, _: RejectNoTime)                => s"${k.cat.name}: no time awarded".asJson
                            case Entry(k, lm)                             => s"${k.cat.name}: $lm".asJson
                          } .asJson,
          )
        }

      Json.obj(
        "proposals" -> Json.fromValues(accepted ++ rejected)
      )
    }

}

object queuecalc extends QueueCalcEncoder