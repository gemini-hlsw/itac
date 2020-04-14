package edu.gemini.tac.qengine.api.queue

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner

class PartnerFairness(remainingTime: Partner => Time, availableTime: Partner => Time, partners: List[Partner]) {

  private def hours(time: Time): Double = time.toHours.value

  private def calcErrorPercent(p: Partner): Double =
    hours(availableTime(p)) match {
      case 0.0   => 0.0
      case avail => - hours(remainingTime(p))/avail * 100
    }

  /**
   * A map from Partner to the percentage of error from the specified partner
   * allocation.  Negative numbers indicate under allocation and positive
   * numbers over allocation.
   */
  val errorPercent: Map[Partner, Double] =
    partners.map(p => p -> calcErrorPercent(p)).toMap

  private def avg(f: Partner => Double): Double =
    (0.0/:partners) { _ + f(_) } / partners.length

  val minErrorPercent: Double  = errorPercent.values.min
  val maxErrorPercent: Double  = errorPercent.values.min
  val meanErrorPercent: Double = avg(errorPercent)

  val medianErrorPercent: Double = errorPercent.values.toList.sorted match {
    case lst if lst.size % 2 == 1 => lst(lst.size / 2)
    case lst => {
      val second = lst.size / 2
      val first  = second - 1
      (lst(first) + lst(second))/2
    }
  }

  private def squareDiffFromMean(p: Partner): Double =
    scala.math.pow(errorPercent(p) - meanErrorPercent, 2.0)

  /**
   * The standard deviation of all the remaining time percentages.  The lower
   * the standard deviation, the more fair the proposal queue.
   */
  val standardDeviation: Double = scala.math.sqrt(avg(squareDiffFromMean))
}