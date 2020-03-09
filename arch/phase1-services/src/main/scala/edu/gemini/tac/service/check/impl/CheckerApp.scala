package edu.gemini.tac.service.check.impl

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.ClassPathResource

import edu.gemini.tac.persistence.Proposal
import edu.gemini.tac.persistence.RichProposal._
import org.hibernate.{Session, SessionFactory}

import collection.JavaConverters._
import edu.gemini.tac.service.check.ProposalIssue
import edu.gemini.tac.persistence.joints.JointProposal
import edu.gemini.tac.persistence.phase1.submission.{Submission, NgoSubmission}

/**
 * This is a simple command line app that queries the database for all
 * proposals, runs the proposal checks, and prints the results.
 */
object CheckerApp {

  private def sessionFactory: SessionFactory = {
    val beanFactory = new XmlBeanFactory(new ClassPathResource("/data-applicationContext.xml"))
    val ppc = new PropertyPlaceholderConfigurer()
    ppc.setLocation(new ClassPathResource("/itac-jetty.properties"))
    ppc.postProcessBeanFactory(beanFactory)

    beanFactory.getBean("sessionFactory").asInstanceOf[SessionFactory]
  }

  private def using(block: Session => Unit) {
    val fact = sessionFactory
    val session = fact.openSession
    try {
      block(session)
    } finally {
      session.close()
    }
  }

  private def formatId(sub: Submission): Option[String] =
    for {
      partner <- Option(sub.getPartner)
      key     <- Option(partner.getPartnerCountryKey())
      ref     <- Option(sub.getReceipt.getReceiptId)
    } yield "%s(%s)".format(key, ref)

  private def proposalId(p: Proposal): String =
    p match {
      case jp: JointProposal =>
        (for {
          Some(id) <- p.submissionsList map { sub => formatId(sub) }
        } yield id) mkString("Joint {", ",", "}")
      case _ =>
        p.flaggedSubmission flatMap {
          sub => formatId(sub)
        } getOrElse "n/a"
    }

  private def formatSeverity(sev: ProposalIssue.Severity): String =
    sev match {
      case ProposalIssue.Severity.error   => "X"
      case ProposalIssue.Severity.warning => "!"
      case _ => "?"
    }

  private def formatIssue(issue: ProposalIssue): String =
    "%s %s".format(formatSeverity(issue.severity), issue.message)

  private def printIssues(id: String, issues: List[ProposalIssue]) {
    println(id)
    issues foreach { issue => println("\t%s".format(formatIssue(issue))) }
  }

  def main(args: Array[String]) {
    using { session =>
      println("* Gathering proposals ...")
      val query = session.createQuery("from Proposal")
      val proposals = query.list;

      println("* Checking proposals ...")
      val start   = System.currentTimeMillis
      val issues  = ProposalChecker.exec(proposals.asInstanceOf[java.util.List[Proposal]])
      val totalms = System.currentTimeMillis - start

      println("* Proposal Issues:")
      val issueMap = issues.asScala.toList.groupBy(issue => proposalId(issue.proposal))
      issueMap.keys.toList.sorted foreach {
        id => printIssues(id, issueMap(id))
      }

      val msg = "Checked %d proposals in %.1f sec.  Found %d issues in %d proposals.".format(proposals.size, totalms/1000.0, issues.size, issueMap.keys.size)
      val brd = "-" * msg.length
      println("\n%s\n%s\n%s\n".format(brd,msg,brd))


      /*
      val lst    = issues.asScala.toList
      val tlst   = lst.filter(issue => issue.check.name == TargetsCheck.name)
      val tissue = tlst.head

      val prop = tissue.proposal
      println("reference...: " + proposalId(prop))
      println("PROPOSAL....: " + prop.getId)
      val doc = prop.getDocument
      println("DOCUMENT....: " + doc.getId)
      val com = doc.getCommon
      println("COMMON......: " + com.getId)
      val cat = com.getTargetCatalog
      println("CATALOG.....: " + cat.getId)
      println("target count: " + cat.getTargets.size)
      */
    }
  }
}
