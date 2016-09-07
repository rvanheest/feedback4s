package nl.rvanheest.test.feedback4s.applicative

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class MapTest {

	@Test
	def testMap() = {
		val c = Component.create[Int, Int](2 *)
		val map = c.map(_ - 1)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		map.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 3, 5, 7)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testMapWithStartWith() = {
		val c = Component[Int, Int](0 +: 1 +: _)
		val map = c.map(_ - 1)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		map.run(input).subscribe(testObserver)

		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onNext(5)
		input.onCompleted()

		testObserver.assertValues(-1, 0, 1, 2, 3, 4)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
	}
}
