package edu.gemini.tac.qengine.p1

import edu.gemini.tac.qengine.util.{Percent, CompoundOrdering, Time}
import edu.gemini.tac.qengine.p1.Ntac.Rank
import edu.gemini.tac.qengine.ctx.{Share, Partner}


case class Ntac(partner: Partner, reference: String, ranking: Rank, awardedTime: Time, poorWeather: Boolean = false, lead: Option[String] = None,  comment: Option[String] = None) extends Ordered[Ntac] {
  require(awardedTime.ms >= 0, "Awarded time must be non-negative, not " + awardedTime.ms)

  def compare(that: Ntac): Int = Ntac.MasterOrdering.compare(this, that)
}

object Ntac {

  case class Rank(num: Option[Double]) extends Ordered[Rank] {
    require(num.forall(_ >= 0), "Ranking must be non-negative, not " + num.get)

    def compare(that: Rank): Int = {
      (num, that.num) match {
        case (None, None) =>  0
        case (_, None)    => -1
        case (None, _)    =>  1
        case (Some(n0), Some(n1)) => n0.compare(n1)
      }
    }

    def format: String = {
      num map { n =>
        val str = "%.1f".format(n)
        if (str.endsWith(".0")) str.dropRight(2) else str
      } getOrElse ""
    }

    override def toString: String = format
  }

  object Rank {
    val empty: Rank = Rank(None)

    def apply(num: Double): Rank = new Rank(Some(num))
  }

  /**
   * An ordering based upon awarded time (descending) followed by partner
   * percentage (ascending).  This is the default ordering for selecting
   * master proposals.
   */
  object MasterOrdering extends CompoundOrdering(
    Ordering.by[Ntac, Time](_.awardedTime).reverse,
    Ordering.by[Ntac, Percent](_.partner.share),
    Ordering.by[Ntac, Rank](_.ranking),
    Ordering.by[Ntac, String](_.partner.id),
    Ordering.by[Ntac, String](_.reference)
  )

  /**
   * Sums the awarded time in a collection of Ntacs.
   */
  def awardedTimeSum(ntacs: Iterable[Ntac]): Time =
    (Time.ZeroHours/:ntacs)(_ + _.awardedTime)

  def apply(partner: Partner, reference: String, ranking: Double, awardedTime: Time, poorWeather : Boolean): Ntac =
      new Ntac(partner, reference, Ntac.Rank(ranking), awardedTime, poorWeather, lead = None)


  def apply(partner: Partner, reference: String, ranking: Double, awardedTime: Time): Ntac =
    new Ntac(partner, reference, Ntac.Rank(ranking), awardedTime, poorWeather = false, lead = None)

  def apply(partner: Partner, reference: String, ranking: Double, awardedTime: Time, lead: String): Ntac =
    new Ntac(partner, reference, Ntac.Rank(ranking), awardedTime, poorWeather = false, Some(lead))
}