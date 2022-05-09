// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

import cats.Monoid
import edu.gemini.tac.qengine.util.Time

package object itac {

  implicit val MonoidTime: Monoid[Time] =
    new Monoid[Time] {
      def combine(x: Time, y: Time): Time = x + y
      def empty: Time = Time.Zero
    }

}