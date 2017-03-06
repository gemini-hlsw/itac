package edu.gemini.tac.qservice.impl

import edu.gemini.tac.service.{ICommitteeService => PsCommitteeService}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import java.lang.Long

import scala.collection.JavaConversions._
import edu.gemini.tac.persistence.phase1.queues.{PartnerSequence => PsPartnerSequence}
import edu.gemini.tac.persistence.joints.JointProposal
import edu.gemini.tac.persistence.{Committee => PsCommittee, Proposal => PsProposal, Person => PsPerson}

object MockCommitteeService {
  private def unboxBoolean(b: java.lang.Boolean): Boolean =
    Option(b).getOrElse(java.lang.Boolean.FALSE).booleanValue
}

import MockCommitteeService._

/**
* Mock committee service for testing.
*/
class MockCommitteeService(val proposals: List[PsProposal]) extends PsCommitteeService {
  override def getPartnerSequenceOrNull(committeeId : Long) : PsPartnerSequence = null

  var cMap: Map[Long, PsCommittee] = Map.empty

  def saveCommitee(committee: PsCommittee) {
    cMap = cMap.updated(committee.getId, committee)
  }

  private def queues(c: PsCommittee): java.util.List[PsQueue] = {
    val s = Option(c.getQueues).getOrElse(java.util.Collections.emptySet)
    new java.util.ArrayList[PsQueue](s)
  }

  def getAllQueuesForCommittee(committeeId: Long): java.util.List[PsQueue] =
    cMap.get(committeeId).map(queues).getOrElse(Nil)

  def getAllQueuesForCommittee(committeeId: Long, siteId: Long): java.util.List[PsQueue] =
    null

  def getProposal(committeeId: Long, proposalId: Long): PsProposal =
    proposals.find(p => p.getId == proposalId).orNull  // null ... tsk tsk


  def getProposalForCommittee(committeeId: Long, proposalId: Long) =
    null

  def checkProposalForCommittee(committeeId: Long, proposalId: Long) =
    null

  def getAllProposalsForCommittee(committeeId: Long): java.util.List[PsProposal] =
    proposals.filter(p => Option(p.getCommittee).map(_.getId).getOrElse(-1l) == committeeId)

  def checkAllProposalsForCommittee(committeeId: Long) =
    null

  def getAllComponentsForCommittee(committeeId: Long) =
    null

  def checkAllComponentsForCommittee(committeeId: Long) =
    null

  def getAllProposalsForCommitteePartner(committeeId: Long, partnerAbbreviation: String) =
    null

  def getCommitteeWithProposals(committeeId: Long): PsCommittee =
    getCommittee(committeeId)

  def getCommittee(committeeId: Long): PsCommittee =
    cMap.get(committeeId).orNull // null again ... tsk tsk

  def getCommitteeWithMemberships(committeeId: Long): PsCommittee =
    cMap.get(committeeId).orNull // null again ... tsk tsk

  def getAllActiveCommittees: java.util.List[PsCommittee] =
    cMap.values.toList.filter(c => unboxBoolean(c.getActive))

  def getAllCommittees: java.util.List[PsCommittee] = cMap.values.toList

  def getCommitteeForExchangeAnalysis(committeeId: Long): PsCommittee =
    cMap.get(committeeId).orNull // null again ... tsk tsk

  def getAllCommitteeProposalsForQueueCreation(committeeId: Long): java.util.List[PsProposal] =
    getAllProposalsForCommittee(committeeId)

  def saveEditedProposal(proposal: PsProposal) {
    cMap = cMap.updated(proposal.getCommittee.getId, proposal.getCommittee)
  }

  def saveProposal(proposal: PsProposal) {
    cMap = cMap.updated(proposal.getCommittee.getId, proposal.getCommittee)
  }

  def getAllCommitteesFor(person : PsPerson): java.util.List[PsCommittee] =
    cMap.values.toList.filter(c => c.getMembers.exists(m => m.getId == person.getId))

  def populateProposalsWithListingInformation(proposals : java.util.List[PsProposal]) {}

  def setPartnerSequenceProportional(committeeId : Long) = {
    cMap.get(committeeId).orNull.setPartnerSequence(null)
  }

  def setPartnerSequence(committeeId : Long, psPartnerSequence : PsPartnerSequence) = {
    cMap.get(committeeId).orNull.setPartnerSequence(psPartnerSequence)
  }

  def dissolve(committee: PsCommittee, proposal: JointProposal) : Unit = {}
}

