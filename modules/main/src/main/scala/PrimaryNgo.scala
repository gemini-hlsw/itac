// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import edu.gemini.model.p1.mutable._
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory

object PrimaryNgo {

  case class Info(partner: NgoPartner, ngoEmail: Option[String])

  private def find(ngo: NgoSubmission, pi: PrincipalInvestigator): Option[Info] =
    if (Some(ngo.getPartner) == find(pi) || ngo.getPartnerLead.getEmail == pi.getEmail) {

      val ngoEmail =
        for {
          r <- Option(ngo.getResponse)
          a <- Option(r.getAccept)
          e <- Option(a.getEmail) if e.trim.nonEmpty
        } yield e

      Some(Info(ngo.getPartner, ngoEmail))

    } else None

  private def find(ngos: List[NgoSubmission], pi: PrincipalInvestigator): Option[Info] =
    ngos.map(find(_, pi)).find(_.isDefined).flatten

  private def find(pc: ClassicalProposalClass, pi: PrincipalInvestigator): Option[Info] =
    find(pc.getNgo.asScala.toList, pi)

  private def find(pc: ExchangeProposalClass, pi: PrincipalInvestigator): Option[Info] =
    find(pc.getNgo.asScala.toList, pi)

  private def find(pc: QueueProposalClass, pi: PrincipalInvestigator): Option[Info] =
    find(pc.getNgo.asScala.toList, pi)

  private def find(pcc: ProposalClassChoice, pi: PrincipalInvestigator): Option[Info] = {

    def go[/* nullable */ A](
      f: ProposalClassChoice => A)(
      g: (A, PrincipalInvestigator) => Option[Info]
    ): Option[Info] =
      Option(f(pcc)).flatMap(g(_, pi))

    go(_.getClassical)(find) <+>
    go(_.getExchange)(find)  <+>
    go(_.getQueue)(find)

  }

  private def find(pi: PrincipalInvestigator): Option[Info] =
    pi.getAddress.getCountry.trim.toUpperCase match {
      case "ARGENTINA"                => Some(Info(NgoPartner.AR, None))
      case "AUSTRALIA"                => Some(Info(NgoPartner.AU, None))
      case "BRASIL"                   |
           "BRAZIL"                   => Some(Info(NgoPartner.BR, None))
      case "CANADA"                   => Some(Info(NgoPartner.CA, None))
      case "CHILE"                    => Some(Info(NgoPartner.CL, None))
      case "KOREA"                    |
           "REPUBLIC OF KOREA"        => Some(Info(NgoPartner.KR, None))
      case "UNIVERSITY OF HAWAII"     => Some(Info(NgoPartner.UH, None))
      case "UNITED STATES"            |
           "USA"                      |
           "US"                       |
           "UNITED STATES OF AMERICA" => Some(Info(NgoPartner.US, None))
      case s =>
        LoggerFactory.getLogger("edu.gemini.itac").debug(s"No NGO partner for ${s}.")
        None
    }

  def find(p: Proposal): Option[Info] = {
    find(p.getProposalClass, p.getInvestigators.getPi)
  }

}