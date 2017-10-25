package edu.gemini.tac.psconversion

import edu.gemini.tac.persistence.phase1.queues.{PartnerSequence => PsPartnerSequence }
import edu.gemini.tac.persistence.queues.{Queue=>PsQueue}
import edu.gemini.tac.qengine.api.config.{CsvToPartnerSequenceParser, CustomPartnerSequence, PartnerSequence, ProportionalPartnerSequence}
import edu.gemini.tac.qengine.ctx.{Site, Partner}

import scalaz._
import Scalaz._


/**
 * Adapts the persistence-layer PartnerSequence (which is essentially a
 * comma-separated string) into a queue-engine sequence.
 *
 * For the Site and set of partners passed-in (i.e., filtered for Site), returns
 * a queue-engine partner sequence. The returned sequence will be a
 * ProportionalPartnerSequence if the PsPartnerSequence is null. Otherwise it
 * will be a CustomPartnerSequence.
 */
object PartnerSequenceConverter {
  def BAD_INITIAL_PICK(p: Partner, s: Site): String =
    s"Initial pick ${p.id} cannot receive time for ${s.abbreviation}"

  def PARTNERS_MISSING_FROM_SEQUENCE(p: Iterable[Partner]): String =
    s"Custom sequence missing partners: ${p.mkString(", ")}"

  /**
   * Creates a proportional partner sequence.
   *
   * @param allPartners all valid partners (must contain at least one partner)
   * @param initialPick override the initial pick (if any must be a partner
   *                    contained in allPartners)
   * @return a proportional partner sequence or an error if the constraints are
   *         not met by the arguments
   */
  def proportional(allPartners: List[Partner], initialPick: Option[Partner], site: Site): PsError \/ PartnerSequence = {
    // Ensure that the initial pick (if defined) is an element of allPartners
    def validatedPick: PsError \/ Option[Partner] =
      initialPick.fold(none[Partner].right[PsError]) { p =>
        if (allPartners.contains(p)) some(p).right[PsError]
        else BadData(s"Initial pick $p not included in partner list: ${allPartners.mkString(", ")}").left[Option[Partner]]
      }

    def highestPerc: Partner = allPartners.maxBy(_.percentDoubleAt(site))

    for {
      _    <- if (allPartners.size == 0) BadData("No partners have been defined").left else ().right
      pick <- validatedPick
      _    <- if (pick.forall(_.sites.contains(site))) ().right else BadData(BAD_INITIAL_PICK(pick.get, site)).left
    } yield new ProportionalPartnerSequence(allPartners, site, pick | highestPerc)
  }

  private def initialPick(partners: List[Partner], queue: PsQueue): PsError \/ Option[Partner] =
    for {
      psPick <- safePersistence { Option(queue.getPartnerWithInitialPick) }
      pick   <- psPick.fold(none[Partner].right[PsError])(ps => PartnerConverter.find(ps, partners).map(some))
    } yield pick

  /**
   * Read a custom partner sequence from the database CSV string.
   */
  def readCustom(psPartnerSequence: PsPartnerSequence, queue: PsQueue): PsError \/ PartnerSequence = {
    def errorMessage(unmatchedIds: Set[String]): String =
      s"Could not find partner matching: ${unmatchedIds.toList.sorted.mkString(", ")}"

    // A repeating custom sequence had better have all the partners with a
    // valid time share in it.  A non-repeating custom sequence is followed by
    // a "proportional" sequence which is guaranteed to have all the partners.
    def validateCustomSequence(partners: List[Partner], repeat: Boolean, seq: List[Partner]): BadData \/ Unit = {
      val missing = partners.filter(_.share.doubleValue > 0.0).toSet &~ seq.toSet
      if (!repeat || missing.isEmpty) ().right
      else BadData(PARTNERS_MISSING_FROM_SEQUENCE(missing.toList.sortBy(_.id))).left
    }

    for {
      partners <- Partners.fromQueue(queue)
      site     <- SiteConverter.read(queue)
      csv      <- nullSafePersistence(s"Unspecified custom partner sequence") { psPartnerSequence.getCsv }
      seq      <- CsvToPartnerSequenceParser.parse(csv, partners.values).disjunction.leftMap(s => BadData(errorMessage(s)))
      repeat   <- nullSafePersistence(s"Sequence repeat not specified") { psPartnerSequence.getRepeat }
      subSeq = if (repeat) None else Some(new ProportionalPartnerSequence(partners.values, site))
      name     <- safePersistence { psPartnerSequence.getName }.map { n => Option(n) | "Custom Partner Sequence" }
      pick     <- initialPick(partners.values, queue)
      _        <- validateCustomSequence(partners.values, repeat, seq)
    } yield new CustomPartnerSequence(seq, site, name, subSeq, pick)
  }

  /**
   * Read a proportional partner sequence using configuration values from the
   * provided queue.
   */
  def readProportional(queue: PsQueue): PsError \/ PartnerSequence =
    for {
      partners <- Partners.fromQueue(queue)
      pick     <- initialPick(partners.values, queue)
      site     <- SiteConverter.read(queue)
      prop     <- proportional(partners.values, pick, site)
    } yield prop
}