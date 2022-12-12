// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.implicits._
import itac._
import java.io.File
import edu.gemini.spModel.core.ProgramId
import cats._
import cats.effect._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import edu.gemini.model.p1.mutable._
import java.math.RoundingMode
import edu.gemini.model.p1.mutable.NgoPartner.US
import edu.gemini.model.p1.mutable.NgoPartner.CA
import edu.gemini.model.p1.mutable.NgoPartner.UH
import edu.gemini.model.p1.mutable.NgoPartner.KR
import edu.gemini.model.p1.mutable.NgoPartner.BR
import edu.gemini.model.p1.mutable.NgoPartner.AR
import edu.gemini.model.p1.mutable.NgoPartner.CL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.davidmoten.text.utils.WordWrap
import itac.config.Common
import itac.util.ProgIdHash
import java.nio.file.Files
import edu.gemini.tac.qengine.p1

object EmailGen {

  implicit class ProposalOps(p: Proposal) {

    def getActualProposalClass: ProposalClass = {
      val pcc = p.getProposalClass
      (
        none[ProposalClass]           <+>
        Option(pcc.getClassical)      <+>
        Option(pcc.getExchange)       <+>
        Option(pcc.getFastTurnaround) <+>
        Option(pcc.getLarge)          <+>
        Option(pcc.getQueue)          <+>
        Option(pcc.getSip)            <+>
        Option(pcc.getSpecial)
      ).getOrElse(sys.error("No proposal class."))
    }

    def getItac: Itac =
      getActualProposalClass.getItac

    def getItacAccept: ItacAccept =
      getItac.getAccept

  }

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
    progids:        List[ProgramId]
  ): Operation[F] =
    new AbstractExportOperation[F](qe, siteConfig, rolloverReport) {

      def export(p: Proposal, pdfs: p1.Proposal.Pdfs[File], pid: ProgramId, cc: Common, pih: ProgIdHash): Unit = {

        // If we gave an explicit list of progids, make sure pid is in it
        if (progids.nonEmpty && !progids.contains(pid))
          return; //

        val (prog, part) =
          ProgramPartnerTimeMutable.programAndPartnerTime(p)

        val (sub, body) =
          emailSubjectAndBody(
            deadline =            cc.emailConfig.deadline,
            instructionsUrl =     cc.emailConfig.instructionsURL,
            semester =            p.getSemester,
            progTitle =           p.getTitle,
            piName =              p.getInvestigators.getPi,
            progId =              p.getItacAccept.getProgramId,
            timeAwarded =         p.getItacAccept.getAward,
            programTime =         prog,
            partnerTime =         part,
            queueBand =           p.getItacAccept.getBand,
            country =             PrimaryNgo.find(p).map(_.partner).foldMap(partnerName),
            ntacSupportEmail =    Option(p.getItacAccept.getEmail).getOrElse("(none)"),
            geminiContactEmail =  Option(p.getItacAccept.getContact).getOrElse("(none)"),
            progKey =             pih.pass(pid.toString),
            eavesdroppingLink =   cc.emailConfig.eavesdroppingURL,
            itacComments =        Option(p.getItac.getComment).getOrElse("(none)"),
          )

        val ccAddrs: List[String] =
          Option(p.getItacAccept.getEmail).toList ++ Option(p.getItacAccept.getContact).toList

        // this is gross but we'll come back and clean up
        val emailFolder = new File(pdfs.p1pdf.getParentFile.getParentFile, "emails")
        val emailFile   = new File(emailFolder, s"${p.getItacAccept.getProgramId}.txt")
        val text =
          s"""|TO:      ${p.getInvestigators().getPi().getEmail()}
              |CC:      ${ccAddrs.mkString(", ")}
              |SUBJECT: $sub
              |
              |$body
              |""".stripMargin

        emailFolder.mkdir
        Files.write(emailFile.toPath, text.getBytes("UTF-8"))
        println(s"Wrote ${emailFile}")

      }

    }

  def partnerName(p: NgoPartner): String =
    p match {
      case US => "United States"
      case CA => "Canada"
      case UH => "University of Hawaii"
      case KR => "Republic of Korea"
      case BR => "Brazil"
      case AR => "Argentina"
      case CL => "Chile"
    }

  implicit val ShowSemester:   Show[Semester]   = s => s"${s.getYear}${s.getHalf}"
  implicit val ShowTimeAmount: Show[TimeAmount] = ta => s"${ta.getValue.setScale(1, RoundingMode.HALF_UP)} ${ta.getUnits.name.toLowerCase}"
  implicit val ShowPrincipalInvestigator: Show[PrincipalInvestigator] = pi => s"${pi.getFirstName} ${pi.getLastName}"
  implicit val ShowLocalDate:   Show[LocalDate]   = DateTimeFormatter.ofPattern("MMM d YYYY").format(_).toUpperCase

  def emailSubjectAndBody(
    deadline:           LocalDate,
    instructionsUrl:    String,
    semester:           Semester,
    progTitle:          String,
    piName:             PrincipalInvestigator,
    progId:             String,
    timeAwarded:        TimeAmount,
    programTime:        TimeAmount,
    partnerTime:        TimeAmount,
    queueBand:          Int,
    country:            String,
    ntacSupportEmail:   String,
    geminiContactEmail: String,
    progKey:            String,
    eavesdroppingLink:  String,
    itacComments:       String
  ): (String, String) = (
    show"$progId Gemini PI Notification",
    show"""|Dear $semester Gemini Principal Investigator,
           |
           |Congratulations! You are receiving this email because your proposal for time on Gemini was
           |successful.  This email contains important information concerning the Phase II definition of
           |your program.
           |
           |!!!THE GENERAL DEADLINE FOR COMPLETING YOUR PHASE II IS $deadline!!!
           |
           |Step by step instructions for completing the Phase II Science Programs for all Gemini North and
           |Gemini South instruments as well as detailed information about Eavesdropping, and Classical and
           |Priority Visitor Observer programs are given at the following link:
           |
           |  $instructionsUrl
           |
           |Please note that since the 2023A Call for Proposals was released there have been changes to both 
           |the Gemini North and South semester on-sky dates. At Gemini North, we are not scheduling new 
           |programs between February 1 and 28, due to the uncertainty in the timeline for the primary mirror
           |to be reinstalled and characterized (see
           |
           |  https://www.gemini.edu/news/general-announcements/gemini-north-shutdown-updates
           |
           |for updates).   At Gemini South, the planned shutdown in April has been extended by one week, and
           |now will run from April 3 to 28. 
           |
           |PROGRAM SUMMARY
           |-----------------------
           |
           |Program Title:                       ${WordWrap.from(progTitle).maxWidth(55).newLine("\n                                     ").wrap()}
           |Principal Investigator:              $piName
           |Gemini Program ID:                   $progId
           |Total Time Awarded:                  $timeAwarded
           |Program Time Awarded:                $programTime
           |Partner Calibration Time Awarded:    $partnerTime
           |Scientific Ranking Band:             $queueBand
           |Gemini Participant Phase II support: $country
           |Principal Support:                   $ntacSupportEmail
           |Additional Support:                  $geminiContactEmail
           |Program Key Password:                $progKey
           |
           |Remote Eavesdropping Google Spreadsheet link (Band 1 and 2 only):
           |
           |  $eavesdroppingLink
           |
           |!!! Your program key password is necessary for accessing both your Phase II program using the
           |Observing Tool and your data in the Gemini Observatory Archive!!!
           |
           |Your awarded time has been split into program and partner (baseline calibration) components.
           |In the OT you will see only the awarded program time. During Phase II you will create the
           |observations to fill this time.
           |
           |You will be the single point of contact for Phase II preparation and notification of data
           |availability.  If you wish to change or add contact information for your program, please
           |contact your support scientists (Principal or Additional Support).  A Gemini Contact Scientist
           |will be able to make the necessary changes.
           |
           |ITAC FEEDBACK
           |------------------
           |
           |You may receive feedback concerning your proposal from your National TAC or NGO. In addition,
           |the following comment (if any) comes from the International Time Allocation Committee (ITAC):
           |
           |  ${WordWrap.from(itacComments).maxWidth(80).newLine("\n  ").wrap()}
           |
           |Thank you for your prompt attention to your Phase II submission and we wish you well in your
           |investigations.
           |
           |Regards,
           |
           |Sandy Leggett, ITAC Chair (sandy.leggett@noirlab.edu)
           |Atsuko Nitta, Gemini North Head of Science Operations (atsuko.nitta@noirlab.edu)
           |Joanna Thomas-Osip, Gemini South Head of Science Operations (joanna.thomas@noirlab.edu)
           |""".stripMargin
  )

}
