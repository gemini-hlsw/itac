package edu.gemini.tac.persistence.phase1.queues

import org.hibernate.annotations.{NamedQueries, NamedQuery}
import edu.gemini.tac.persistence.queues.Queue
import javax.persistence._

import edu.gemini.tac.persistence.Partner

import scala.collection.JavaConverters._
import org.apache.commons.lang.Validate

import scala.beans.BeanProperty

/**
 * The actual partner proportions used in the generation of a particular queue.
 *
 *
 */

@Entity
@Table(name = "partner_percentage")
@NamedQueries(Array(
  new NamedQuery(name = "PartnerPercentage.allForQueue",  query = "from PartnerPercentage p where p.queue.id = :queueId"),
  new NamedQuery(name = "PartnerPercentage.forId",  query = "from PartnerPercentage p where p.id = :id")
))
class PartnerPercentage(queue0 : Queue, partner0 : Partner, proportion0 : Double) {
  //No-args constructor for Hibernate
  def this() = this(null, null, null.asInstanceOf[Double])

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id : Long = _

  @BeanProperty
  @ManyToOne
  @JoinColumn(name="queue_id", nullable=false, updatable=false)
  var queue : Queue = queue0

  @BeanProperty
  @OneToOne
  @JoinColumn(name="partner_id", nullable=false, updatable=false)
  var partner : Partner = partner0

  @BeanProperty
  @Column(name = "percentage")
  var percentage : Double = proportion0
  Validate.isTrue(percentage >= 0.0 && percentage <= 1.0, "Expected partner percentage in range [0,1]. Got: " + percentage)
}

object PartnerPercentage {
  def fromHours(queue: Queue,  hrsByPartner : java.util.Map[Partner,  java.lang.Double]) : java.util.List[PartnerPercentage] = {
    val totalHours = hrsByPartner.values().asScala.foldLeft(0.0)((accum,hr) => accum + hr)
    hrsByPartner.keySet().asScala.map { partner : Partner =>
      val proportion = hrsByPartner.get(partner).doubleValue() / totalHours
      new PartnerPercentage(queue, partner, proportion)
    }.toList.asJava
  }
}