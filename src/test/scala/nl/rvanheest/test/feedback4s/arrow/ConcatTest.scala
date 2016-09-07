package nl.rvanheest.test.feedback4s.arrow

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class ConcatTest {

	@Test
	def testConcat() = {
		val c1 = Component[Int, Int](_.scan(0)(_ + _))
		val c2 = Component.create[Int, Int](1 +)
		val concat = c1 >>> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		concat.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 2, 4, 7, 11)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testConcatStartWithInSubComponent() = {
		val c1 = Component[Int, Int](input => (0 +: input).scan(0)(_ + _))
		val c2 = Component.create[Int, Int](1 +)
		val concat = c1 >>> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		concat.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 1, 2, 4, 7, 11)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testConcatWithError() = {
		val error = new Exception("this is a test exception")

		val c1 = Component[Int, Int](_.scan(0)(_ + _))
		val c2 = Component.create[Int, Int](1 +)
		val concat = c1 >>> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		concat.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onError(error)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 2, 4)
		testObserver.assertError(error)
		testObserver.assertNotCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testConcatWithCompletedEarly() = {
		val c1 = Component[Int, Int](_.scan(0)(_ + _))
		val c2 = Component.create[Int, Int](1 +)
		val concat = c1 >>> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		concat.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onCompleted()
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 2, 4)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
