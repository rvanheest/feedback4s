package nl.rvanheest.test.feedback4s.arrow

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class FirstTest {

	@Test
	def testFirst() = {
		val c = Component.create[Int, Int](2 *)
		val first = c.first[String]
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		first.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onNext(3, "ghi")
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "abc"), (4, "def"), (6, "ghi"), (8, "jkl"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFirstWithError() = {
		val error = new Exception("this is a test exception")

		val c = Component.create[Int, Int](2 *)
		val first = c.first[String]
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		first.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onError(error)
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "abc"), (4, "def"))
		testObserver.assertError(error)
		testObserver.assertNotCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFirstWithCompletedEarly() = {
		val c = Component.create[Int, Int](2 *)
		val first = c.first[String]
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		first.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onCompleted()
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "abc"), (4, "def"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFirstWithStart() = {
		val c = Component[Int, Int](0 +: 1 +: _)
		val first = c.first[String]
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		first.run(input).subscribe(testObserver)

		input.onNext(2, "abc")
		input.onNext(3, "def")
		input.onNext(4, "ghi")
		input.onNext(5, "jkl")
		input.onCompleted()

		testObserver.assertValues((0, "abc"), (1, "def"), (2, "ghi"), (3, "jkl"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
