package nl.rvanheest.test.feedback4s

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class CreateTest {

	@Test
	def testCreate() = {
		val integral = Component.create[Int, Int](2 *)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(2, 4, 6, 8)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testApplyTransform() = {
		val integral = Component[Int, Int](_.map(2 *))
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(2, 4, 6, 8)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testApplyStartWith() = {
		val integral = Component[Int, Int](1 +: _)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 1, 2, 3, 4)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testCreateWithError() = {
		val error = new Exception("this is a test exception")

		val integral = Component.create[Int, Int](2 *)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onError(error)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(2, 4)
		testObserver.assertError(error)
		testObserver.assertNotCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testCreateWithCompletedEarly() = {
		val integral = Component.create[Int, Int](2 *)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onCompleted()
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(2, 4)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}