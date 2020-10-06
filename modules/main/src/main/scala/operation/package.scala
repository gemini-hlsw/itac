// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import java.nio.file.Path
import java.nio.file.Paths

package object operation {

  implicit def string2path(s: String): Path =
    Paths.get(s)

}
