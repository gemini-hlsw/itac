import cats.Monoid
import edu.gemini.tac.qengine.util.Time

package object itac {

  implicit val MonoidTime: Monoid[Time] =
    new Monoid[Time] {
      def combine(x: Time, y: Time): Time = x + y
      def empty: Time = Time.Zero
    }

}