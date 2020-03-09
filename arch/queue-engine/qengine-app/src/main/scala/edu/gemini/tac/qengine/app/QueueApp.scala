/*
Commented out. This is no longer in sync with design and maintaining its compilability
is ultimately counter-productive. If this needs to be revived, better to go back to the
early 2012 version and try to re-invent the intent.

Leaving the file as a placeholder for now.
 */

//package edu.gemini.tac.qengine.app
//
//import edu.gemini.tac.qengine.impl.QueueEngine
//import edu.gemini.qengine.skycalc.{DecBinSize, RaBinSize, RaDecBinCalc}
//import edu.gemini.tac.qengine.util.{Percent, Time}
//import edu.gemini.tac.qengine.api.config.{QueueEngineConfig, DecBinGroup, RaBinGroup, SiteSemesterConfig}
//import edu.gemini.tac.qengine.api.queue.time.{QueueTime, PartnerTime}
//import edu.gemini.tac.qengine.ctx.{Context, Semester, Site, Partner}
//import edu.gemini.tac.qengine.log.{ProposalDetailMessage, ProposalLog, LogMessage}
//
//import scala.collection.JavaConversions._
//import edu.gemini.tac.service.HibernateSource
//import edu.gemini.tac.qengine.p1.{Proposal}
//import java.util.Properties
//import java.io.{IOException, InputStream, File}
//import edu.gemini.tac.qengine.api.queue.ProposalQueue
//import edu.gemini.tac.qengine.api.QueueCalc
//import edu.gemini.tac.qengine.configuration.impl.HardCodedConfiguration
//import edu.gemini.tac.qengine.p1io.{NtacIo, PartnerIo, P1IoError, ProposalIo}
//
///**
// * A command line application for exercising the Queue Engine given a
// * list of proposal files or directories containing proposal files.
// *
// * <p>A tool for exercising the queue engine without going through the web
// * app.
// */
//object QueueApp {
//  val partners = HardCodedConfiguration.partners
//
//  val proposalIo = new ProposalIo(new NtacIo(new PartnerIo(partners)))
//
//  case class LoadResults(collection: ProposalCollection, loadTime: Time)
//
//  def load(committeeName: String): LoadResults = {
//    val startRead = System.currentTimeMillis
//    val proposals: List[edu.gemini.tac.persistence.Proposal] = HibernateSource.proposals(committeeName)
//    val endRead = System.currentTimeMillis
//    val eitherParsedOrErrors: List[Either[P1IoError, ParsedProposal]] = proposals.map {
//      p =>
//        val key = p.getPhaseIProposal.getPrimary.getReceipt.getReceiptId
//        val eitherProposalOrError = proposalIo.toProposal(p)
//        Either.cond(eitherProposalOrError.isRight, new ParsedProposal(eitherProposalOrError.right.get, key, p.getPhaseIProposal), eitherProposalOrError.left.get)
//    }
//    val errs = for (Left(err) <- eitherParsedOrErrors) yield err
//    val ps = for (Right(p) <- eitherParsedOrErrors) yield p
//
//
//    val proposalCollection = new ProposalCollection(errs, ps)
//
//    LoadResults(proposalCollection, Time.millisecs(endRead - startRead))
//  }
//
//  private def reportProposals(res: LoadResults) {
//    //res.collection.errors.foreach(e => println(e.reason))
//    println("Errors")
//    println("------")
//    res.collection.errors.groupBy(_.reason).map {
//      case (k, v) => println(k + ": " + v.size)
//    }
//    println("------")
//
//    val n = res.collection.errors.size + res.collection.parsed.size
//    println("Processed %d proposals in %s".format(n, res.loadTime.toSeconds))
//    println("%d good, %d errors".format(res.collection.parsed.size, res.collection.errors.size))
//  }
//
//  private def loadProperties(fileName: String): Map[String, String] = {
//    val properties: Properties = new Properties
//    val is: InputStream = getClass.getResourceAsStream(fileName)
//    try {
//      properties.load(is)
//      is.close
//    }
//    catch {
//      case e: IOException => {
//        throw new RuntimeException(e)
//      }
//    }
//    properties.keys.toSet.asInstanceOf[Set[String]].map(k => (k, properties.get(k).asInstanceOf[String])).toMap
//  }
//
//  private def hasRequiredProperties(keys: Set[String]): Boolean = {
//    val requiredKeys = Seq("committeeName", "site", "semester", "raBinSize", "decBinSize")
//    if (false == keys.containsAll(requiredKeys)) {
//      requiredKeys.map(s =>
//        keys.contains(s) match {
//          case false => println("Missing required property: " + s)
//          case true => //OK
//        }
//      )
//      false
//    } else {
//      true
//    }
//  }
//
//  private def msg2String(msg: LogMessage): String = msg match {
//    case det: ProposalDetailMessage => "%-26s - %s".format(det.reason, det.detail)
//    case _ => msg.getClass.getName
//  }
//
//
//  def main(args: Array[String]) {
//
//    val properties = loadProperties("/queue_app.properties")
//    if (false == hasRequiredProperties(properties.keySet.toSet)) {
//      throw new RuntimeException("Missing required properties")
//    }
//    properties.get("rollover").get match {
//      case "PartnerTime.empty" => PartnerTime.empty(Nil)
//      case _ => throw new RuntimeException("Don't know how to handle rollover " + properties.get("rollover"))
//    }
//
//    val loadResults = load(properties.get("committeeName").get)
//    val site = Site.parse(properties.get("site").get)
//    val semester = Semester.parse(properties.get("semester").get)
//    val initialPick = partners.parse(properties.get("initialPick").get) match {
//      case Some(p) => p
//      case None => throw new RuntimeException("Could not parse initialPick value" + properties.get("initialPick"))
//    }
//    //    val raBinSize = new RaBinSize(properties.get("raBinSize").get.toInt)
//    //    val decBinSize = new DecBinSize(properties.get("decBinSize").get.toInt)
//    //    val raDec = RaDecBinCalc.get(site, semester, raBinSize, decBinSize)
//    val qTime = queueTime(site)
//
//    //Report configuration stuff
//    reportProposals(loadResults)
//    partners.values.foreach {
//      p => println(p + "\t" + qTime(p) + "\t" + qTime.quantum(p))
//    }
//    println("total = " + (Time.ZeroHours /: partners.values.map(p => qTime(p)))(_ + _))
//
//    val startCalc = System.currentTimeMillis
//    val calc = buildQueue(loadResults.collection.props, site, semester, initialPick, raBinGroup, decBinGroup, qTime)
//    val endCalc = System.currentTimeMillis
//    val calcTime = Time.millisecs(endCalc - startCalc)
//
//    report(calc, loadResults.collection, new Context(site, semester), calcTime)
//  }
//
//  def buildQueue(proposals: List[Proposal], site: Site, semester: Semester, initialPick: Partner, raBins: RaBinGroup[Time], decBins: DecBinGroup[Percent], qTime: QueueTime): QueueCalc = {
//    // Set up the configuration, calculating queue time and ra / dec bins ...
//    println("After joint proposal merging, there are " + proposals.size + " proposals being considered")
//    val binConfig = new SiteSemesterConfig(site, semester, raBins, decBins)
//    // Execute the algorithm.
//    throw new RuntimeException("Must reimplement some kind of hard-coded partner sequence")
//    /*
//    val hardCodedPartnerSequence : PartnerSequence <-- TODO: Implement
//    val calc = QueueEngine(proposals, qTime, QueueEngineConfig(binConfig, initialPick, hardCodedPartnerSequence(initialPick)))
//    calc
//    */
//  }
//
//  def raBinGroup: RaBinGroup[Time] = {
//    val hrs = List(
//      Time.hours(20),
//      Time.hours(0),
//      Time.hours(36),
//      Time.hours(88),
//      Time.hours(144),
//      Time.hours(200),
//      Time.hours(254),
//      Time.hours(302),
//      Time.hours(288),
//      Time.hours(212),
//      Time.hours(138),
//      Time.hours(76)
//    )
//
//    RaBinGroup(hrs)
//  }
//
//  def queueTime(site: Site): QueueTime = {
//    val US = partners.parse("US").get
//    val UH = partners.parse("UH").get
//    val BR = partners.parse("BR").get
//    val CA = partners.parse("CA").get
//    val UK = partners.parse("UK").get
//    val CL = partners.parse("CL").get
//    val AU = partners.parse("AU").get
//    val GS = partners.parse("GS").get
//    val AR = partners.parse("AR").get
//
//    // For now, explicitly setting partner times to match the GN spreadsheet...
//    val ptimes = List(
//      (US -> Time.hours(518.7)),
//      (UK -> Time.hours(317.7)),
//      (CA -> Time.hours(180.0)),
//      (CL -> Time.hours(0.0)),
//      (AU -> Time.hours(64.0)),
//      (AR -> Time.hours(31.0)),
//      (BR -> Time.hours(49.7)),
//      (UH -> Time.hours(163.0)),
//      (GS -> Time.hours(108.0))
//    )
//    val qtime = new QueueTime(site, PartnerTime(ptimes: _*))
//    qtime
//  }
//
//  def decBinGroup: DecBinGroup[Percent] = {
//    val perc = List(
//      Percent(0), // -90, -70
//      Percent(0), // -70, -50
//      Percent(0), // -50, -30
//      Percent(60), // -30, -10
//      Percent(75), // -10,  10
//      Percent(100), //  10,  30
//      Percent(100), //  30,  50
//      Percent(100), //  50,  70
//      Percent(65) //  70,  90
//    )
//
//    DecBinGroup(perc)
//  }
//
//  private def report(calc: QueueCalc, proposalCollection: ProposalCollection, context: Context, calcTime: Time): Unit = {
//
//    // Report the results in insertion order.
//    println("*** Insertion Order Log")
//    println(calc.proposalLog.toList.map {
//      case ProposalLog.Entry(key, msg) => "%-25s -> %s".format(key.id, msg2String(msg))
//    } mkString ("\n"))
//    println()
//
//
//    println("*** Proposal Key Order Log")
//    println(calc.proposalLog.toList.sorted(ProposalLog.KeyOrdering).map {
//      case ProposalLog.Entry(key, msg) => "%-25s -> %s".format(key.id, msg2String(msg))
//    } mkString ("\n"))
//    println()
//
//    println("*** Queue")
//
//    // Show the generated queue.
//    val report = QueueReport(calc.queue, proposalCollection, context)
//    println(report.lines.mkString("\n"))
//    println()
//
//    //    //Save the queue if asked to do so
//    //    def saveQueue(q : ProposalQueue){
//    //
//    //    }
//    //
//    //    for{
//    //      p : Option[String] <- properties.get("saveQueueToDatabase");
//    //      b = p.get
//    //    } yield b.toBoolean match {
//    //      case true => saveQueue(calc.queue)
//    //      case _ => //None or false
//    //    }
//    println(calc.proposalLog.toXML)
//
//
//
//    println("Queue calculated in %s, considered %d proposals.".format(calcTime.toSeconds, calc.queue.toList.size + calc.proposalLog.proposalIds.size))
//
//    println("\tUsed time.....: %6.1f hrs".format(calc.queue.usedTime.toHours.value))
//    println("\tRemaining time: %6.1f hrs".format(calc.queue.remainingTime.toHours.value))
//    println("\tTotal time....: %6.1f hrs".format(calc.queue.queueTime.full.toHours.value))
//  }
//
//}