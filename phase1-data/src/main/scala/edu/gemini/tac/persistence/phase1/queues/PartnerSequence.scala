package edu.gemini.tac.persistence.phase1.queues

import org.hibernate.annotations.{NamedQueries, NamedQuery}
import edu.gemini.tac.persistence.Committee

import javax.persistence._

import scala.beans.BeanProperty


/**
 * Holds the sequence of partners for a given committee
 *
 */
@Entity
@Table(name = "partner_sequence")
@NamedQueries(Array(
  new NamedQuery(name = "PartnerSequence.forName",  query = "from PartnerSequence p where p.name = :sequence_name and p.committee.id = :committeeId"),
  new NamedQuery(name = "PartnerSequence.forId",  query = "from PartnerSequence p where p.id = :id"),
  new NamedQuery(name = "PartnerSequence.allForCommittee", query = "from PartnerSequence p where p.committee.id = :committee_id")
))
class PartnerSequence(committee0 : Committee,  name0 : String, csv0 : String, repeat0 : Boolean) {
  def this() = this(null, null, null, true)

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id : Long = _

  @BeanProperty
  @OneToOne
  var committee : Committee = committee0

  @BeanProperty
  @Column(name = "name")
  var name : String = name0

  @BeanProperty
  @Column(name = "csv")
  var csv : String = csv0

  @BeanProperty
  @Column(name = "repeat")
  var repeat : Boolean = repeat0
}
