package edu.gemini.tac.qengine.impl

import org.junit._
import util.Random
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.api.config._
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.p1.QueueBand.Category.Guaranteed
import edu.gemini.tac.qengine.util.{Angle, Percent, Time}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.{Semester,Site}
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import edu.gemini.tac.qengine.impl.resource.Fixture

class RandomQueueTest {
  import edu.gemini.tac.qengine.ctx.Partner._
  val partners = all

  val site     = Site.GN
  val semester = new Semester(2011, Semester.Half.A)
  val rand     = new Random(42)

  private def randomTime(max: Int): Time = Time.hours(max * rand.nextDouble)

  private def queueTime: QueueTime =
    Fixture.evenQueueTime(1000, None)
  //   QueueTime(site, partners.map(p => (p, Time.hours(1000) * Percent(p.percentDoubleAt(site)))).toMap, partners)

  // Ra Bins for GN-A
  private def raLimits: RaBinGroup[Time] =
    RaBinGroup(Vector(17, 3, 0, 0, 9, 23, 37, 51, 65, 79, 93, 106, 120, 134, 148, 154, 153, 134, 115, 96, 77, 61, 45, 31)).map(Time.hours(_))

  private def decBins: DecBinGroup[Percent] =
    DecBinGroup.fromBins(
      DecBin(DecRange(-25,  -5), Percent( 60)),
      DecBin(DecRange( -5,  15), Percent( 75)),
      DecBin(DecRange( 15,  35), Percent(100)),
      DecBin(DecRange( 35,  55), Percent(100)),
      DecBin(DecRange( 55,  75), Percent(100)),
      DecBin(DecRange( 75,  90).inclusive, Percent( 65))
    )

  private def gnABinConfig: SiteSemesterConfig = new SiteSemesterConfig(site, semester, raLimits, decBins, List.empty)

  private def randomConditions: ObservingConditions =
    ObservingConditions(
      CloudCover.values(rand.nextInt(CloudCover.values.size)),
      ImageQuality.values(rand.nextInt(ImageQuality.values.size)),
      SkyBackground.values(rand.nextInt(SkyBackground.values.size)),
      WaterVapor.values(rand.nextInt(WaterVapor.values.size)))

  private def randomRa(raHour: Int, plusMinusMin: Double): Angle =
    new Angle(raHour * 60 - plusMinusMin/2 + plusMinusMin * rand.nextDouble, Angle.Min).toHr

  private def randomDec: Angle =
    new Angle(-30 + (120 * rand.nextDouble), Angle.Deg)

  private def randomTarget(raHour: Int, plusMinusMin: Double): Target =
    Target(randomRa(raHour, plusMinusMin), randomDec)


  private def randomObservation(raHour: Int, plusMinusMin: Double): Observation =
    Observation(null, randomTarget(raHour, plusMinusMin), randomConditions, randomTime(1))

  private def randomObsList: List[Observation] = {
    val count  = rand.nextInt(10) + 1
    val raHour = rand.nextInt(24)
    val plusMinusMin = rand.nextDouble * 60
    (for (i <- 0 until count) yield randomObservation(raHour, plusMinusMin)).toList
  }

  private def randomProposal(p: Partner, i: Int): Proposal = {
    val ntac = Ntac(p, p.id + "-" + i, i, randomTime(10))
    val band3 = if (rand.nextInt(2) % 2 == 0) randomObsList.filter(o => o.conditions.cc.percent >= 80) else List.empty
    val poorWeather = rand.nextInt(20) == 0
    Proposal(ntac, site = site, band3Observations = band3, obsList = randomObsList, isPoorWeather = poorWeather)
  }

  private def randomProposals: List[Proposal] =
    for (p <- partners if p.sites.contains(site); i <- 1 until 100) yield randomProposal(p, i)

  private def hrs(time: Time): Double = time.toHours.value

  @Test def testRandomQueue() {
    val binConf = gnABinConfig
    val props   = randomProposals
    val partnerSequence = new PartnerSequence { def sequence: Stream[Partner] = all.toStream #::: sequence }
    val conf    = QueueEngineConfig(partners, binConf, partnerSequence, RolloverReport.empty)

    val startTime  = System.currentTimeMillis
    val calc       = QueueEngine(props, queueTime, conf, partners)
    val endTime    = System.currentTimeMillis

    calc.queue.bandedQueue.foreach(tup => {
      println("--------" + tup._1)
      println(tup._2.map(p => p.id).mkString("\n"))
    })

    /*
    println(calc.log.toList.map(entry =>
      entry.key.id + " ->\t" + (entry.msg match {
        case msg: RejectMessage => msg.reason + msg.detail
        case msg: AcceptMessage => msg.reason + msg.detail
      })).mkString("\n"))
      */

//      tup._1 + " ->\t" + tup._2.reason).mkString("\n")
//    )

    /*
    val m = calc.log.toList.foldLeft(new HashMap[String, Int]()) {
      (logMap, msg) =>
        val c = msg._2.getClass.getName
        logMap + (c -> (logMap.getOrElse(c, 0) + 1))
    }

    println(m.mkString("\n"))
*/
    // println("------------------------ Partner Fairness -----------------------")
    // QueueBand.values.init.foreach(band => {
    //   println("\n--- Band: " + band + " ---")
    //   val bf = calc.queue.partnerFairness(band, partners)
    //   partners.foreach(p => {
    //     println("%s -> rem = %6.2f hrs, total = %7.2f hrs, pecent error = %5.1f%%".format(p, hrs(calc.queue.remainingTime(band, p)), hrs(calc.queue.queueTime(band, p)), bf.errorPercent(p)))
    //   })
    //   println("Total rem = %6.2f hrs, total = %7.2f hrs".format(hrs(calc.queue.remainingTime(band)), hrs(calc.queue.queueTime(band))))
    //   println("Std. Dev. = %5.2f".format(bf.standardDeviation))
    // })

    // println("\n--- Total ---")
    // val f = calc.queue.partnerFairness(Guaranteed, partners)
    // partners.foreach(p => {
    //   println("%s -> rem = %6.2f hrs, total = %7.2f hrs, pecent error = %5.1f%%".format(p, hrs(calc.queue.remainingTime(Guaranteed, p)), hrs(calc.queue.queueTime(Guaranteed, p)), f.errorPercent(p)))
    // })
    // println("Total rem = %6.2f hrs, total = %7.2f hrs".format(hrs(calc.queue.remainingTime(Guaranteed)), hrs(calc.queue.queueTime(Guaranteed))))

    // println("Std. Dev. = %5.2f".format(f.standardDeviation))
    // println("-----------------------------------------------------------------")


    // println("Execution Time = " + Time.millisecs(endTime-startTime).toSeconds)
  }

}
