//package edu.gemini.tac.qengine.app
//
//import edu.gemini.tac.qengine.p1io.P1IoError
//import java.io.File
//import edu.gemini.tac.qengine.p1._
//import edu.gemini.tac.persistence.{Proposal=>P1Proposal}
//import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal
//import edu.gemini.tac.qengine.p1.CoreProposal
//
///**
//* A simple container for the results of parsing a collection of proposal
//* files.  It splits up the errors and successfully parsed proposals into
//* separate lists.
//*/
//case class ProposalCollection(errors: List[P1IoError], parsed: List[ParsedProposal]) {
//
//  def +:(e: Either[P1IoError, ParsedProposal]): ProposalCollection =
//    if (e.isLeft)
//      copy(errors = e.left.get  :: this.errors)
//    else
//      copy(parsed = e.right.get :: this.parsed)
//
//    // Map of proposal id to P1P
//  val p1: Map[Proposal.Id, PhaseIProposal] =
//    parsed.flatMap {
//      pprop => (pprop.prop.id -> pprop.p1Doc) :: (pprop.prop match {
//        case cp: CoreProposal       => Nil
//        case jp: JointProposal      => jp.toParts.map(_.id -> pprop.p1Doc)
//        case dp: DelegatingProposal => sys.error("Unhandled case")
//      })
//    }.toMap
//
//  /**
//   * Extracts the proposals that parsed correctly, handling joint proposal
//   * parts correctly.
//   */
//  val props: List[Proposal] = {
//
//    // Group the proposals by their proposal key, creating a map from
//    // key -> List[Proposal] of proposals that share the same key
//    val keyMap = parsed.groupBy(_.key)
//
//    // Shorten the long keys used by joint proposals to a joint proposal id
//    // in the style of PIPD (in other words J#).
//    // Creates a jointIdMap: Map[String, String] where "A2C4-DF456 ..." -> "J1"
//    val jointKeys  = keyMap.filter(tup => tup._2.size > 1).keys.toList
//    val jointIdMap = jointKeys.zipWithIndex.map(tup => (tup._1, "J" + (tup._2 + 1))).toMap
//
//    def merge(pprops: List[ParsedProposal]): JointProposal = {
//      val jointId = jointIdMap(pprops(0).key)
//      val cores = pprops.map(_.prop).flatMap {
//        case cp: CoreProposal       => List(cp)
//        case jp: JointProposal      => jp.toParts.map(_.core)
//        case pt: JointProposalPart  => sys.error("wasn't expecting a part!")
//        case dp: DelegatingProposal => sys.error("Unhandled case")
//      }
//      val scores = cores.sorted(Proposal.MasterOrdering)
//      val master = scores(0)
//      JointProposal(jointId, master, scores.map(_.ntac))
//    }
//
//
//   // Where there is more than one proposal with the same ID, turn the list
//   // of CoreProposal into a list of JointProposalPart.
//    val jmap = keyMap.mapValues {
//      lst => if (lst.size == 1) lst(0).prop else merge(lst)
//    }
//
//    // Throw away the map, we just want the List of CoreProposal and
//    // JointProposal.
//    jmap.values.toList
//  }
//
//  /**
//   * Map from joint id to the list of proposals that share that id.
//   */
//  val joints: Map[String, List[Proposal]] =
//      (Map.empty[String,List[Proposal]]/:props) {
//        (m, proposal) => proposal match {
//          case cp: CoreProposal       => m
//          case jp: JointProposal      => m + (jp.jointId.get -> jp.toParts)
//          case pt: JointProposalPart  => sys.error("wasn't expecting a part!")
//          case dp: DelegatingProposal => sys.error("Unhandled case")
//        }
//      }
//}
//
//object ProposalCollection {
//  val empty: ProposalCollection = ProposalCollection(Nil, Nil)
//
//  //  def apply(files: List[File]): ProposalCollection =
//  //    (empty/:files)((col, file) => ProposalReader.read(file) +: col)
//
//
//
//}