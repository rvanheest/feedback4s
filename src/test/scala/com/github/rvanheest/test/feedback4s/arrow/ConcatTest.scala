/**
 * Copyright (C) 2016 Richard van Heest
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rvanheest.test.feedback4s.arrow

import com.github.rvanheest.feedback4s.Component
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
