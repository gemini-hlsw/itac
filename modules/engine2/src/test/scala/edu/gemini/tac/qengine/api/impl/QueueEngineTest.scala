package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.ctx.{Semester, Site, Partner}
import edu.gemini.tac.qengine.api.config.{DecBinGroup, RaBinGroup, SiteSemesterConfig, QueueEngineConfig, ProportionalPartnerSequence}
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import org.junit.{Assert, Test}
import resource.RaResource
import edu.gemini.tac.qengine.p1._
import util.Random
import edu.gemini.tac.qengine.util.{Angle, Percent, Time}

/**
 * Higher-level tests. These are intended to exercise the Queue Engine with a large number of pseudo-random proposals.
 * The hope is that these would flush out any low-level mutations not picked up by other tests
 */
class QueueEngineTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All


  //Deterministic random-number generator
  var random = new Random(0)

  def raBinGroup: RaBinGroup[Time] = {
    val hrs = List(
      Time.hours(20),
      Time.hours(0),
      Time.hours(36),
      Time.hours(88),
      Time.hours(144),
      Time.hours(200),
      Time.hours(254),
      Time.hours(302),
      Time.hours(288),
      Time.hours(212),
      Time.hours(138),
      Time.hours(76)
    )

    RaBinGroup(hrs)
  }

  def queueTime(site: Site): QueueTime = {
    val ptimes = List(
      US -> Time.hours(518.7),
      CA -> Time.hours(180.0),
      CL -> Time.hours(0.0),
      AU -> Time.hours(64.0),
      AR -> Time.hours(31.0),
      BR -> Time.hours(49.7),
      UH -> Time.hours(163.0),
      GS -> Time.hours(108.0)
    )
    QueueTime(site, ptimes.toMap, partners)
  }

  def decBinGroup: DecBinGroup[Percent] = {
    val perc = List(
      Percent(100), // -90, -70
      Percent(100), // -70, -50
      Percent(100), // -50, -30
      Percent(100), // -30, -10
      Percent(100), // -10,  10
      Percent(100), //  10,  30
      Percent(100), //  30,  50
      Percent(100), //  50,  70
      Percent(100)  //  70,  90
    )

    DecBinGroup(perc)
  }

  def semester: Semester = Semester.parse("2012-B")

  def initialPick: Partner = BR

  def coreProposal(ref: String, partner: Partner, ranking: Int, awardedHours: Double, site: Site, os: List[Observation] = List.empty, isPoorWeather: Boolean = false, mode: Mode = Mode.Queue, b3s: List[Observation]) = {
    val ntac = Ntac(partner, ref, ranking, Time.hours(awardedHours))
    val p = CoreProposal(ntac, site, mode, Too.none, os, b3s, isPoorWeather, Some("fella"))
    p
  }

  def randomOs(i: Int, cc: Option[CloudCover] = None): List[Observation] = (for (j <- 1 to i) yield randomO(cc)).toList

  def randomProposals(i: Int): List[Proposal] = {
    random = new Random(0) //Reset -- every time we want the same set
    (for (j <- 1 to i) yield randomProposal(j)).toList
  }

  def randomO(cc: Option[CloudCover] = None): Observation = {
    val ra = randomRA
    val dec = randomDec
    val t = new Target(ra, dec, None)
    val c = randomCondition(cc)
    val time = randomTime
    val lgs = random.nextBoolean()
    new Observation(t, c, time, lgs)
  }

  def randomRA: Angle = {
    val mag = random.nextDouble() * 360
    new Angle(mag, Angle.Deg)
  }

  def randomDec: Angle = {
    val mag = random.nextDouble() * 180 - 90
    new Angle(mag, Angle.Deg)
  }

  def randomCondition(maybeCC: Option[CloudCover] = None): ObsConditions = {
    val cc = maybeCC.getOrElse(CloudCover.values.apply(random.nextInt(4)))
    val iq = ImageQuality.values.apply(random.nextInt(4))
    val sb = SkyBackground.values.apply(random.nextInt(4))
    val wv = WaterVapor.values.apply(random.nextInt(4))

    new ObsConditions(cc, iq, sb, wv)
  }

  def randomTime: Time = {
    val mag = 60 + (random.nextGaussian() * 6) * 10
    mag >= 0 match {
      case true => Time.minutes(mag)
      case false => Time.minutes(60)
    }
  }

  def randomSite(p: Partner): Site =
    if (p.sites.size == 1) p.sites.iterator.next()
    else random.nextBoolean() match {
      case true => Site.north
      case false => Site.south
    }

  def randomPartner: Partner = partners(random.nextInt(partners.size))

  def randomProposal(index: Int): Proposal = {
    val obsCount = Math.abs(5 + random.nextGaussian()).toInt
    val os = randomOs(obsCount)
    val b3Count = Math.abs(3 + random.nextGaussian()).toInt
    val b3s = randomOs(b3Count, Some(CloudCover.CC80))
    val partner = randomPartner
    val rank = random.nextInt(100)
    val awardedTime = os.map(_.time.toHours.value).sum.ceil.toInt
    coreProposal(s"${partner.id}-$index", partner, rank, awardedTime, randomSite(partner), os, b3s = b3s)
  }

  def queueEngineConfig(site: Site, semester: Semester = semester, initialPick: Partner = initialPick): QueueEngineConfig = {
    val binConfig = new SiteSemesterConfig(site, semester, raBinGroup, decBinGroup, List.empty)
    QueueEngineConfig(partners, binConfig, new ProportionalPartnerSequence(partners, site, initialPick))
  }

  def tinyPs: List[CoreProposal] = {
    val t = new Target(randomRA, randomDec, None)
    val tinyO = Observation(t, ObsConditions.AnyConditions, Time.minutes(10), lgs = false)
    val tinyOs = List(tinyO)

    val s0 = coreProposal("S0", GS, 1, 0.25, Site.south, tinyOs, b3s = List.empty)
    val n0 = coreProposal("N0", GS, 1, 0.25, Site.north, tinyOs, b3s = List.empty)
    val s1 = coreProposal("S1", GS, 1, 0.25, Site.south, tinyOs, b3s = List.empty, mode = Mode.Classical)
    List(s0, n0, s1)
  }

  def psWithBothB1B2AndB3Os : List[CoreProposal] = {
    val t = new Target(randomRA, randomDec, None)
    val tinyO = Observation(t, ObsConditions.AnyConditions, Time.minutes(10), lgs = false)
    val tinyOs = List(tinyO)

    val s0 = coreProposal("S0", GS, 1, 0.15, Site.south, tinyOs, b3s = tinyOs)
    val n0 = coreProposal("N0", GS, 1, 0.15, Site.north, tinyOs, b3s = tinyOs)
    val s1 = coreProposal("S1", GS, 1, 0.15, Site.south, tinyOs, b3s = tinyOs, mode = Mode.Classical)
    List(s0, n0, s1)
  }

  @Test
  def filterProposals(): Unit = {
    val config = queueEngineConfig(Site.south)
    val ps = tinyPs
    val (filtered, _) = QueueEngine.filterProposalsAndInitializeBins(ps, config)
    Assert.assertEquals(1, filtered.size)
    Assert.assertEquals(ps.head, filtered.head)
  }

  @Test
  def initializeBins(): Unit = {
    val ps = tinyPs
    val config = queueEngineConfig(Site.south)

    val (_, resourceGroup) = QueueEngine.filterProposalsAndInitializeBins(ps, config)
    val binGroup = resourceGroup.grp
    Assert.assertEquals(12, binGroup.bins.size)
    val bin: RaResource = binGroup.bins.head
    Assert.assertEquals(false, bin.isFull)
    val absBound = bin.absBounds
    //From raBinGroup def
    Assert.assertEquals(20.0, absBound.limit.value, Double.MinPositiveValue)
    Assert.assertEquals(0.0, absBound.used.value, Double.MinPositiveValue)
  }

  @Test
  def calculateBands1And2(): Unit =
    Range(1, 5).foreach { i =>
      val initialPs = randomProposals(100)
      val config = queueEngineConfig(Site.south)
      val (ps, bins) = QueueEngine.filterProposalsAndInitializeBins(initialPs, config)
      //Deterministic seed means random always has same result (I hope)
      Assert.assertEquals(51, ps.size)
      val stage = QueueEngine.fillBands1And2(ps, queueTime(Site.south), config, bins)

      val logs = stage.log.toList
      //Assert.assertEquals(31, logs.size)
      logs.map(l => println(l.msg.toXML))
      val q = stage.queue.toList
      //Assert.assertEquals(18, q.size)
      //Assert.assertEquals("US(欁료숰뽓驴ɚ)", first.id.toString)

      validateInRankOrder(q)
    }

  @Test
  def buildFinalQueue(): Unit = {
    Range(1, 2).foreach {
      i =>
        val initialPs = randomProposals(1000)
        val config = queueEngineConfig(Site.south)
        val q = QueueEngine.calc(initialPs, queueTime(Site.south), config, partners)
        /*
       Not true if B3 rejected from B1/B2 and then accepted
       validateInRankOrder(queue)
        */
        val bandedList = q.queue.bandedQueue

        bandedList.keys.foreach {
          band =>
            val ps = bandedList.get(band)
            println("Band %s has %d proposals".format(band, ps.size))
        }
    }
  }


  @Test
  def band3ProposalsAreNotCounted(): Unit = {
    val initialPs = tinyPs
    val config = queueEngineConfig(Site.south)
    val q = QueueEngine.calc(initialPs, queueTime(Site.south), config, partners)
    val queue = q.queue.toList
    Assert.assertEquals(1, queue.size)
    val proposalMinutes = tinyPs.head.obsList.map(o => o.time.toMinutes.value).sum
    Assert.assertEquals(10.0, proposalMinutes, Double.MinPositiveValue)
    //Given more time than requested because I didn't want to deal with 1/6 of an hour
    Assert.assertEquals(Time.hours(0.25), q.queue.usedTime)
  }

  @Test
  def band12NotRejectedDueToB3(): Unit = {
    val initialPs = psWithBothB1B2AndB3Os
    val config = queueEngineConfig(Site.south)
        val q = QueueEngine.calc(initialPs, queueTime(Site.south), config, partners)
        val queue = q.queue.toList
        Assert.assertEquals(1, queue.size)
        val proposalMinutes = tinyPs.head.obsList.map(o => o.time.toMinutes.value).sum
        Assert.assertEquals(10.0, proposalMinutes, Double.MinPositiveValue)
        //Given more time than requested because I didn't want to deal with 1/6 of an hour
        Assert.assertEquals(Time.hours(0.15), q.queue.usedTime)

  }

  def validateInRankOrder(ps: List[Proposal]) =
    partners.map { partner =>
        ps.filter(p => p.ntac.partner == partner).foldLeft(-1.0)((lastRank, currentProposal) => {
          val myRank = currentProposal.ntac.ranking.num.get
          Assert.assertTrue("%s rank %f found after rank %f".format(partner.toString, myRank, lastRank), myRank >= lastRank)
          myRank
        })
    }
}
