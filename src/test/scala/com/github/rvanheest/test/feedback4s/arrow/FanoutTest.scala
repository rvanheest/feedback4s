/**
 * Copyright (C) 2016 Richard van Heest (richard.v.heest@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rvanheest.test.feedback4s.arrow

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class FanoutTest {

	@Test
	def testFanout() = {
		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component.create[Int, Int](3 *)
		val fanout = c1 &&& c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[(Int, Int)]

		fanout.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues((2, 3), (4, 6), (6, 9), (8, 12))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFanoutWithError() = {
		val error = new Exception("this is a test exception")

		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component.create[Int, Int](3 *)
		val fanout = c1 &&& c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[(Int, Int)]

		fanout.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onError(error)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues((2, 3), (4, 6))
		testObserver.assertError(error)
		testObserver.assertNotCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFanoutWithCompletedEarly() = {
		val c1 = Component.create[Int, Int](2 *)
		val c2 = Component.create[Int, Int](3 *)
		val fanout = c1 &&& c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[(Int, Int)]

		fanout.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onCompleted()
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues((2, 3), (4, 6))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFanoutWithStartWiths() = {
		val c1 = Component[Int, Int](1 +: 2 +: _)
		val c2 = Component[Int, Int](3 +: 4 +: _)
		val fanout = c1 &&& c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[(Int, Int)]

		fanout.run(input).subscribe(testObserver)

		input.onNext(5)
		input.onNext(6)
		input.onNext(7)
		input.onNext(8)
		input.onCompleted()

		testObserver.assertValues((1, 3), (2, 4), (5, 5), (6, 6), (7, 7), (8, 8))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFanoutWithStartWithsNotAligned() = {
		val c1 = Component[Int, Int](1 +: 2 +: _)
		val c2 = Component[Int, Int](3 +: _)
		val fanout = c1 &&& c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[(Int, Int)]

		fanout.run(input).subscribe(testObserver)

		input.onNext(5)
		input.onNext(6)
		input.onNext(7)
		input.onNext(8)
		input.onCompleted()

		testObserver.assertValues((1, 3), (2, 5), (5, 6), (6, 7), (7, 8))
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
