package edu.gemini.tac.qengine.api.config

import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.ctx.Partner
import xml.Elem
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProportionalPartnerSequence(seq: List[Partner], val site: Site, val initialPick: Partner) extends  edu.gemini.tac.qengine.api.config.PartnerSequence {
  def this(seq : List[Partner], site : Site) = this(seq, site, seq.sortWith(_.percentDoubleAt(site) > _.percentDoubleAt(site)).head)

  private val LOGGER : Logger = LoggerFactory.getLogger("edu.gemini.itac")

  private def filter(site: Site) = seq.filter(_.sites.contains(site))

  private val gnseq = filter(Site.GN)
  private val gsseq = filter(Site.GS)

  private def siteSeq(site: Site): List[Partner] =
    site match {
      case Site.GN => gnseq
      case Site.GS => gsseq
    }

  //Confirm OK initial pick
  if (!siteSeq(site).contains(initialPick)) {
    throw new IllegalArgumentException("Incompatible PartnerSequence for Site %s starting with Partner %s".format(site.displayName, initialPick.fullName))
  }

  /**
  Returns the key whose achieved proportions are most below desired proportions
   */
  private def next[T](proportions: Map[T, Double], achievedToDate: Map[T, Double]): T = {
    val proportionsSum = proportions.values.sum
    val desiredPercentages = proportions.mapValues(v => v / proportionsSum)
    //Initially no achieved percentages, so avoid / 0
    val toDateTotal = if (achievedToDate.values.sum == 0.0) {
      1
    } else {
      achievedToDate.values.sum
    }
    val achievedPercentages = achievedToDate.mapValues(v => v / toDateTotal).toMap
    LOGGER.debug("Desired percentages:" + desiredPercentages)
    LOGGER.debug("Achieved percentages: " + achievedPercentages)
    val gaps = achievedPercentages.map {
      case (k, v) => k -> (desiredPercentages(k) - v)
    }
    val maxUnder = gaps.values.toList.sortWith(_ > _).head
    LOGGER.debug(maxUnder.toString)
    //Now find the key whose current gap corresponds to biggest under-served element
    val gapsForMaxUnder = gaps.mapValues {
      v => Math.abs(v - maxUnder) < Double.MinPositiveValue
    } .toMap
    val keysByHasMaxUnder =
      maxUnder < Double.MinPositiveValue match {
        case false => gapsForMaxUnder.map(_.swap)
        //Special case for cycle + some partner has 0.0 percentage (e.g., Keck)
        case true => {
          val maxPartnerPercentage = proportions.values.max
          Map(true->proportions.find(_._2 == maxPartnerPercentage).get._1)
        }
      }

    LOGGER.debug("Served by " + keysByHasMaxUnder(true))
    keysByHasMaxUnder(true)
  }

  /**
  Stream of most-fair next element
   */
  private def proportionalStream[T](proportions: Map[T, Double], toDate: Map[T, Double]): LazyList[T] = {
    val nextS = next(proportions, toDate)
    val tailToDate = toDate + (nextS -> (toDate(nextS) + 1.0))
    LazyList.cons(
      nextS,
      proportionalStream(proportions, tailToDate)
    )
  }

  def sequence: LazyList[Partner] = {
    val partnersForSite = siteSeq(site)
    val proportions = partnersForSite.map(p => p -> p.share.doubleValue).toMap
    val none = proportions.mapValues(_ => 0.0).toMap
    proportionalStream(proportions, none).dropWhile(p => p != initialPick)
  }

  def configuration: Elem = <ProportionalPartnerSequence name="ProportionalPartnerSequence" initialPick={initialPick.id}>
    {seq.map(_.toXML)}
  </ProportionalPartnerSequence>

  override def toString = sequence.take(100).toList.mkString(",")
}