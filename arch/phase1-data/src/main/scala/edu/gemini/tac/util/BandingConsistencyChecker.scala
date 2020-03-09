package edu.gemini.tac.util

import edu.gemini.tac.persistence.queues.Queue
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import java.util.Properties
import java.io.{IOException, InputStream}
import org.hibernate.{Query, Session, SessionFactory}
import scala.collection.JavaConverters._
import collection.mutable.Buffer
import org.apache.log4j.Logger

/**
 * Validate the Bandings database elements versus the Queue Log
 *
 */

object BandingConsistencyChecker {
  private def LOGGER: Logger = Logger.getLogger(BandingConsistencyChecker.getClass.toString)

  def main(args: Array[String]) {
    val queueId = 1094949L
    val entries = logForQueue(queueId)
    entries.map(e => println(e))
  }

  def logForQueue(queueId: Long): Buffer[_] = {
    var es : Buffer[_] = null
    using {
      session =>
      val query: Query = session.createQuery("from LogEntry where queue_id = " + queueId)
      es = query.list().asScala
    }
    es
  }

  private def sessionFactory: SessionFactory = {
    val beanFactory = new XmlBeanFactory(new ClassPathResource("/data-applicationContext.xml"))
    val ppc = new PropertyPlaceholderConfigurer()

    val properties: Properties = new Properties
    val is: InputStream = getClass.getResourceAsStream("/itac-jetty.properties")
    try {
      properties.load(is)
      is.close
    }
    catch {
      case e: IOException => {
        throw new RuntimeException(e)
      }
    }

    ppc.setProperties(properties)

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
}