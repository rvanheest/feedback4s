package nl.rvanheest.test.feedback4s.arrow

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class SecondTest {

	@Test
	def testSecond() = {
		val c = Component.create[Int, Int](2 *)
		val second = c.second[String]
		val input = Subject[(String, Int)]()
		val testObserver = TestSubscriber[(String, Int)]

		second.run(input).subscribe(testObserver)

		input.onNext("abc", 1)
		input.onNext("def", 2)
		input.onNext("ghi", 3)
		input.onNext("jkl", 4)
		input.onCompleted()

		testObserver.assertValues(("abc", 2), ("def", 4), ("ghi", 6), ("jkl", 8))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSecondWithError() = {
		val error = new Exception("this is a test exception")

		val c = Component.create[Int, Int](2 *)
		val second = c.second[String]
		val input = Subject[(String, Int)]()
		val testObserver = TestSubscriber[(String, Int)]

		second.run(input).subscribe(testObserver)

		input.onNext("abc", 1)
		input.onNext("def", 2)
		input.onError(error)
		input.onNext("jkl", 4)
		input.onCompleted()

		testObserver.assertValues(("abc", 2), ("def", 4))
		testObserver.assertError(error)
		testObserver.assertNotCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSecondWithCompletedEarly() = {
		val c = Component.create[Int, Int](2 *)
		val second = c.second[String]
		val input = Subject[(String, Int)]()
		val testObserver = TestSubscriber[(String, Int)]

		second.run(input).subscribe(testObserver)

		input.onNext("abc", 1)
		input.onNext("def", 2)
		input.onCompleted()
		input.onNext("jkl", 4)
		input.onCompleted()

		testObserver.assertValues(("abc", 2), ("def", 4))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSecondWithStart() = {
		val c = Component[Int, Int](0 +: 1 +: _)
		val second = c.second[String]
		val input = Subject[(String, Int)]()
		val testObserver = TestSubscriber[(String, Int)]

		second.run(input).subscribe(testObserver)

		input.onNext("abc", 2)
		input.onNext("def", 3)
		input.onNext("ghi", 4)
		input.onNext("jkl", 5)
		input.onCompleted()

		testObserver.assertValues(("abc", 0), ("def", 1), ("ghi", 2), ("jkl", 3))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
