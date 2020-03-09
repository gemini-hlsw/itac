//package edu.gemini.tac.qengine.app

//import edu.gemini.tac.qengine.util.Time
//import edu.gemini.tac.qengine.api.queue.ProposalQueue
//import edu.gemini.tac.qengine.p1._
//import edu.gemini.tac.qengine.ctx.{Partner, Context, Site}

/**
* A report that contains the most relevant information from the old "*_final"
* spreadsheet "queue" tab.  Useful for comparing the queue engine results
* with the official results.
*/
/*
case class QueueReport(queue: ProposalQueue, coll: ProposalCollection, ctx: Context) {

    sealed class NumericBand(val bandNumber: Int) extends Ordered[NumericBand] {
    def compare(that: NumericBand): Int = bandNumber - that.bandNumber
  }

  object NumericBand {
    case object B1 extends NumericBand(1)
    case object B2 extends NumericBand(2)
    case object B3 extends NumericBand(3)
    case object B4 extends NumericBand(4)

    val values = List(B1, B2, B3, B4)

    def apply(band: QueueBand): NumericBand = values.find(_.bandNumber == band.number).get
    def pw: NumericBand = B4
  }

  case class ProposalData(prop: Proposal, band: NumericBand) {
    val reference: Option[String] = prop match {
      case cp: CoreProposal                          => Some(prop.id.reference)
      case jp: JointProposal if jp.ntacs.length == 1 => Some(jp.ntacs(0).reference)
      case dp: DelegatingProposal                    => sys.error("Unhandled case")
      case _ => None
    }

    val country: List[Partner] = prop match {
      case cp: CoreProposal       => List(prop.ntac.partner)
      case jp: JointProposal      => jp.ntacs.sorted(Ntac.MasterOrdering).map(_.partner)
      case dp: DelegatingProposal => sys.error("Unhandled case")
    }

    val time: Time = prop match {
      case cp: CoreProposal       => prop.ntac.awardedTime
      case jp: JointProposal      => jp.ntacs.map(_.awardedTime).foldLeft(Time.ZeroHours)(_ + _)
      case dp: DelegatingProposal => sys.error("Unhandled case")
    }

    val principal: Proposal =
      prop match {
        case cp: CoreProposal       => prop
        case jp: JointProposal      => jp.core
        case dp: DelegatingProposal => sys.error("Unhandled case")
      }

    // Fish out the primary investigator's last name.  If anything goes wrong,
    // punt.
    val pi: String =
      try {
        Option(coll.p1(principal.id).getInvestigators.getPi.getLastName).getOrElse("")
      } catch {
        case ex: Exception => ""
      }

    // Is this a partially accepted joint?  If so, list the country and ids
    // that were rejected.
    val rejectedPartIds: List[String] = prop match {
      case cp: CoreProposal       => Nil
      case jp: JointProposal      => {
        val survivingParts = jp.toParts
        val originalParts  = coll.joints(prop.jointId.get)

        val survivingIds = survivingParts.map(_.ntac.reference).toSet
        val originalIds  = originalParts.map(_.ntac.reference).toSet

        val rejectedIds  = originalIds -- survivingIds
        if (rejectedIds.size == 0)
          Nil
        else {
          val rejectedParts = originalParts.filter(p => !survivingIds.contains(p.ntac.reference))
          rejectedParts.map(p => "%s (%s)".format(p.ntac.partner, p.ntac.reference))
        }
      }
      case dp: DelegatingProposal => sys.error("Unhandled case")
    }


    private def countryString: String = country.map(_.id).mkString("/")

    override def toString: String =
      "%25s %15s %20s %5.1f %3d".format(reference.getOrElse(""), countryString, pi, time.toHours.value, band.bandNumber)
  }

  object GemReferenceOrdering extends Ordering[ProposalData] {
    // band, followed by reverse name (reverse alphabetical)
    def compare(pd1: ProposalData, pd2: ProposalData): Int = {
      val res = pd1.band.compare(pd2.band)
      if (res == 0) pd2.pi.compareTo(pd1.pi) else res
    }
  }

  case class Line(data: ProposalData, gemRef: String) {
    override def toString: String = "%s %15s   %s".format(data.toString, gemRef, data.rejectedPartIds.mkString(","))
  }

  private def refSitePrefix(ctx: Context): String =
    ctx.getSite match {
      case Site.north => "GN"
      case Site.south => "GS"
    }

  private val prefix: String =
    "%s-%s-Q-".format(refSitePrefix(ctx), ctx.getSemester)

  val lines: List[Line] = {
    val bandedQueue = queue.bandedQueue
    val all = QueueBand.values.map {
      band => bandedQueue(band).map(ProposalData(_, NumericBand(band)))
    }
    all.flatten.sorted(GemReferenceOrdering).zipWithIndex map {
      case (pdata, index) => Line(pdata, prefix + (index + 1))
    }
  }
}
*/