package edu.gemini.tac.qengine.p1

import xml.Elem

trait Var[T <: Var[T]] extends Ordered[T] {
  val prefix: String
  val percent: Int

  def compare(that: T): Int = this.percent - that.percent
  override def toString: String = prefix + (if (percent < 100) percent else "Any")
  def toXml = new Elem(null, prefix, scala.xml.Null, scala.xml.TopScope, true, scala.xml.Text(percent.toString))

}

sealed class CloudCover(val percent: Int) extends Var[CloudCover] {
  val prefix = "CC"

}

object CloudCover {
  case object CC50  extends CloudCover( 50)
  case object CC70  extends CloudCover( 70)
  case object CC80  extends CloudCover( 80)
  case object CCAny extends CloudCover(100)
  val values = List(CC50, CC70, CC80, CCAny)
}

sealed class ImageQuality(val percent: Int) extends Var[ImageQuality] {
  val prefix = "IQ"
}

object ImageQuality {
  case object IQ20  extends ImageQuality( 20)
  case object IQ70  extends ImageQuality( 70)
  case object IQ85  extends ImageQuality( 85)
  case object IQAny extends ImageQuality(100)
  val values = List(IQ20, IQ70, IQ85, IQAny)
}

sealed class SkyBackground(val percent: Int) extends Var[SkyBackground] {
  val prefix = "SB"
}

object SkyBackground {
  case object SB20  extends SkyBackground( 20)
  case object SB50  extends SkyBackground( 50)
  case object SB80  extends SkyBackground( 80)
  case object SBAny extends SkyBackground(100)
  val values = List(SB20, SB50, SB80, SBAny)
}

sealed class WaterVapor(val percent: Int) extends Var[WaterVapor] {
  val prefix = "WV"
}

object WaterVapor {
  case object WV20  extends WaterVapor( 20)
  case object WV50  extends WaterVapor( 50)
  case object WV80  extends WaterVapor( 80)
  case object WVAny extends WaterVapor(100)
  val values = List(WV20, WV50, WV80, WVAny)
}

object ObsConditions {
  val AnyConditions = ObsConditions(CloudCover.CCAny, ImageQuality.IQAny,
                                    SkyBackground.SBAny, WaterVapor.WVAny)
}

case class ObsConditions(cc: CloudCover    = CloudCover.CCAny,
                         iq: ImageQuality  = ImageQuality.IQAny,
                         sb: SkyBackground = SkyBackground.SBAny,
                         wv: WaterVapor    = WaterVapor.WVAny) {
  def isPoorWeather: Boolean =
    (wv == WaterVapor.WVAny) && ((cc >= CloudCover.CC80) || ((iq == ImageQuality.IQAny) && (cc > CloudCover.CC50)))

  override def toString: String = "%s,%s,%s,%s".format(cc, iq, sb, wv)

  def toXml = <ObsConditions>
    {cc.toXml}
    {iq.toXml}
    {sb.toXml}
    {wv.toXml}
    </ObsConditions>
}