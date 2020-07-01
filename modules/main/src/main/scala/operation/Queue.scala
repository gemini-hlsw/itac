// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import edu.gemini.tac.qengine.impl.QueueEngine.RemovedRejectMessage
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.p1.Proposal
import itac.util.Colors
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
import edu.gemini.tac.qengine.log.RejectCategoryOverAllocation
import edu.gemini.tac.qengine.log.RejectOverAllocation
import edu.gemini.tac.qengine.log.RejectTarget
import edu.gemini.tac.qengine.log.RejectConditions
import edu.gemini.tac.qengine.ctx.Partner

object Queue {

  /**
   * Given a List[Proposal] yield a List[List[Proposal]] which groups proposals sharing the same
   * pi and title. These are joint proposals. The position in the list is determined by the first
   * occurrence.
   */
  def groupedJoints(ps: List[Proposal]): List[List[Proposal]] =
    ps.groupBy(p => (p.piName, p.p1proposal.title)).values.toList.sortBy(_.head.ntac.ranking.num.orEmpty)

  def printWithGroupBars[A](as: List[A]): Unit =
    as match {
      case Nil => ()
      case a :: Nil => println(s" $a")
      case a :: as  =>
        println(s"┌$a") // first one
        def printMany(xs: List[A]): Unit =
          xs match {
            case Nil => ()
            case a :: Nil => println(s"└$a") // last one
            case a :: xs  => println(s"│$a"); printMany(xs) // middle ones
          }
        printMany(as)
    }

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
        computeQueue(ws).flatMap { case (ps, queueCalc) =>
          val log = queueCalc.proposalLog
          Sync[F].delay {

            val pids = log.proposalIds // proposals that were considered
            val separator = "━" * 100 + "\n"

            // println(s"${Colors.BOLD}The following proposals were not considered due to site, mode, or lack of awarded time or observations.${Colors.RESET}")
            // ps.filterNot(p => pids.contains(p.id)).foreach { p =>
            //   println(f"- ${p.id.reference} (${p.site.abbreviation}, ${p.mode.programId}%2s, ${p.ntac.awardedTime.toHours.value}%4.1fh ${p.ntac.partner.id}, ${p.obsList.length}%3d obs)")
            // }
            // println()

            println(s"\n${Colors.BOLD}${queueCalc.context.site.displayName} ${queueCalc.context.semester} Queue Candidate${Colors.RESET}")
            println(s"${new java.util.Date}\n") // lazy, this has a reasonable default toString

            println(separator)

            println(s"${Colors.BOLD}RA/Conditions Bucket Allocations:                                                    Limit    Used   Avail${Colors.RESET}")
            println(queueCalc.bucketsAllocation.raTablesANSI)
            println()

            println(separator)
            val result = QueueResult(queueCalc)

            QueueBand.values.foreach { qb =>

              println(s"${Colors.BOLD}The following proposals were accepted for Band ${qb.number}.${Colors.RESET}")
              println(qb.number match {
                case 1 => Colors.YELLOW
                case 2 => Colors.GREEN
                case 3 => Colors.BLUE
                case 4 => Colors.RED
              })
              result.entries(qb).sortBy(_.proposals.head.ntac.ranking.num.orEmpty).foreach { case QueueResult.Entry(ps, pid) =>
                val ss = ps.map { p =>
                  f"${p.ntac.ranking.num.orEmpty}%5.1f ${p.id.reference}%-30s ${p.piName.orEmpty.take(20)}%-20s ${p.time.toHours.value}%5.1f h  $pid"
                }
                printWithGroupBars(ss.toList)
              }
              println(Colors.RESET)
            }

            println(separator)

            def hasProposals(p: Partner): Boolean = queueCalc.queue.toList.exists(_.ntac.partner == p)

            // Partners that appear in the queue
            Partner.all.sortBy(_.id).filter(hasProposals) foreach { p =>
              println(s"${Colors.BOLD}Partner Details for ${if (p.id == "CFH") "GT" else p.toString} ${Colors.RESET}\n")
              QueueBand.values.foreach { qb =>
                val q = queueCalc.queue

                print(qb.number match {
                  case 1 => Colors.YELLOW
                  case 2 => Colors.GREEN
                  case 3 => Colors.BLUE
                  case 4 => Colors.RED
                })
                result.entries(qb, p).sortBy(_.proposals.head.ntac.ranking.num.orEmpty).foreach { case QueueResult.Entry(ps, pid) =>
                  val ss = ps.map { p =>
                    f"${p.ntac.ranking.num.orEmpty}%5.1f ${p.id.reference}%-30s ${p.piName.orEmpty.take(20)}%-20s ${p.time.toHours.value}%5.1f h  $pid"
                  }
                  printWithGroupBars(ss.toList)
                }
                print(Colors.RESET)

                if (qb.number < 4) {
                  val used  = q.usedTime(qb, p).toHours.value
                  val avail = q.queueTime(qb, p).toHours.value
                  val pct   = if (avail == 0) 0.0 else (used / avail) * 100

                  if (qb != QueueBand.QBand3) {
                    println(f"                                                 B${qb.number} Total: $used%5.1f h/${avail}%5.1f h ($pct%3.1f%%)")
                  }

                  // After the Band2 total print an extra B1+B2 total.
                  if (qb == QueueBand.QBand2) {
                    val used  = (q.usedTime(QueueBand.QBand1, p) + q.usedTime(QueueBand.QBand2, p)).toHours.value
                    val avail = (q.queueTime(QueueBand.QBand1, p) + q.queueTime(QueueBand.QBand2, p)).toHours.value
                    val pct   = if (avail == 0) 0.0 else (used / avail) * 100
                    println(f"                                              B1+B2 Total: $used%5.1f h/${avail}%5.1f h ($pct%3.1f%% ≤ ${(queueCalc.queue.queueTime.overfillAllowance(QueueBand.Category.B1_2).foldMap(_.doubleValue) + 100.0)}%3.1f%%)")
                  }

                  if (qb == QueueBand.QBand3) {
                    println(f"                                                 B${qb.number} Total: $used%5.1f h/${avail}%5.1f h ($pct%3.1f%% ≤ ${(queueCalc.queue.queueTime.overfillAllowance(QueueBand.Category.B3).foldMap(_.doubleValue) + 100.0)}%3.1f%%)")
                  }

                } else {
                  val used = q.usedTime(qb, p).toHours.value
                  println(f"                                                 B${qb.number} Total: $used%5.1f h\n")
                }

                  println()

              }
              println()
            }


            println(separator)
            println(s"${Colors.BOLD}Rejection Report for ${queueCalc.context.site.abbreviation}-${queueCalc.context.semester}${Colors.RESET}\n")

            List(QueueBand.Category.B1_2, QueueBand.Category.B3).foreach { qc =>
              println(s"${Colors.BOLD}The following proposals were rejected for $qc.${Colors.RESET}")
              pids.toList.flatMap(pid => ps.find(_.id == pid)).sortBy(_.ntac.ranking.num).foreach { p=>
                val pid = p.id
                log.get(pid, qc) match {
                  case None =>
                  case Some(AcceptMessage(_, _, _))          => //println(f"- ${pid.reference}%-20s ${p.piName.orEmpty}%-15s 👍")
                  case Some(m: RejectPartnerOverAllocation)  => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Partner full:"}%-20s ${m.detail}")
                  case Some(m: RejectNotBand3)               => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Not band 3:"}%-20s ${m.detail}")
                  case Some(m: RejectNoTime)                 => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"No time awarded:"}%-20s ${m.detail}")
                  case Some(m: RejectCategoryOverAllocation) => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Category overallocated:"}%-20s ${m.detail}")
                  case Some(m: RejectTarget)                 => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${m.raDecType + " bin full:"}%-20s ${m.detail} -- ${ObservationDigest.digest(m.obs.p1Observation)}")
                  case Some(m: RejectConditions)             => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Conditions bin full:"}%-20s ${m.detail} -- ${ObservationDigest.digest(m.obs.p1Observation)}")
                  case Some(m: RejectOverAllocation)         => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Overallocation"}%-20s ${m.detail}")
                  case Some(m: RemovedRejectMessage)         => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Unknown"}%-20s ${m.detail}")
                  case Some(lm)                              => println(f"${p.ntac.ranking.num.orEmpty}%5.1f ${pid.reference}%-20s ${p.piName.orEmpty}%-15s ${"Miscellaneous"}%-20s ${lm.getClass.getName}")
                }
              }
              println()
            }

            println(s"${Colors.BOLD}The following proposals for ${queueCalc.context.site.abbreviation} do not appear in the proposal log:${Colors.RESET}")
            ps.foreach { p =>
              val b12msg = log.get(p.id, QueueBand.Category.B1_2)
              val b3msg  = log.get(p.id, QueueBand.Category.B3)
              if (p.site == queueCalc.context.site && b12msg.isEmpty && b3msg.isEmpty) {
                println(f"- ${p.id.reference}%-30s ${p.piName.orEmpty}%-20s  ${p.time.toHours.value}%5.1f h (${p.mode})")
              }
            }
            println()

            // Find proposals with divided time.
            val dividedProposals: List[Proposal] =
              ps.sortBy(_.id.reference).filter(p => p.time != p.undividedTime && p.site == queueCalc.context.site)

            val thisSite = queueCalc.context.site
            val otherSite = if (thisSite == Site.GN) Site.GS else Site.GN

            if (dividedProposals.nonEmpty) {
              println(separator)
              println(s"${Colors.BOLD}Time proportions for ${thisSite} proposals that also have awarded time at ${otherSite} ${Colors.RESET}\n")
              println(s"${Colors.BOLD}  Reference         PI                      Award     GN     GS${Colors.RESET}")
                                    //- CA-2020B-013     Drout                   3.8 h    2.0    1.8
              dividedProposals.foreach { p =>
                val t  = p.undividedTime.toHours.value
                val tʹ = p.time.toHours.value
                val (gn, gs) = queueCalc.context.site match {
                  case Site.GN => (tʹ, t - tʹ)
                  case Site.GS => (t - tʹ, tʹ)
                }
                println(f"- ${p.id.reference}%-16s  ${p.piName.orEmpty.take(20)}%-20s  $t%5.1f h  $gn%5.1f  $gs%5.1f")
              }
            }


            ExitCode.Success

          }
        }

  }

}


