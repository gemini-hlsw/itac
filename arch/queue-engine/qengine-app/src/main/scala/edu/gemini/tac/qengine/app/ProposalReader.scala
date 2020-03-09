//package edu.gemini.tac.qengine.app
//
//import java.io.{FileReader, File}
//import edu.gemini.tac.qengine.p1io.{P1IoError, ProposalIo}
//import edu.gemini.tac.persistence.phase1.PhaseIProposal
//
///**
//* Reads an XML file into a proposal if possible, otherwise produces a
//* P1IoError.
//*/
//private object ProposalReader {
//  import P1IoError.asP1IoError
//
//  private def error(file: File, msg: String): P1IoError =
//    "%s: Could not parse: %s".format(file.getName, msg)
//
//  /**
//   * Reads a file containing an XML proposal into a P1P, if possible.
//   */
//  def toP1(file: File): Either[P1IoError, PhaseIProposal] =
//    try {
//      val frdr = new FileReader(file)
//      val xrdr = new XmlDomReader(false)
//      P1ExtensionFactory.createInstance
//      Option(xrdr.readDocument(frdr)).toRight(error(file, "unknown"))
//    } catch {
//      case ex: Exception => Left(error(file, ex.getMessage))
//    }
//
//  /**
//   * Extracts the proposal id from the document.
//   */
//  private def uuid(file: File, p1p: PhaseIProposal): Either[P1IoError, String] =
//    for {
//      key <- Option(p1p.getProposalKey).toRight(error(file, "Missing proposal key.")).right
//      id  <- Option(key.getStringRepresentation).toRight(error(file, "Missing proposal key.")).right
//    } yield id
//
//  /**
//   * Reads a file containing an XML proposal into a Queue Engine Proposal,
//   * if possible.
//   */
//  def read(file: File): Either[P1IoError, ParsedProposal] = {
//    val res = for {
//      p1   <- toP1(file).right
//      id   <- uuid(file, p1).right
//      prop <- ProposalIo.toProposal(p1).right
//    } yield ParsedProposal(prop, id, p1)
//
//    // Prepend the file name to the error message if this proposal couldn't be
//    // parsed.
//    res.left.map(e => P1IoError(file.getPath + ": " + e.reason))
//  }
//}