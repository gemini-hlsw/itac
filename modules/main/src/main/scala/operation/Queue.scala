// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import cats._
import cats.effect._
import cats.implicits._
import edu.gemini.tac.qengine.api.QueueEngine
import io.chrisdavenport.log4cats.Logger
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.log.AcceptMessage
import edu.gemini.tac.qengine.log.RejectPartnerOverAllocation
import edu.gemini.tac.qengine.log.RejectNotBand3
import edu.gemini.tac.qengine.log.RejectNoTime
import java.nio.file.Path
import _root_.edu.gemini.tac.qengine.log.RejectCategoryOverAllocation
import edu.gemini.tac.qengine.log.RejectTarget
import edu.gemini.tac.qengine.log.RejectConditions

object Queue {

  /**
    * @param siteConfig path to site-specific configuration file, which can be absolute or relative
    *   (in which case it will be resolved relative to the workspace directory).
    */
  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        ws.commonConfig.map(_.engine.partners).flatMap { partners =>
          computeQueue(ws).flatMap { case (ps, queueCalc) =>
            val log = queueCalc.proposalLog
            Sync[F].delay {

              val pids = log.proposalIds // proposals that were considered

              // println(s"${Console.BOLD}The following proposals were not considered due to site, mode, or lack of awarded time or observations.${Console.RESET}")
              // ps.filterNot(p => pids.contains(p.id)).foreach { p =>
              //   println(f"- ${p.id.reference} (${p.site.abbreviation}, ${p.mode.programId}%2s, ${p.ntac.awardedTime.toHours.value}%4.1fh ${p.ntac.partner.id}, ${p.obsList.length}%3d obs)")
              // }
              // println()

              println(s"\n\n${Console.BOLD}Queue Report for ${queueCalc.context.site.abbreviation}-${queueCalc.context.semester}${Console.RESET}\n")

              List(QueueBand.Category.B1_2, QueueBand.Category.B3).foreach { qc =>
                println(s"${Console.BOLD}The following proposals were rejected for $qc.${Console.RESET}")
                pids.foreach { pid =>
                  val p = ps.find(_.id == pid).get
                  log.get(pid, qc) match {
                    case None =>
                    case Some(AcceptMessage(_, _, _)) =>
                    case Some(m: RejectPartnerOverAllocation) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.detail}")
                    case Some(m: RejectNotBand3) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.detail}")
                    case Some(m: RejectNoTime) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.detail}")
                    case Some(m: RejectCategoryOverAllocation) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.detail}")
                    case Some(m: RejectTarget) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.detail} -- ${ObservationDigest.digest(m.obs.p1Observation)}")
                    case Some(m: RejectConditions) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.detail} -- ${ObservationDigest.digest(m.obs.p1Observation)}")
                    case Some(lm) => println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s $lm")
                  }
                }
                println()
              }

              val separator = "━" * 100 + "\n"
              println(separator)

              println(s"${Console.BOLD}RA/Conditions Bucket Allocations:                                                       Rem   Avail${Console.RESET}")
              println(queueCalc.bucketsAllocation.raTablesANSI)
              println()

              println(separator)

              QueueBand.values.foreach { qb =>
                val q = queueCalc.queue
                println(s"${Console.BOLD}The following proposals were accepted for Band ${qb.number}.${Console.RESET}")
                println(qb.number match {
                  case 1 => Console.YELLOW
                  case 2 => Console.GREEN
                  case 3 => Console.BLUE
                  case 4 => Console.RED
                })
                q.bandedQueue.get(qb).orEmpty.foreach { p =>
                  println(f"- ${p.id.reference}%-20s -> ${q.programId(p).get} ${p.piName.orEmpty}")
                }
                println(Console.RESET)
              }

              println(separator)

              // Partners that appear in the queue
              partners.filter(queueCalc.queue.queueTime(_).toHours.value > 0) foreach { p =>
                println(s"${Console.BOLD}Partner Details for $p ${Console.RESET}")
                QueueBand.values.foreach { qb =>
                  val q = queueCalc.queue
                  val color = qb.number match {
                    case 1 => Console.YELLOW
                    case 2 => Console.GREEN
                    case 3 => Console.BLUE
                    case 4 => Console.RED
                  }
                  val included = q.bandedQueue.get(qb).orEmpty.filter(_.ntac.partner == p)
                  included.foreach { p =>
                    println(f"$color- ${p.id.reference}%-20s -> ${q.programId(p).get} ${p.time.toHours.value}%5.1f ${p.piName.orEmpty}${Console.RESET}")
                  }
                  if (qb.number < 4) {
                    val used = q.usedTime(qb, p).toHours.value
                    val avail = q.queueTime(qb, p).toHours.value
                    val pct   = if (avail == 0) 0.0 else (used / avail) * 100
                    println(f"                                         $used%5.1f/${avail}%5.1f ($pct%3.1f%%)")
                  } else {
                    val used = q.usedTime(qb, p).toHours.value
                    println(f"                                         $used%5.1f")
                  }
                }
                println()
              }



              ExitCode.Success

            }
          }
        }

  }

}


