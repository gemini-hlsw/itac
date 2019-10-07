package edu.gemini.tac.persistence.phase1

import blueprint.BlueprintBase
import sitequality.{WaterVapor, SkyBackground, ImageQuality, CloudCover}
import edu.gemini.tac.exchange.ProposalExporterImpl
import org.springframework.util.FileCopyUtils
import xml.{NodeSeq, Node, Elem, XML}
import org.junit.{Ignore, Test}
import edu.gemini.tac.persistence.Partner
import edu.gemini.model.p1.mutable.{TooOption, Band}
import edu.gemini.tac.persistence.fixtures.builders.{ExchangeProposalBuilder, ClassicalProposalBuilder, ProposalBuilder, QueueProposalBuilder}
import java.text.SimpleDateFormat
import util.matching.Regex
import edu.gemini.tac.util.ProposalSchemaValidator
import java.io.{FileInputStream, FileOutputStream, ByteArrayInputStream, File}
import java.util.Date
import scala.language.postfixOps

class ProposalMigratorTest {

  val dateFormatter = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss")

  val iqs: Map[String, ImageQuality] = Map("20" -> ImageQuality.IQ_20,   "70" -> ImageQuality.IQ_70,   "85" -> ImageQuality.IQ_85,   "Any" -> ImageQuality.IQ_100)
  val sbs: Map[String, SkyBackground] = Map("20" -> SkyBackground.SB_20,  "50" -> SkyBackground.SB_50,  "80" -> SkyBackground.SB_80,  "Any" -> SkyBackground.SB_100)
  val wvs: Map[String, WaterVapor] = Map("20" -> WaterVapor.WV_20,     "50" -> WaterVapor.WV_50,     "80" -> WaterVapor.WV_80,     "Any" -> WaterVapor.WV_100)
  // fixup mapping: 90->80
  val ccs: Map[String, CloudCover] = Map("50" -> CloudCover.CC_50,     "70" -> CloudCover.CC_70,     "80" -> CloudCover.CC_80,     "90" -> CloudCover.CC_80, "Any" -> CloudCover.CC_100)

  @Test
  @Ignore
  def migrateAll() {
//    /Users/fnussber/Downloads/2011A-xml-only/US/qprop005.xml
//    /Users/fnussber/Downloads/2011A-xml-only/US/qprop033.xml
// /Users/fnussber/Downloads/2011A-xml-only/US/qprop034.xml
    val inputDir = new File("/Users/fnussber/Downloads/2011A-xml-only")
    val outputDir = new File("/Users/fnussber/Downloads/migration")
    migrateDirectory(inputDir, outputDir)
  }
  
  def migrateDirectory(inputDir: File, outputDir: File) {
    inputDir listFiles() foreach {
      f => if (f isDirectory) {
        migrateDirectory(f, outputDir)
      } else {
        try {
          if (f.getName.endsWith("xml")) migrateFile(f, outputDir)
        } catch {
          case e: Exception =>
            System.out.println(e.printStackTrace())
            System.out.println("migration failed: " + e)
        }
      }
    }
  }

  @Test
  @Ignore
  def migrateFileTest() {
    val inputFile = new File("/Users/fnussber/Downloads/2011A-xml-only/Subaru/SUBARU_2011A_008.xml")
    val outputDir = new File("/Users/fnussber/Downloads/migration")
    migrateFile(inputFile, outputDir)
  }


  def migrateFile(src: File, targetDir: File) {
    
    System.out.println("ingesting " + src.getAbsoluteFile)

    val proposal = XML loadFile src
    val submissionDetails = collectSubmissionDetails(proposal)
    val partner = getPartner(submissionDetails)
    val tacDetails = collectTac(proposal, partner)

    val builder = createBuilder(proposal, partner)
    builder.setPartner(partner)
    collectObservations(builder, proposal, partner)
    addRequest(builder, submissionDetails)
    addReceipt(builder, submissionDetails)
    addAccept(builder, tacDetails)
    builder setSubmissionKey submissionsKey (proposal)
    builder setTitle title (proposal)
    builder setPiName (proposal\"common"\"investigators"\"pi"\"name"\"first" text, proposal\"common"\"investigators"\"pi"\"name"\"last" text)
    
    val ppp = builder create

    // export this proposal to xml
    val exporter = new ProposalExporterImpl()
    val bytes = exporter.getAsXml(ppp)
    val target = new File(targetDir + File.separator + ppp.getPartner.getAbbreviation + "_" + src.getName)
    FileCopyUtils.copy(new ByteArrayInputStream(bytes), new FileOutputStream(target))

    // re-read exported xml and validate schema
    val validationInput = new FileInputStream(target)
    try {
      ProposalSchemaValidator.validate(validationInput)
    } finally {
      validationInput close()
    }
  }

  def collectObservations(builder: ProposalBuilder, proposal: Elem, partner: Partner): Unit = {
    val targets = collectTargets(proposal)
    val conditions = collectConditions(proposal)
    val blueprints = collectBlueprints(proposal)
    val defaultCondition = getDefaultCondition(proposal, conditions)
    val defaultBlueprint = getDefaultBlueprint(proposal, blueprints)
    val context = new Context(targets, conditions, blueprints, defaultCondition, defaultBlueprint)
    proposal\"observatory"\"obsList"\"observation" foreach (o => addObservation(Band.BAND_1_2, o, builder, context))
    if (!isExchange(proposal) && !isClassical(proposal) && isBand3(proposal)) {
      // the new model supports band3 only for queue proposals (is this correct??)
      addBand3(builder, band3(proposal))
      proposal\"observatory"\"obsList"\"observation" foreach (o => addObservation(Band.BAND_3, o, builder, context))
    }
  }

  def collectTargets(proposal: Elem): Map[String, Target] =
    // Note: we are only interested in science targets and throw away all other targets (guide stars)
    // this makes it easier to identify the science target that belongs to an observation in a later step
    proposal\"common"\"targetCatalog"\"target" filter (t => (t\"@type" text) equals "science") map (t => (t\"@id" text, toTarget(t, isToo(proposal)))) toMap

  def toTarget (t: Node, isToo: Boolean): Target = {
    val name = t\"targetName" text
    if (isToo) {
      ProposalBuilder.createTooTarget(name)           // for too targets we don't care about the actual coordinates (they're always 0 in the old model)
    } else if ((t\"nonSidSystem" length) > 0) {
      val c1 = fixUpRaDec(t \ "nonSidSystem" \ "c1" text) // fixup values
      val c2 = fixUpRaDec(t \ "nonSidSystem" \ "c2" text) // fixup values
      val dateString = t\"nonSidSystem"\"taizDate" text
      val timeString = t\"nonSidSystem"\"taizTime" text
      val date = dateFormatter.parse(dateString + timeString)
      ProposalBuilder.createNonSiderealTarget(name, c1, c2, date)
    } else if ((t\"hmsdegSystem" length) > 0) {
      val c1 = fixUpRaDec(t \ "hmsdegSystem" \ "c1" text) // fixup values
      val c2 = fixUpRaDec(t \ "hmsdegSystem" \ "c2" text) // fixup values
      ProposalBuilder.createSiderealTarget(name, c1, c2)
    } else {
      // unknown/not yet implemented coordinate type
      throw new IllegalArgumentException("not a non-sideral or hmsdeg target")
    }
  }

  // for whatever reason there are quite a few prpoosals in the dataset with weird ra/dec coordinates
  // this method tries to fix all of them up in a way that they can be propeorly parsed and converted
  // into targets
  def fixUpRaDec(raDec: String): String = {
    val raDecTrimmed = raDec.trim.replace(" ", ":")
    val shortRaDec = new Regex("""([+-]?\d\d)""")
    raDecTrimmed match {
      case shortRaDec(a)                      => a + ":0:0.0"   // 01, -01, +01 => +-01:0:0.0
      case ""                                 => "0:0:0.0"
      case "0"                                => "0:0:0.0"
      case "00"                               => "0:0:0.0"
      case "00:00:00"                         => "0:0:0.0"
      case "0:00:13.337"                      => "0:0:13.337"
      case "0:00:45.32"                       => "0:0:45.32"
      case "12:45"                            => "12:45:0.0"
      case "-2:45"                            => "-2:45:0.0"
      case "06:32:154.0"                      => "06:32:15.4"
      case "59:11:967.0"                      => "59:11:96.7"
      case "61:22:589.0"                      => "61:22:58.9"
      case "09:13:593.0"                      => "09:13:59.3"
      case "43:11:789.0"                      => "43:11:78.9"
      case "49:25:639.0"                      => "49:25:63.9"
      case raDec if { raDec endsWith "00" }  => raDec substring(0, (raDec length) - 1)
      case raDec if { raDec endsWith "." }   => raDec + "0"
      case raDec                              => raDec
    }
  }

  def collectConditions(proposal: Elem): Map[String, Condition] =
    proposal\"observatory"\"constraint" map (c => (c\"@id" text, toCondition(c))) toMap

  def toCondition(n: Node): Condition = {
    val constraint = n\"geminiObsConditionsConstraint"
    val cc = ccs get (constraint\"@cloudCover" text)     orNull // error Handling
    val iq = iqs get (constraint\"@imageQuality" text)   orNull // error Handling
    val sb = sbs get (constraint\"@skyBackground" text)  orNull // error Handling
    val wv = wvs get (constraint\"@waterVapor" text)     orNull // error Handling
    ; // neat, don't think this is not needed here...
    ProposalBuilder.createCondition(cc, iq, sb, wv)
  }
  def getDefaultCondition(proposal: Elem, conditions: Map[String, Condition]): Condition =
    conditions get (proposal\"observatory"\"obsList"\"constraintRef"\"@constraintId" text) orNull // error handling

  def collectBlueprints(proposal: Elem): Map[String, BlueprintBase] = {
    val resourceCategory = proposal\"observatory"\"resourceList"\"resourceCategory"
    val site = resourceCategory\"resourceType" text // no mixed proposals either all north or all south
    ; // funny: Scala insists on having a semicolon here (?)
    resourceCategory\"resource" map (r => (r\"@id" text, toBlueprint(site, r))) toMap
  }

  def toTimeAmount(n: Node): TimeAmount = {
    val unit = n\"@units" text match {
      case "hours"    => TimeUnit.HR
      case "minutes"  => TimeUnit.MIN
      case "nights"   => TimeUnit.NIGHT
    }
    val amount = java.lang.Double parseDouble (n text)
    new TimeAmount(amount, unit)
  }
  
  def toRanking(r: String): Int = {
    r match {
      case ""   => 0
      case x    => java.lang.Integer parseInt x
    }
  }


  def toBlueprint(site: String,  r: Node): BlueprintBase = {
    site match {
      case "Gemini North"   =>
        val laser = (r\"resourceComp" filter (rc => (rc\"resourceCompName" text) contains "Laser guide star" )) length
        val hasLaser = if (laser > 0) true else false
        if (hasLaser) ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS else ProposalBuilder.GMOS_N_IMAGING
      case "Gemini South"   => ProposalBuilder.GMOS_S_LONGSLIT
      case "Keck"     => ProposalBuilder.KECK
      case "Subaru"   => ProposalBuilder.SUBARU
    }
  }

  def getDefaultBlueprint(proposal: Elem, blueprints: Map[String, BlueprintBase]): BlueprintBase =
    blueprints get (proposal\"observatory"\"obsList"\"resourceRef"\"@resourceId" text) orNull // error handling


  def addObservation (band: Band, o: Node, builder: ProposalBuilder, ctx: Context): ProposalBuilder = {
      val blueprint = ctx.blueprints get (o\"blueprintRef"\"@blueprintId" text) orElse Option(ctx.defaultBlueprint) // howto do this without option??
      val condition = ctx.conditions get (o\"conditionRef"\"@conditionId" text) orElse Option(ctx.defaultCondition) // howto do this without option??
      val target = scienceTarget(o\"targetRef", ctx.targets) orNull // error handling!
      val time = new TimeAmount(1, TimeUnit.HR)
      builder.addObservation(band, blueprint get, condition get, target, time)
  }
  
  def scienceTarget(refs: NodeSeq, targets: Map[String, Target]): Option[Target] = {
    val key = for (r <- refs if targets contains (r\"@targetId" text)) yield r \"@targetId" text;
    targets get key.head
  }

  def submissionsKey(proposal: Elem): String =
    (proposal\"@proposalKey" text) toLowerCase

  def title(proposal: Elem): String =
    proposal\"common"\"title" text

  def collectSubmissionDetails(proposal: Elem): Node =
    // return head -> error checking in case more than one submission flag
    (proposal\"observatory"\"extension"\"geminiSubDetailsExtension"\"submissions"\"partnerSubmission") filter (n => (n\"@submissionFlag" text) equals "true") head

  def collectTac(proposal: Elem, partner: Partner): Node =
    // return head -> error checking in case more than one tac with this partner
    (proposal\"observatory"\"extension"\"geminiTACExtension") filter (n => (n\"@partnerName" text) equals (partner getName) ) head

  def band3(proposal: Elem): Node =
  // return head -> error checking in case more than one tac with this partner
    proposal\"observatory"\"extension"\"geminiBand3Extension" head

  def getPartner(submission: Node) : Partner = {
      submission\"@partnerName" text match {
      case "Argentina"  => ProposalBuilder.dummyPartnerAR
      case "Australia"  => ProposalBuilder.dummyPartnerAU
      case "Brazil"     => ProposalBuilder.dummyPartnerBR
      case "Canada"     => ProposalBuilder.dummyPartnerCA
      case "Chile"      => ProposalBuilder.dummyPartnerCL
      case "KR"         => ProposalBuilder.dummyPartnerKR
      case "UH"         => ProposalBuilder.dummyPartnerUH
      case "USA"        => ProposalBuilder.dummyPartnerUS
      case "Subaru"     => ProposalBuilder.dummyPartnerSubaru
      case "Keck"       => ProposalBuilder.dummyPartnerKeck
    }
  }

  def addRequest(builder: ProposalBuilder,  sub: Node): ProposalBuilder =
    builder setRequest (toTimeAmount(sub\"submissionMinimumRequestedTime" head), toTimeAmount(sub\"submissionRequestedTime" head))

  def addReceipt(builder: ProposalBuilder, sub: Node): ProposalBuilder =
    builder setReceipt (sub\"partnerReferenceNumber" text, new Date())
  
  def addAccept(builder: ProposalBuilder,  tac: Node): ProposalBuilder =
    builder setAccept (toTimeAmount(tac\"partnerMinimumTime" head), toTimeAmount(tac\"partnerRecommendedTime" head), toRanking(tac\"partnerRanking" text), (tac\"@poorWeaterFlag" text) equals "true")

  def isBand3(proposal: Elem): Boolean =
    (proposal\"observatory"\"extension"\"geminiBand3Extension"\"@usableInBand3" text) equals "true"

  def addBand3(builder: ProposalBuilder,  band3: Node): ProposalBuilder =
    builder setBand3Request (toTimeAmount(band3\"minimumUsableBand3Time" head), toTimeAmount(band3\"requestedBand3Time" head))

  def isClassical(proposal: Elem): Boolean =
    (proposal\"observatory"\"observingMode"\"@mode" text) equals "classical"
  
  def isExchange(proposal: Elem): Boolean =
    isExchangeForSubaru(proposal) || isExchangeForKeck(proposal)
  
  def isExchangeForSubaru(proposal: Elem): Boolean =
    (proposal\"observatory"\"resourceList"\"resourceCategory"\"resourceType" text) equals "Subaru"

  def isExchangeForKeck(proposal: Elem): Boolean =
    (proposal\"observatory"\"resourceList"\"resourceCategory"\"resourceType" text) equals "Keck"

  def isToo(proposal: Elem): Boolean =
    proposal\"observatory"\"tooTrigger"\"@value" text match {
      case "no"       => false
      case "rapid"    => true
      case "standard" => true
    }

  def createBuilder(proposal: Elem, partner: Partner): ProposalBuilder = {
    // exchange for Subaru/Keck takes precedence over everything else (classical/queue/too etc)
    if (isExchange(proposal)) {
      new ExchangeProposalBuilder()
    } else {
      proposal\"observatory"\"observingMode"\"@mode" text match {
        case "queue" =>
          proposal\"observatory"\"tooTrigger"\"@value" text match {
            case "no"       => new QueueProposalBuilder(TooOption.NONE)
            case "rapid"    => new QueueProposalBuilder(TooOption.RAPID)
            case "standard" => new QueueProposalBuilder(TooOption.STANDARD)
          }
        case "classical" => new ClassicalProposalBuilder()
      }
    }
  }


  class Context(
    val targets: Map[String, Target],
    val conditions: Map[String, Condition],
    val blueprints: Map[String, BlueprintBase],
    val defaultCondition: Condition,
    val defaultBlueprint: BlueprintBase) {
  }
}
