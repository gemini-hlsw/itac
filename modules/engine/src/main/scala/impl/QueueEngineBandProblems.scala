package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.p1._
import scalaz._, Scalaz._

object QueueEngineBandProblems {
  import QueueBand._

  /** A function type that is define only in cases of failure. */
  type Problem = PartialFunction[(Proposal, QueueBand), String]

  val ClassicalNotInBand1: Problem = {
    case (p, b@(QBand2 | QBand3 | QBand4)) if p.mode == Mode.Classical =>
      s"Classical proposal is not allowed in band ${b.number}"
  }

  val NoObsInBand: Problem = {
    case (p, b) if p.obsListFor(b).isEmpty =>
      s"No observations were found for Band ${b.number}"
  }

  val LpInBand3Or4: Problem = {
    case (p, b@(QBand3 | QBand4)) if p.mode == Mode.LargeProgram =>
      s"LP proposal not allowed in Band ${b.number}"
  }

  val RapidTooOutsideBand1: Problem = {
    case (p, b@(QBand2 | QBand3 | QBand4)) if p.too == Too.rapid =>
      s"Rapid TOO proposal not allowed in Band ${b.number}"
  }

  val All: List[Problem] =
    List(ClassicalNotInBand1, NoObsInBand, LpInBand3Or4, RapidTooOutsideBand1)

  def checkAll(p: Proposal, b: QueueBand): ValidationNel[String, Unit] =
    All.foldMap(problem => problem.lift((p, b)).toFailureNel(()))

  def unsafeCheckAll(p: Proposal, b: QueueBand): Unit =
    checkAll(p, b) match {
      case Success(u)  => u
      case Failure(es) =>
        throw new RuntimeException(
          s"""|Proposal ${p.ntac.reference} is improperly categorized in Band ${b.number}:
              |  ${es.intercalate("\n  ")}
              |""".stripMargin
        )
    }

}