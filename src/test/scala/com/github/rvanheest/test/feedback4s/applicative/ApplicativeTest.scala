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
package com.github.rvanheest.test.feedback4s.applicative

import com.github.rvanheest.feedback4s.Component
import org.junit.Assert.assertEquals
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.collection.mutable
import scala.language.postfixOps

class ApplicativeTest {

	@Test
	def testApplicative() = {
		val c1 = Component.create[Int, Int => Int](_ +)
		val c2 = Component.create[Int, Int](3 *)
		val applicative = c1 <*> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		applicative.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(4, 8, 12, 16)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testApplicativeLeft() = {
		val buffer = mutable.ListBuffer[Int]()
		val c1 = Component.create[Int, Double](_.toDouble)
		val c2 = Component.create[Int, Int](3 *).doOnNext(buffer += _)
		val applicative = c1 <* c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Double]

		applicative.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1.0, 2.0, 3.0, 4.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()

		// side effect from c2 was performed
		assertEquals(List(3, 6, 9, 12), buffer)
	}

	@Test
	def testApplicativeRight() = {
		val buffer = mutable.ListBuffer[Int]()
		val c1 = Component.create[Int, Int](3 *).doOnNext(buffer += _)
		val c2 = Component.create[Int, Double](_.toDouble)
		val applicative = c1 *> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Double]

		applicative.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1.0, 2.0, 3.0, 4.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()

		// side effect from c1 was performed
		assertEquals(List(3, 6, 9, 12), buffer)
	}

	@Test
	def testApplicativeReversed() = {
		val c1 = Component.create[Int, Int](3 *)
		val c2 = Component.create[Int, Int => Int](_ +)
		val applicative = c1 <**> c2
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		applicative.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(4, 8, 12, 16)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
