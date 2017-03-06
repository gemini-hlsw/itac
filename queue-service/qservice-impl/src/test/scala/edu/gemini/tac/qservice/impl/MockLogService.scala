package edu.gemini.tac.qservice.impl

import edu.gemini.tac.service.{AbstractLogService => PsLogService}
import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.persistence.{Proposal => PsProposal}
import edu.gemini.tac.persistence.{LogEntry  => PsLogEntry}
import edu.gemini.tac.persistence.{LogNote   => PsLogNote}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}

import scala.collection.JavaConversions._
import edu.gemini.tac.util.ProposalImporter

class MockLogService extends PsLogService {
  var qLog: Map[PsQueue,     List[PsLogEntry]] = Map.empty
  var cLog: Map[PsCommittee, List[PsLogEntry]] = Map.empty
  var pLog: Map[PsProposal,  List[PsLogEntry]] = Map.empty

  def addNoteToEntry(logEntry: PsLogEntry, logNote: PsLogNote) {
    val s = Option(logEntry.getNotes).getOrElse(new java.util.HashSet[PsLogNote])
    s.add(logNote)
    logEntry.setNotes(s)
  }

  private def entries[T](key: T, log: Map[T, List[PsLogEntry]]): java.util.List[PsLogEntry] =
    log.get(key).getOrElse(Nil).reverse

  def getLogEntriesForQueue(queue: PsQueue): java.util.List[PsLogEntry] =
    entries(queue, qLog)

  def getLogEntriesForCommittee(committee: PsCommittee): java.util.List[PsLogEntry] =
    entries(committee, cLog)

  def getLogEntriesForProposal(proposal: PsProposal): java.util.List[PsLogEntry] =
    entries(proposal, pLog)

  private def add[T](entry: PsLogEntry, key: T, log: Map[T, List[PsLogEntry]]): Map[T, List[PsLogEntry]] =
    log.updated(key, entry :: log.get(key).getOrElse(Nil))

  def addLogEntry(logEntry: PsLogEntry) {
    Option(logEntry.getCommittee).foreach(c => cLog = add(logEntry, c, cLog))
    Option(logEntry.getQueue).foreach(    q => qLog = add(logEntry, q, qLog))
  }

  def createNoteForEntry(entryId: java.lang.Long, text: java.lang.String) {}
  def updateNote(noteId: java.lang.Long, text: java.lang.String) {}
  def deleteNote(noteId: java.lang.Long) {}

  def createNewForCommittee(message: java.lang.String, types: java.util.Set[PsLogEntry.Type], committee: PsCommittee) : PsLogEntry = {
    val logEntry = new PsLogEntry()
    logEntry.setTypes(types)
    logEntry.setCommittee(committee)
    logEntry.setMessage(message)
    logEntry.setCreatedAt(new java.util.Date())
    logEntry.setUpdatedAt(new java.util.Date())
    logEntry
  }

  def createNewForQueue(message: java.lang.String, types: java.util.Set[PsLogEntry.Type], queue: PsQueue) : PsLogEntry = {
    val logEntry = new PsLogEntry()
    logEntry.setTypes(types)
    logEntry.setQueue(queue)
    logEntry.setCommittee(queue.getCommittee)
    logEntry.setMessage(message)
    logEntry.setCreatedAt(new java.util.Date())
    logEntry.setUpdatedAt(new java.util.Date())
    logEntry
  }


  def createNewForProposalOnly(message : String, types: java.util.Set[PsLogEntry.Type], proposal :PsProposal) : PsLogEntry  = {
    val logEntry = new PsLogEntry()
    logEntry.setTypes(types)
    logEntry.setProposal(proposal)
    logEntry.setMessage(message)
    logEntry.setCreatedAt(new java.util.Date())
    logEntry.setUpdatedAt(new java.util.Date())
    logEntry
  }
  def createNewForCommitteeAndProposal(message: String, types : java.util.Set[PsLogEntry.Type], committee : PsCommittee, proposal : PsProposal) : PsLogEntry = {
    val logEntry = new PsLogEntry()
    logEntry.setTypes(types)
    logEntry.setCommittee(committee)
    logEntry.setProposal(proposal)
    logEntry.setMessage(message)
    logEntry.setCreatedAt(new java.util.Date())
    logEntry.setUpdatedAt(new java.util.Date())
    logEntry
  }

  def logSuccessfullyImportedProposals(successfulResults : java.util.List[ProposalImporter.Result]) {}
  def logBandedProposals(queue : PsQueue) {}
}