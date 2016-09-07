package nl.rvanheest.test.feedback4s.commons

import nl.rvanheest.feedback4s.commons.Controllers
import org.junit.Test
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.{Observable, Subject}

import scala.language.postfixOps

class IntegralTest {

	@Test
	def testIntegralWithNoDT() = {
		val integral = Controllers.integralController[Int]()
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 3, 6, 10)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testIntegralWithSingleDT() = {
		val dt = Observable.just(2)
		val integral = Controllers.integralController[Int](dt)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(2, 6, 12, 20)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testIntegralWithMultipleDT() = {
		val dt = Subject[Int]()
		val integral = Controllers.integralController[Int](dt)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

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

		testObserver.assertValues(1, 3, 9, 21, 26, 32)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
