// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p2.rollover

import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.{CategorizedTime, ObservingConditions, Target}
import edu.gemini.tac.qengine.p2.ObservationId
import edu.gemini.tac.qengine.util.Angle
import edu.gemini.tac.qengine.util.Time
import scala.util.Try
import scalaz._, Scalaz._

/**
 * A class that represents a rollover time observation.  The time for each
 * rollover observation should be subtracted from the corresponding bins.
 */
case class RolloverObservation(
  partner: Partner,
  obsId: ObservationId,
  target: Target,
  conditions: ObservingConditions,
  time: Time
) extends CategorizedTime {
  def site: Site = obsId.site
}

object RolloverObservation {

  /**
   * Parse a `RolloverObservation` from an XML element, as provided by the OCS SPDB rollover
   * servlet. Such an element looks like this. Remaining time is in milliseconds.
   *
   *  <obs>
   *    <id>GS-2019B-Q-107-5</id>
   *    <partner>Argentina</partner>
   *    <target>
   *      <ra>122.252373 deg</ra>
   *      <dec>-61.302384 deg</dec>
   *    </target>
   *    <conditions>
   *      <cc>50</cc>
   *      <iq>70</iq>
   *      <sb>100</sb>
   *      <wv>100</wv>
   *    </conditions>
   *    <time>5497000</time>
   *  </obs
   *
   * You can fetch such a report yourself via
   *
   *     curl -i http://gsodb.gemini.edu:8442/rollover
   *
   * @return a RolloverObservation, or a message on failure.
   */
  def fromXml(o: scala.xml.Node, partners: List[Partner]): Either[String, RolloverObservation] =
    Try {

      def fail(field: String): Nothing =
        sys.error(s"Error parsing RolloverObservation: missing or invalid $field\n$o")

      val id   = ObservationId.parse((o \ "id").text).getOrElse(fail("observation id"))
      val p    = partners.find(_.fullName == (o \ "partner").text).getOrElse(fail("partner"))
      val time = Time.millisecs((o \ "time").text.toLong).toMinutes
      val ra   = new Angle((o \ "target" \ "ra" ).text.takeWhile(_ != ' ').toDouble, Angle.Deg)
      val dec  = new Angle((o \ "target" \ "dec").text.takeWhile(_ != ' ').toDouble, Angle.Deg)
      val t    = Target(ra, dec, None)
      val cc   = CloudCover   .values.find(_.percent == (o \ "conditions" \ "cc").text.toInt).getOrElse(fail("cc"))
      val iq   = ImageQuality .values.find(_.percent == (o \ "conditions" \ "iq").text.toInt).getOrElse(fail("iq"))
      val sb   = SkyBackground.values.find(_.percent == (o \ "conditions" \ "sb").text.toInt).getOrElse(fail("sb"))
      val wv   = WaterVapor   .values.find(_.percent == (o \ "conditions" \ "wv").text.toInt).getOrElse(fail("wv"))

      RolloverObservation(p, id, t, ObservingConditions(cc, iq, sb, wv), time)

    } .toEither.leftMap(_.getMessage)

}