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
package com.github.rvanheest.test.feedback4s.applicative

import com.github.rvanheest.feedback4s.Component
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
