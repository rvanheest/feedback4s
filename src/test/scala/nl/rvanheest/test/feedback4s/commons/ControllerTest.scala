package nl.rvanheest.test.feedback4s.commons

import java.util.concurrent.CountDownLatch

import nl.rvanheest.feedback4s.commons.Controllers
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class ControllerTest {

	@Test
	def testPIController() = {
		val pi = Controllers.piController(2.0, 0.5)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		pi.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(2.0)
		input.onNext(3.0)
		input.onNext(4.0)
		input.onCompleted()

		testObserver.awaitTerminalEvent()
		testObserver.assertValues(2.5, 5.5, 9.0, 13.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testPIControllerWithMultipleDT() = {
		val dt = Subject[Int]()
		val pi = Controllers.piController(0, 1, dt)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		pi.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		dt.onNext(2)
		input.onNext(3)
		dt.onNext(3)
		input.onNext(4)
		dt.onNext(1)
		input.onNext(5)
		input.onNext(6)
		input.onCompleted()

		testObserver.awaitTerminalEvent()
		testObserver.assertValues(1, 3, 9, 21, 26, 32)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testPIDController() = {
		val pid = Controllers.pidController(2.0, 0.5, 0.8)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		pid.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(2.0)
		input.onNext(3.0)
		input.onNext(4.0)
		input.onCompleted()

		testObserver.awaitTerminalEvent()
		testObserver.assertValues(3.3, 6.3, 9.8, 13.8)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
