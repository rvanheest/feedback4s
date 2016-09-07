package nl.rvanheest.test.feedback4s.arrow

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class SplitTest {

	@Test
	def testSplit() = {
		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component.create[String, String](_.reverse)
		val split = c1 *** c2
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		split.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onNext(3, "ghi")
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "cba"), (4, "fed"), (6, "ihg"), (8, "lkj"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSplitWithError() = {
		val error = new Exception("this is a test exception")

		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component.create[String, String](_.reverse)
		val split = c1 *** c2
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		split.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onError(error)
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "cba"), (4, "fed"))
		testObserver.assertError(error)
		testObserver.assertNotCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSplitWithCompletedEarly() = {
		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component.create[String, String](_.reverse)
		val split = c1 *** c2
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		split.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onCompleted()
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "cba"), (4, "fed"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSplitWithStartFirst() = {
		val c1 = Component[Int, Int](0 +: 1 +: _)
		val c2 = Component.create[String, String](_.reverse)
		val split = c1 *** c2
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		split.run(input).subscribe(testObserver)

		input.onNext(2, "abc")
		input.onNext(3, "def")
		input.onNext(4, "ghi")
		input.onNext(5, "jkl")
		input.onCompleted()

		testObserver.assertValues((0, "cba"), (1, "fed"), (2, "ihg"), (3, "lkj"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSplitWithStartSecond() = {
		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component[String, String]("uvw" +: "xyz" +: _)
		val split = c1 *** c2
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		split.run(input).subscribe(testObserver)

		input.onNext(1, "abc")
		input.onNext(2, "def")
		input.onNext(3, "ghi")
		input.onNext(4, "jkl")
		input.onCompleted()

		testObserver.assertValues((2, "uvw"), (4, "xyz"), (6, "abc"), (8, "def"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testSplitWithStart() = {
		val c1 = Component[Int, Int](input => (0 +: 1 +: input).map(2 *))
		val c2 = Component[String, String](input => ("uvw" +: "xyz" +: input).map(_.reverse))
		val split = c1 *** c2
		val input = Subject[(Int, String)]()
		val testObserver = TestSubscriber[(Int, String)]

		split.run(input).subscribe(testObserver)

		input.onNext(2, "abc")
		input.onNext(3, "def")
		input.onNext(4, "ghi")
		input.onNext(5, "jkl")
		input.onCompleted()

		testObserver.assertValues((0, "wvu"), (2, "zyx"), (4, "cba"), (6, "fed"), (8, "ihg"), (10, "lkj"))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
