package edu.gemini.tac.service.check.impl

import checks._
import edu.gemini.tac.service.{check => api}

import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.{IsExchange, Proposal}

// The implementation was complicated a bit by the decision to step through
// proposals, one by one, applying all checks to each instead of the
// other way around.  Namely, if each check is performed in turn on the
// entire collection of proposals then checks that require state to perform
// their calculation could keep it locally.  Instead, the check is run
// repeatedly adding to exportable state for each proposal.

/**
 * Implements the ProposalChecker API by applying all proposal checks to each
 * proposal in turn.
 */
object ProposalChecker extends api.ProposalChecker {

  val checkList = List[CheckFunction](
    Band3ConditionsCheck,
    Band3RequestedTime,
    Band3MinimumTimeCheck,
    ClassicalTimeRequestCheck,
    ConditionsCheck,
    InstrumentCheck,
    RankingCheck,
    RecommendedTimeCheck,
    ReferenceCheck,
    SubmissionPartnerCheck,
    TargetsCheck,
    MixedToOAndOtherTargetCheck,
    ZeroRaAndDecInNonTooCheck
  )

  /**
   * Returns all the proposal checks that will be performed.
   */
  def checks: java.util.Set[api.ProposalCheck] =
    new java.util.HashSet[api.ProposalCheck](checkList.asJavaCollection)

  // Object folded across the list of proposals in order calculate the set of
  // proposal issues.  Contains all issues found for previous proposals and the
  // current set of updated check functions.
  private[impl] case class FoldValue(issues: Set[api.ProposalIssue], adapters: List[CheckFunction]) {
    // Performs all checks on the given proposal, updating the collection of
    // proposal issues and updating an stateful check functions.
    def +(p: Proposal): FoldValue =
      adapters.map { _.check(p) }.unzip match {
        case (newIssues, updatedAdapters) =>
          FoldValue((issues/:newIssues)(_ ++ _), updatedAdapters)
      }
  }

  private[impl] object FoldValue {
    def empty(checkList: List[CheckFunction]): FoldValue = FoldValue(Set.empty, checkList)
  }

  /**
   * Converts the collection of proposals to a Scala list and filters out
   * proposals that are to be executed on Keck or Subaru.
   */
  private def validProposals(proposals: java.util.Collection[Proposal]): List[Proposal] =
    proposals.asScala.toList.filterNot(IsExchange.isForExchangePartner)

  /**
   * Applies all proposal checks to each proposal in turn.
   */
  def exec(proposals: java.util.Collection[Proposal]): java.util.Set[api.ProposalIssue] =
    exec(validProposals(proposals), checkList).issues.asJava

  // Split out for testing support.
  private[impl] def exec(proposals: List[Proposal], checkList: List[CheckFunction]): FoldValue =
    (FoldValue.empty(checkList)/:proposals)(_ + _)
}