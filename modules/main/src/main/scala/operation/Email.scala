// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats._
import cats.implicits._
import itac.config.QueueConfig
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.p1.CoreProposal
import edu.gemini.tac.qengine.p1._
import org.apache.velocity.VelocityContext
import java.io.StringWriter
import scala.jdk.CollectionConverters._
import cats.effect.Sync
import cats.Parallel
import itac.Operation
import cats.effect.{Blocker, ExitCode}
import _root_.io.chrisdavenport.log4cats.Logger
import itac.Workspace
import java.nio.file.Path
import edu.gemini.spModel.core.Site
import itac.EmailTemplateRef
import edu.gemini.spModel.core.Semester
import org.apache.velocity.app.VelocityEngine
import edu.gemini.tac.qengine.api.QueueEngine
import edu.gemini.tac.qengine.api.queue.ProposalQueue
// import edu.gemini.util.security.auth.ProgIdHash
import edu.gemini.model.p1.immutable.TimeAmount
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.QueueBand.QBand1
import edu.gemini.tac.qengine.p1.QueueBand.QBand2
import edu.gemini.tac.qengine.p1.Mode

/**
 * @see Velocity documentation https://velocity.apache.org/engine/2.2/developer-guide.html
 */
object Email {

  // We have this instance in scalaz but we need the cats one here
  implicit val m: Monoid[TimeAmount] =
    new Monoid[TimeAmount] {
      def combine(x: TimeAmount, y: TimeAmount): TimeAmount = x |+| y
      def empty: TimeAmount = TimeAmount.empty
    }

  implicit class TimeOps(self: Time) {
    def toPrettyString: String =
      f"${self.value}%.2f ${self.unit}%s"
  }

  // This one's easiest if we uncurry everything.
  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      // The entry
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {

        // Velocity engine, which takes a bit of work to set up but the computation as a whole is pure.
        val velocity: VelocityEngine = {

          // I feel bad about this but it's the best I could figure out. I need the underlying
          // side-effecting logger from `log` so I can make stupid Velocity use it, but there's no
          // direct accessor. So we will try to pull it out and if it fails we get the normal logger
          // and well, too bad that's what we get. The underlying class is probably
          // io.chrisdavenport.log4cats.slf4j.internal.Slf4jLoggerInternal.Slf4jLogger, which has a
          // `logger` member (and generated accessor `logger()`). In the future maybe someone will
          // use something else, in which case they'll find this comment and work something out.
          val sideEffectingLogger: Option[org.slf4j.Logger] =
            Either.catchNonFatal {
              val getter     = log.getClass.getDeclaredMethod("logger")
              val underlying = getter.invoke(log).asInstanceOf[org.slf4j.Logger]
              underlying
            } .toOption

          val ve = new VelocityEngine
          ve.setProperty("runtime.strict_math",        true)
          ve.setProperty("runtime.strict_mode.enable", true)
          sideEffectingLogger.foreach(ve.setProperty("runtime.log.instance", _))
          ve

        }

        for {
          q  <- computeQueue(ws)
          (ps, qc) = q
          c <- ws.queueConfig(siteConfig)
          // _  <- createSuccessfulEmailsForClassical(velocity, ws, ps, c, qc.queue)
          _  <- createSuccessfulEmailsForQueue(velocity, ws, ps, c, qc.queue)
          // TODO: more!
        } yield ExitCode.Success

      }

      // For now
      type MailMessage = String

      /** Some convenience operations for filtering a list of proposals. */
      implicit class ProposalListOps(ps: List[Proposal]) {

        def classicalProposalsForSite(site: Site): List[Proposal] =
          ps.filter { p =>
              p.site == site           &&
              p.mode == Mode.Classical &&
            !p.isJointComponent
          }

        def successfulQueueProposalsForSite(site: Site, pq: ProposalQueue): List[Proposal] =
          ps.filter { p =>
             pq.positionOf(p).isDefined &&
             p.site == site       &&
             p.mode == Mode.Queue &&
            !p.isJointComponent
          }

      }

      def createPiEmail(velocity: VelocityEngine,ws: Workspace[F], p: Proposal, pq: ProposalQueue): F[MailMessage] = {
        // We have no types to ensure these things, so let's assert to be sure.
        assert(!p.isJointComponent, "Program must not be a joint component.")
        // TODO: assert that p is successful
        for {
          s  <- ws.commonConfig.map(_.semester)
          t  <- ws.readEmailTemplate(EmailTemplateRef.PiSuccessful)
          // h  <- ws.progIdHash
          ps  = velocityBindings(p, s, pq) // , h)
          _  <- Sync[F].delay(ps.foreach(println))
          tit = merge(velocity, t.name, t.titleTemplate, ps)
          _  <- Sync[F].delay(println(tit))
          bod = merge(velocity, t.name, t.bodyTemplate, ps)
          _  <- Sync[F].delay(println(bod))
        } yield "ok"
      }

      def createNgoEmails(p: Proposal): F[List[MailMessage]] = {
        p match {
          case c: CoreProposal      => List(s"<email for ${c.ntac.partner.id}>")
          case j: JointProposal     => j.ntacs.map(n =>s"<email for ${n.partner.id}>")
          case _: JointProposalPart => Nil // Don't create mails for joint parts
        }
      } .pure[F]

      // def createSuccessfulEmailsForClassical(velocity: VelocityEngine, ws: Workspace[F], ps: List[Proposal], qc: QueueConfig, pq: ProposalQueue): F[List[MailMessage]] =
      //   ps.classicalProposalsForSite(qc.site).parFlatTraverse { cp =>
      //     (createPiEmail(velocity, ws, cp, pq), createNgoEmails(cp)).parMapN(_ :: _)
      //   }

      def createSuccessfulEmailsForQueue(velocity: VelocityEngine, ws: Workspace[F], ps: List[Proposal], qc: QueueConfig, pq: ProposalQueue): F[List[MailMessage]] =
        ps.successfulQueueProposalsForSite(qc.site, pq).flatTraverse { cp => // TODO: parFlatTraverse
          (createPiEmail(velocity, ws, cp, pq), createNgoEmails(cp)).parMapN(_ :: _)
        }

      /**
       * Given a Velocity template and a map of bindings, evaluate the template and return the
       * generated text, or an indication of why it failed.
       */
      def merge(velocity: VelocityEngine, templateName: String, template: String, bindings: Map[String, AnyRef]): Either[Throwable, String] =
        Either.catchNonFatal {
          val ctx = new VelocityContext(bindings.asJava)
          val out = new StringWriter
          if (!velocity.evaluate(ctx, out, templateName, template)) {
            // It's not at all clear when we get `false` here rather than a thrown exception. It
            // has never come up in testing. But this is here just in case.
            throw new RuntimeException("Velocity evaluation failed (see the log, or re-run with -v debug if there is none!).")
          }
          out.toString
        }

      /**
       * Construct a map of key/value pairs that will be bound to the Velocity context. Our strategy
       * is to define only the keys where values are available (rather than using `null`) and then
       * running in strict reference mode. What this means is, undefined references will throw an
       * exception as Satan intended. Templates can use `#if` to determine whether a key is defined or
       * not, before attempting a dereference.
       * @see strict reference mode https://velocity.apache.org/engine/1.7/user-guide.html#strict-reference-mode
       */
      def velocityBindings(p: Proposal, s: Semester, q: ProposalQueue) /*, pih: ProgIdHash)*/: Map[String, AnyRef] = {

        // println(s"==> ${p.id.reference} - ${q.programId(p).map(_.toString).orEmpty}")

        var mut = scala.collection.mutable.Map.empty[String, AnyRef]
        mut = mut // defeat bogus unused warning

        // bindings that are always present
        mut += "semester" -> s.toString

        p.piEmail     .foreach(v => mut += "piMail"       -> v)
        p.piName      .foreach(v => mut += "piName"       -> v)
        p.p1proposal  .foreach(p => mut += "progTitle"    -> p.title)

        //     // proposals that made it that far must have an itac part, accepted ones must have an accept part
        //     Validate.notNull(proposal.getPhaseIProposal());
        //     Validate.notNull(proposal.getItac());

        //     // get some of the important objects
        //     PhaseIProposal doc = proposal.getPhaseIProposal();
        //     Itac itac = proposal.getItac();
        val itac = p.p1proposal.get.proposalClass.itac
        //     Investigator pi = doc.getInvestigators().getPi();
        //     Submission partnerSubmission = doc.getPrimary();

        //     this.geminiComment = itac.getGeminiComment() != null ? itac.getGeminiComment() : "";
        mut += "geminiComment" -> "" // TODO
        //     this.itacComments =  itac.getComment() != null ? itac.getComment() : "";
        itac.map(_.comment.orEmpty).foreach(v => mut += "itacComments" -> v)

        //     if (itac.getRejected() || itac.getAccept() == null) {
        //         // either rejected or no accept part yet: set empty values
        //         this.progId = "";
        //         this.progKey = "";
        //         this.geminiContactEmail = "";
        //         this.timeAwarded = "";
        //     } else {
        //         this.progId = itac.getAccept().getProgramId();
        q.programId(p).foreach(v => mut += "progId" -> v)
        //         this.progKey = ProgIdHash.pass(this.progId);

        // TODO
        // q.programId(p).foreach(v => mut += "progKey" -> pih.pass(v.toString))

        //         this.geminiContactEmail = itac.getAccept().getContact();
        itac.flatMap(_.decision.flatMap(_.toOption)).flatMap(_.contact).foreach(v => mut += "geminiContactEmail" -> v)
        //         this.timeAwarded = itac.getAccept().getAward().toPrettyString();
        itac.flatMap(_.decision.map(_.foldMap(_.award))).foreach(v => mut += "timeAwarded" -> v.toHours.format()) // handles both accept and reject below
        //     }

        //     if (!successful) {
        //         // ITAC-70: use original partner time, the partner time might have been edited by ITAC to "optimize" queue
        //         this.timeAwarded = "0.0 " + ntacExtension.getRequest().getTime().getUnits();
        //     }
        // ^^ handled above

        //     if (proposal.isJoint()) {
        //         StringBuffer info = new StringBuffer();
        //         StringBuffer time = new StringBuffer();
        //         for(Submission submission : doc.getSubmissions()){
        //             NgoSubmission ngoSubmission = (NgoSubmission) submission;
        //             info.append(ngoSubmission.getPartner().getName());
        //             info.append(" ");
        //             info.append(ngoSubmission.getReceipt().getReceiptId());
        //             info.append(" ");
        //             info.append(ngoSubmission.getPartner().getNgoFeedbackEmail()); //TODO: Confirm -- not sure
        //             info.append("\n");
        //             time.append(ngoSubmission.getPartner().getName() + ": " + ngoSubmission.getAccept().getRecommend().toPrettyString());
        //             time.append("\n");
        //         }
        //         this.jointInfo = info.toString();
        //         this.jointTimeContribs = time.toString();
        //     }

        //     // ITAC-70 & ITAC-583: use original recommended time, the awarded time might have been edited by ITAC to optimize queue
        //     TimeAmount time = ntacExtension.getAccept().getRecommend();
        //     this.country = ntacExtension.getPartner().getName();
        mut += "country"             -> p.ntac.partner.fullName
        //     this.ntacComment = ntacExtension.getComment() != null ? ntacExtension.getComment() : "";
        mut += "ntacComment"         -> p.ntac.comment.orEmpty
        //     this.ntacRanking = ntacExtension.getAccept().getRanking().toString();
        mut += "ntacRanking"         -> p.ntac.ranking
        //     this.ntacRecommendedTime = time.toPrettyString();
        mut += "ntacRecommendedTime" -> p.ntac.awardedTime.toHours.toString
        //     this.ntacRefNumber = ntacExtension.getReceipt().getReceiptId();
        mut += "ntacRefNumber"       -> p.ntac.reference
        //     this.ntacSupportEmail = ntacExtension.getAccept().getEmail();
        mut += "ntacSupportEmail"    -> p.ntac.partner.email
        //     // We'll match the total time to the time awarded and scale
        //     // the program and partner time to fit

        //     // Find the correct set of observations. Note that they return the active observations already
        //     List<Observation> bandObservations = null;
        //     if (banding != null && banding.getBand().equals(ScienceBand.BAND_THREE)) {
        //       bandObservations = proposal.getPhaseIProposal().getBand3Observations();
        //     } else {
        //       bandObservations = proposal.getPhaseIProposal().getBand1Band2ActiveObservations();
        //     }
        val bandObservations: List[edu.gemini.model.p1.immutable.Observation] =
          q.positionOf(p).map { pos => p.obsListFor(pos.band).map(_.p1Observation) } .get

        //     if (successful) {
        //         TimeAmount progTime = new TimeAmount(0, TimeUnit.HR);
        //         TimeAmount partTime = new TimeAmount(0, TimeUnit.HR);
        //         for (Observation o : bandObservations) {
        //             progTime = progTime.sum(o.getProgTime());
        //             partTime = partTime.sum(o.getPartTime());
        //         }
        val (progTime, partTime): (TimeAmount, TimeAmount) =
          bandObservations.foldMap(o => (o.progTime.orEmpty, o.partTime.orEmpty))

        //         // Total time for program and partner
        //         TimeAmount sumTime = progTime.sum(partTime);
        val sumTime: TimeAmount =
          progTime |+| partTime

        //         // Scale factor with respect to the awarded time
        //         double ratio = progTime.getValueInHours().doubleValue() / sumTime.getValueInHours().doubleValue();
        val ratio: Double =
          progTime.toNights.value / sumTime.toNights.value

        //         // Scale the prog and program time
        //         this.progTime = TimeAmount.fromMillis(itac.getAccept().getAward().getDoubleValueInMillis() * ratio).toPrettyString();
        //         this.partnerTime = TimeAmount.fromMillis(itac.getAccept().getAward().getDoubleValueInMillis() * (1.0 - ratio)).toPrettyString();
        mut += "programTime" -> Time.millisecs((p.time.ms * ratio).toLong).toHours.toPrettyString
        mut += "partnerTime" -> Time.millisecs((p.time.ms * (1.0 - ratio)).toLong).toHours.toPrettyString

        //     } else {
        //         this.partnerTime = "0.0 " + ntacExtension.getRequest().getTime().getUnits();
        //         this.progTime = "0.0 " + ntacExtension.getRequest().getTime().getUnits();
        //     }

        //     // Merging of PIs: first names and last names will be concatenated separated by '/',
        //     // emails will be concatenated to a list separated by semi-colons
        //     this.piMail = pi.getEmail();
        //     this.piName = pi.getFirstName() + " " + pi.getLastName();
        val pi = p.p1proposal.get.investigators.pi
        mut += "piMail"       -> s"${pi.firstName} ${pi.lastName}"
        mut += "piName"       -> pi.email

        //     if (doc.getTitle() != null) {
        //         this.progTitle = doc.getTitle();
        //     }
        p.p1proposal  .foreach(p => mut += "progTitle"    -> p.title)

        //     if (banding != null) {
        //         this.queueBand = banding.getBand().getDescription();
        //     } else if (proposal.isClassical()) {
        //         this.queueBand = "classical";
        //     } else {
        //         this.queueBand = N_A;
        //     }
        p.mode match {
          case Mode.Queue        => q.positionOf(p).foreach(pos => mut += "queueBand" -> s"Band ${pos.band.number}")
          case Mode.Classical    => mut += "queueBand" -> "classical"
          case Mode.LargeProgram => mut += "queueBand" -> "N/A"
        }

        val eavesdroppingLink: String =
          q.positionOf(p).map(_.band) match {
            case Some(QBand1 | QBand2) => "<link>" // TODO
            case _                     => "N/A"
          }

        mut += "eavesdroppingLink" -> eavesdroppingLink

        // Done
        mut.toMap

      }

    }

}


