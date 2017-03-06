package edu.gemini.tac.service

import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import scala.collection.JavaConverters._
import org.hibernate.{Query, Session, SessionFactory}
import edu.gemini.tac.persistence.{Partner, Proposal}
import java.util.Properties
import java.io.{IOException, InputStream}
import edu.gemini.tac.persistence.phase1.{EphemerisElement, Target, SiderealTarget, NonsiderealTarget}
import org.apache.log4j.{Logger, Level}
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence

object HibernateSource {
  private def LOGGER: Logger = Logger.getLogger(HibernateSource.getClass.toString)

  private def sessionFactory: SessionFactory = {
    val beanFactory = new XmlBeanFactory(new ClassPathResource("/data-applicationContext.xml"))
    val ppc = new PropertyPlaceholderConfigurer()
    ppc.setLocation(new ClassPathResource("/itac.properties"))

    //    val properties: Properties = new Properties
    //    val is: InputStream = getClass.getResourceAsStream("/itac-qa.properties")
    //    try {
    //      properties.load(is)
    //      is.close
    //    }
    //    catch {
    //      case e: IOException => {
    //        throw new RuntimeException(e)
    //      }
    //    }
    //
    //    ppc.setProperties(properties)

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

  def proposals(committeeName: String): List[Proposal] = {
    var proposals: java.util.List[Proposal] = null
    using {
      session =>
        val query: Query = session.getNamedQuery("proposal.proposalsForQueueAnalysis").setParameter("committeeName", committeeName)
        proposals = query.list.asInstanceOf[java.util.List[Proposal]]
        proposals.asScala.map {
          p =>
          //Block pulls in various elements in the session, so Hibernate ties them together (TODO: Switch to query-based)
            val p1p = p.getPhaseIProposal
            val p1pString = p1p.toString
            val is = p.getPhaseIProposal.getInvestigators
            val pi = is.getPi.getLastName
            val ngos = p.getPhaseIProposal.getSubmissions.asScala
            ngos.map(ngo => ngo)
            val bs = p.getPhaseIProposal.getBlueprints.asScala
            bs.map {
              b =>
                val s = b.toString //Draws in a lot of LIEs (filters, etc.) from descendant classes
                s
            }
            val ks = p.getPhaseIProposal.getKeywords.asScala
            ks.map(k => k)
            val os = p.getPhaseIProposal.getObservations.asScala
            os.map {
              o =>
                val t: Target = o.getTarget
                val t2 = t.copy()
                o.setTarget(t2)
                t2 match {
                  case t1: SiderealTarget => t1
                  case nst: NonsiderealTarget => {
                    val ees = nst.getEphemeris
                    ees.asScala.map(e => e)
                  }
                  case other => LOGGER.log(Level.WARN, "Unrecognized Target of type " + other.getClass.toString)
                }
            }
            p.getPhaseIProposal.getTargets.asScala.map(t => t)
            p.getPhaseIProposal.getConditions.asScala.map(c => c)
        }
        proposals
    }
    proposals.asScala.toList
  }

  def partner(key: String): Partner = {
    partners.find(_.getPartnerCountryKey == key).get
  }

  //Lazy initialize -- don't hit the DB every time
  var _partners: List[Partner] = null
  def partners: List[Partner] = {
    _partners == null match {
      case true => {
        var ps: java.util.List[Partner] = null
        using {
          session =>
            val query = session.getNamedQuery("partner.findAllPartners")
            ps = query.list().asInstanceOf[java.util.List[Partner]]
        }
        _partners = ps.asScala.toList
      }
      case false => //Nothing
    }
    _partners
  }

  def partnerSequence(committeeId: Int): PartnerSequence = {
    var ps: PartnerSequence = null
    using {
      session =>
        val query = session.getNamedQuery("partnerSequence.forCommittee").setParameter("committeeId", committeeId)
        ps = query.uniqueResult().asInstanceOf[PartnerSequence]
    }
    ps
  }
}
