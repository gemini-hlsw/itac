package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.Partner
import xml.Elem


trait PartnerSequence{
  def sequence: Stream[Partner]
}

