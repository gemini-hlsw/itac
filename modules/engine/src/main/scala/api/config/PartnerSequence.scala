package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.Partner

trait PartnerSequence{
  def sequence: LazyList[Partner]
}

