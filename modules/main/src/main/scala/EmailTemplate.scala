// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

trait EmailTemplate {
  def name:      String
  def titleTemplate: String
  def bodyTemplate:  String
}

sealed abstract case class EmailTemplateRef(filename: String) {
  // we "just know" that this is where the template file lives on the classpath
  val resourcePath = s"/email_templates/$filename"
}

object EmailTemplateRef {

  object NgoClassical        extends EmailTemplateRef("ngo_classical.vm")
  object NgoExchange         extends EmailTemplateRef("ngo_exchange.vm")
  object NgoJointClassical   extends EmailTemplateRef("ngo_joint_classical.vm")
  object NgoJointExchange    extends EmailTemplateRef("ngo_joint_exchange.vm")
  object NgoJointPoorWeather extends EmailTemplateRef("ngo_joint_poor_weather.vm")
  object NgoJointQueue       extends EmailTemplateRef("ngo_joint_queue.vm")
  object NgoPoorWeather      extends EmailTemplateRef("ngo_poor_weather.vm")
  object NgoQueue            extends EmailTemplateRef("ngo_queue.vm")
  object PiSuccessful        extends EmailTemplateRef("pi_successful.vm")
  object Unsuccessful        extends EmailTemplateRef("unsuccessful.vm")

  val all: List[EmailTemplateRef] =
    List(
      NgoClassical,
      NgoExchange,
      NgoJointClassical,
      NgoJointExchange,
      NgoJointPoorWeather,
      NgoJointQueue,
      NgoPoorWeather,
      NgoQueue,
      PiSuccessful,
      Unsuccessful,
    )

}
