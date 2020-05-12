// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package org.slf4j.impl

import itac.util.Colors

/** Extend `SimpleLogger` to get some colors. Hacky, sorry. */
class ColoredSimpleLogger(name: String) extends SimpleLogger(name) {
  import ColoredSimpleLogger._

  override protected def renderLevel(level: Int): String =
    level match {
        case SimpleLogger.LOG_LEVEL_TRACE => "TRACE".colored(Colors.CYAN)
        case SimpleLogger.LOG_LEVEL_DEBUG => "DEBUG".colored(Colors.BLUE)
        case SimpleLogger.LOG_LEVEL_INFO  => "INFO ".colored(Colors.GREEN)
        case SimpleLogger.LOG_LEVEL_WARN  => "WARN ".colored(Colors.YELLOW) // n.b. ignores the [dumb] fact that [only] this label is configurable in the superclass
        case SimpleLogger.LOG_LEVEL_ERROR => "ERROR".colored(Colors.RED)
    }

}

object ColoredSimpleLogger {

  // java is dumb, have to call this before constructing an instance or it will NPE
  def init(): Unit =
    SimpleLogger.lazyInit()

  private implicit class StringOps(val s: String) extends AnyVal {
    def colored(color: String): String =
      s"$color$s${Colors.RESET}"
  }

}