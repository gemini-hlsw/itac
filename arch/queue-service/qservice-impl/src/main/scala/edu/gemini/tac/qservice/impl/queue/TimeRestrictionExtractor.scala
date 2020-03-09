package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.queues.Queue

import edu.gemini.tac.persistence.restrictedbin.{LgsObservationsRestrictedBin      => PsLgsObservationRestrictedBin}
import edu.gemini.tac.persistence.restrictedbin.{RestrictedBin                     => PsRestrictedBin}
import edu.gemini.tac.persistence.restrictedbin.{WaterVaporPercentageRestrictedBin => PsWaterVaporRestrictedBin}
import edu.gemini.tac.psconversion._

import edu.gemini.tac.qengine.api.config.TimeRestriction
import edu.gemini.tac.qengine.util.{Time, Percent}
import edu.gemini.tac.qengine.util.QEngineUtil._

import scalaz._
import Scalaz._

// Apologies for the convolutedness of this class.  The complexity comes from
// the fact that there are three cases to handle for each time restriction:
//
// - not specified at all which is okay
// - specified correctly which is also okay
// - specified poorly, which is not okay
//
// We need to differentiate the three cases so you will see return types like
// PsError \/ Option[TimeRestriction[T]]

/**
 * Extracts TimeRestriction values from the persistence Queue, separating out
 * relative (Percent valued) restrictions from absolute (Time valued)
 * restrictions.
 */
object TimeRestrictionExtractor {

  // Shorten the type name a bit.
  private type Rel = TimeRestriction[Percent]
  private type Abs = TimeRestriction[Time]

  def BAD_PERCENT_VALUE(p: Percent) = "Relative time restriction percentage must fall in the range 0 to 100, not %f".format(p.value)
  def MISSING_PERCENT_VALUE         = "Relative time restriction missing percentage value"

  def BAD_TIME_VALUE(t: Time)       = "Absolute time restriction must not be negative, not %d".format(t.value.round.toInt)
  def MISSING_TIME_VALUE            = "Absolute time restriction missing value"

  def UNRECOGNIZED_TIME_RESTRICTION(ps: PsRestrictedBin) =
    "Unrecognized time restriction: %s".format(name(ps))

  /**
   * Extracts the name of the restricted bin, substituting in the class name
   * if null.
   */
  private def name(ps: PsRestrictedBin): String = {
    val o = safePersistence { ps.getDescription }.fold(_ => none[String], s => Option(s))
    o | ps.getClass.getName
  }

  /**
   * Extracts the double bin value from the persistence class, unless it is
   * null in which case None is returned.
   */
  private def binValue(ps: PsRestrictedBin): PsError \/ Option[Double] =
    for {
      v <- safePersistence { ps.getValue }
    } yield Option(v).map(_.doubleValue())

  /**
   * Extracts the bin value from the persistence class (if not null) and
   * converts it to a Percent or Time using the provided function.  If null,
   * None is returned.
   */
  private def binValue[T](ps: PsRestrictedBin, f: Double => T): PsError \/ Option[T] =
    for {
      bv <- binValue(ps)
    } yield bv.map(f)

  /**
   * Extracts the bin value from the persistence class and converts it to
   * a Percent wrapped in a Right.  If null or not in the range 0-100, a
   * Left(ConfigError) is returned.
   */
  private def percentBinValue(ps: PsRestrictedBin): PsError \/ Percent =
    binValue(ps, Percent.apply).flatMap {
      case Some(p) if p < Percent.Zero || p > Percent.Hundred => BadData(BAD_PERCENT_VALUE(p)).left[Percent]
      case o => o \/> BadData(MISSING_PERCENT_VALUE)
    }

  /**
   * Converts the persistence class into a relative TimeRestriction, generating
   * an error if not possible.
   */
  private def rel(ps: PsRestrictedBin, f: Percent => Rel): PsError \/ Rel =
    percentBinValue(ps).map(f)

  /**
   * Extracts the bin value from the persistence class and converts it to
   * a Time wrapped in a Right.  If null or negative, a left is returned.
   */
  private def timeBinValue(ps: PsRestrictedBin): PsError \/ Time =
    binValue(ps, i => Time.hours(i)).flatMap {
      case Some(t) if t.ms < 0 => BadData(BAD_TIME_VALUE(t)).left[Time]
      case o => o \/> BadData(MISSING_TIME_VALUE)
    }

  /**
   * Converts the persistence class into an absolute TimeRestriction, generating
   * an error if not possible.
   */
  private def abs(ps: PsRestrictedBin, f: Time => Abs): PsError \/ Abs =
    timeBinValue(ps).map(f)

  /**
   * Converts the persistence class into Pair of options wrapped in a Right
   * where the first member is Some if a relative time restriction and the
   * second member None.  On the other hand, if an absolute time restriction
   * the first member is None and the second Some.  A Left(ConfigError) is
   * returned if there is a problem converting the persistence class.
   */
  private def toTimeRestriction(ps: PsRestrictedBin): PsError \/ (Option[Rel], Option[Abs]) =
    ps match {
      case wv: PsWaterVaporRestrictedBin =>
        rel(ps, TimeRestriction.wv).map(tr => (Some(tr), None))
      case lgs: PsLgsObservationRestrictedBin =>
        abs(ps, TimeRestriction.lgs).map(tr => (None, Some(tr)))
      case _ =>
        BadData(UNRECOGNIZED_TIME_RESTRICTION(ps)).left
    }

  /**
   * Recursive algorithm for converting a list of persistence restricted
   * bins into an Either[ConfigError, (List[Rel], List[Abs])].
   */
  private def timeRestrictions(rem: List[PsRestrictedBin], relList: List[Rel], absList: List[Abs]): PsError \/ (List[Rel], List[Abs]) =
    rem match {
      case Nil => (relList.reverse, absList.reverse).right[PsError]
      case ps :: tail => {
        toTimeRestriction(ps).flatMap {
          case (Some(rel), None) => timeRestrictions(tail, rel :: relList, absList)
          case (None, Some(abs)) => timeRestrictions(tail, relList, abs :: absList)
          case _                 => sys.error("Unexpected return value")
        }
      }
    }

  private def timeRestrictions(lst: List[PsRestrictedBin]): PsError \/ (List[Rel], List[Abs]) =
    timeRestrictions(lst, Nil, Nil)

  def extract(queue: Queue): PsError \/ (List[TimeRestriction[Percent]], List[TimeRestriction[Time]]) =
    for {
      rb <- safePersistence { queue.getRestrictedBins }
      tr <- timeRestrictions(toList(rb))
    } yield tr
}