package edu.gemini.tac.persistence.phase1.queues

import org.junit.{Assert, Test}
import edu.gemini.tac.persistence.queues.Queue
import edu.gemini.tac.persistence.Partner
import java.util.{Collections, HashMap}
import scala.collection.JavaConverters._


class PartnerPercentageTest {
   class QueueStub extends Queue{
     override def getTotalTimeAvailable = 1000
   }

   @Test def canCreateFromMap() {
     val queueStub : QueueStub = new QueueStub()
     val map : java.util.Map[Partner, java.lang.Double] = new java.util.HashMap[Partner, java.lang.Double]()

     val p0 = new Partner()
     p0.setPartnerCountryKey("A")
     map.put(p0, new java.lang.Double(600.0))
     val p1 = new Partner()
     p1.setPartnerCountryKey("B")
     map.put(p1, new java.lang.Double(400.0))
     val pps = PartnerPercentage.fromHours(queueStub, map)
     Assert.assertEquals(2, pps.size())
     pps.asScala.map { pp =>
      pp.getPartner().getPartnerCountryKey() match {
        case "A" => Assert.assertEquals(0.60, pp.getPercentage(), Double.MinValue)
        case "B" => Assert.assertEquals(0.40, pp.getPercentage(), Double.MinValue)
        case _ => Assert.fail()
      }
     }
   }
}