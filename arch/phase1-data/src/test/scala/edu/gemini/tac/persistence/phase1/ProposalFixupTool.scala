package edu.gemini.tac.persistence.phase1

import xml._
import org.springframework.util.FileCopyUtils
import java.io.{InputStreamReader, FileInputStream, File}

/**
 * In the heat of the fight the addition of the submission key was forgotten for 2012B.
 * This script can be pointed at a directory with import data for ITAC and adds a submission key to all proposal
 * xmls trying to identify those that belong to the same joint and assigning them the same key. The key is
 * calculated from the hash keys of the title and the pi name.
 */

object ProposalFixupTool {

  val absInputDir = new File("/Users/fnussber/Downloads/2012B_PRODUCTION/meech")
  val absOutputDir = new File("/Users/fnussber/Downloads/2012B_PRODUCTION/meech-fixed")
  val fixupAccept = false;

  def main(args: Array[String]) {
    val inputDir = absInputDir
    val outputDir = absOutputDir
    fixupProposals(inputDir, outputDir)
  }

  /**
   * Fixup all proposals in a directory.
   * @param inputDir
   * @param outputDir
   */
  def fixupProposals(inputDir: File, outputDir: File) {
    inputDir listFiles() foreach {
      f => if (f.isDirectory) {
        fixupProposals(f, outputDir)
      } else {
        try {
          val fOut = targetFile(f, absInputDir, absOutputDir)
          if (f.getName.endsWith("xml")) fixupProposal(f, fOut) else FileCopyUtils.copy(f, fOut)
        } catch {
          case e: Exception => {
            System.out.println(e.printStackTrace)
            System.out.println("could not fixup prpoosal " + f.getAbsolutePath + ": " + e)
          }
        }
      }
    }
  }

  /**
   * Create target file object with similar directory structure relative from root input directory.
   * @param f
   * @param inDir
   * @param outDir
   * @return
   */
  def targetFile(f: File, inDir: File, outDir: File) = {
    val outFileRel = f.getAbsolutePath drop inDir.getAbsolutePath.length
    val outFileAbs = new File(outDir + outFileRel)
    outFileAbs.getParentFile.mkdirs;
    outFileAbs
  }
  
  /**
   * Fixup a single proposal.
   * @param proposalFile
   * @param fixedProposalFile
   */
  def fixupProposal(proposalFile: File, fixedProposalFile: File) {
    val proposal = XML load (new InputStreamReader(new FileInputStream(proposalFile), "UTF-8"))
    val key = ProposalKey.fromProposal(proposal)
    var newProposal = updateSubmissionsKey(proposal, key.toPseudoUUID)
// IMPORTANT: FOR PRODUCTION DO *NOT* FIXUP ACCEPT PARTS!
//    if (fixupAccept) {
//      newProposal = updateAcceptPart(newProposal, getPartnerFromFileName(proposalFile))
//    }
    XML save (fixedProposalFile.getAbsolutePath, newProposal, "UTF-8", true, null)  // important: write as UTF-8!
  }

  /**
   * Get the partner name from the file name
   * @param proposalFile
   * @return
   */
  def getPartnerFromFileName(proposalFile: File) = {
    proposalFile.getName().substring(0,2) match {
      case "su" => "subaru"
      case "ke" => "keck"
      case "N0" => "us"    // us files are currently not renamed?
      case "qp" => "us"    // us files are currently not renamed?
      case "cp" => "us"    // us files are currently not renamed?
      case "GS" => "br"    // br files are currently not renamed?
      case "GN" => "br"    // br files are currently not renamed?
      case other => other
    }
  }

  /**
   * Recursively goes through the xml document returning all nodes unchanged except for the queue, classical
   * and exchange nodes which will have a submission key added to them.
   * NOTE: This is how to work with Scala immutable xml structure -> instead of updating the one you have
   * a new one is built by recursively going through the original one and returning all elements unchanged
   * that need to stay the same and creating new ones for the ones that need to change.
   * NOTE: This method relies on having only one node in the whole xml structure with the name queue/classical/
   * exchange - the context of the node is not checked!
   * @param proposal
   * @return
   */
  def updateSubmissionsKey(proposal: Elem, key: String) : Node = {
    // worker  methods
    def updateNodes(nodes: Seq[Node]) : Seq[Node] = {
      for (node <- nodes) yield node match {
        case Elem(prefix, "queue" | "classical" | "exchange", attribs, scope, children @ _*) =>
          Elem(prefix, node.label, addKeyAttribute(attribs, key), scope, false, children : _*)
        case Elem(prefix, label, attribs, scope, children @ _*) =>
          Elem(prefix, label, attribs, scope, false, updateNodes(children) : _*)
        case text => text  // preserve text
      }
    }

    def addKeyAttribute(attribs: MetaData, key: String) = {
      // remove an already existing key and add/replace with calculated key
      val strippedAttribs = attribs remove("key")
      strippedAttribs append (new UnprefixedAttribute("key", key, strippedAttribs))
    }

    // recursion entry point
    updateNodes(proposal.seq)(0)
  }

  // add accept part to the response part of the submittin partner
  // note: submitting partner can only be determined based on the file name given by the submission server
  def updateAcceptPart(nodes: Seq[Node], partner: String) : Node = {
    // worker  methods
    def updateNodes(nodes: Seq[Node], parent: Node) : Seq[Node] = {
      for (node <- nodes) yield node match {
        case Elem(prefix, "response", attribs, scope, children @ _*) if ((parent\"partner").text.equals(partner)) =>
          Elem(prefix, node.label, attribs, scope, false, addAcceptElem(parent, children) : _*) // unroll the sequence -> pass all elements of sequence as individual parameters (confusing...??)
        case Elem(prefix, label, attribs, scope, children @ _*) =>
          Elem(prefix, label, attribs, scope, false, updateNodes(children, node) : _*)
        case text => text  // preserve text
      }
    }

    def addAcceptElem(parent: Node,  children: Seq[Node]) = {
      val time    = toTimeAmount((parent\"request"\"time")(0))
      val minTime = toTimeAmount((parent\"request"\"minTime")(0))
      val xml = {
<accept>
  <email>accept@accept.org</email>
  <ranking>{Math.round (Math.random * 30) + 1}</ranking>
  <recommend units="hr">{time.getValueInHours.toString}</recommend>
  <minRecommend units="hr">{minTime.getValueInHours.toString}</minRecommend>
  <poorWeather>false</poorWeather>
</accept>
      }
      val newChildren = children :+ xml
      newChildren
    }

    // recursion entry point
    updateNodes(nodes, nodes(0)) (0)
  }
  
  def toTimeAmount(n: Node) = {
    val unit = (n\"@units").text match {
      case "hr"    => TimeUnit.HR
      case "min"   => TimeUnit.MIN
      case "night" => TimeUnit.NIGHT
    }
    val amount = java.lang.Double.parseDouble(n.text)
    new TimeAmount(amount, unit)
  }

  object ProposalKey {
    def fromProposal(proposal: Elem) = {
      new ProposalKey(
        (proposal\"title").text,
        (proposal\"investigators"\"pi"\"firstName").text,
        (proposal\"investigators"\"pi"\"lastName").text
      )
    }
  }

  class ProposalKey (val title: String, val piFirstName: String, val piLastName: String) {
    override def hashCode =
      title.hashCode
    override def equals(that: Any) =
      that match {
        case that: ProposalKey => {
          (this.title equals that.title) &&
          (this.piFirstName equals that.piFirstName) &&
          (this.piLastName equals that.piLastName)
        }
        case _ => false
      }

    def toPseudoUUID = {
      var pool = title.hashCode.toHexString + piFirstName.hashCode.toHexString + piLastName.hashCode.toHexString
      while (pool.length < 32) pool += pool
      pool.substring(0,8) + "-" + pool.substring(8,12) + "-" + pool.substring(12,16) + "-" + pool.substring(16,20) + "-" + pool.substring(20,32)
    }
  }


}
