package nl.rvanheest.test.feedback4s.commons

import nl.rvanheest.feedback4s.commons.Controllers
import org.junit.Test
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.{Observable, Subject}

class DerivativeTest {

	@Test
	def testDerivativeWithNoDT() = {
		val derivative = Controllers.derivativeController[Double]()
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		derivative.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(6.0)
		input.onNext(3.0)
		input.onNext(9.0)
		input.onCompleted()

		testObserver.assertValues(1.0, 5.0, -3.0, 6.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testDerivativeWithSingleDT() = {
		val dt = Observable.just(2.0)
		val derivative = Controllers.derivativeController[Double](dt)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		derivative.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(6.0)
		input.onNext(3.0)
		input.onNext(9.0)
		input.onCompleted()

		testObserver.assertValues(0.5, 2.5, -1.5, 3.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testDerivativeWithMultipleDT() = {
		val dt = Subject[Double]()
		val derivative = Controllers.derivativeController[Double](dt)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		derivative.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(6.0)
		dt.onNext(2.0)
		input.onNext(3.0)
		dt.onNext(3.0)
		input.onNext(9.0)
		input.onCompleted()

		testObserver.assertValues(1.0, 5.0, -1.5, 2.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
