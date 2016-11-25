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
package nl.rvanheest.test.feedback4s.commons

import nl.rvanheest.feedback4s.commons.Controllers
import org.junit.Test
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.{Observable, Subject}

import scala.language.postfixOps

class IntegralTest {

	@Test
	def testIntegralWithNoDT() = {
		val integral = Controllers.integralController[Int]()
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(1, 3, 6, 10)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testIntegralWithSingleDT() = {
		val dt = Observable.just(2)
		val integral = Controllers.integralController[Int](dt)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		input.onNext(3)
		input.onNext(4)
		input.onCompleted()

		testObserver.assertValues(2, 6, 12, 20)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testIntegralWithMultipleDT() = {
		val dt = Subject[Int]()
		val integral = Controllers.integralController[Int](dt)
		val input = Subject[Int]()
		val testObserver = TestSubscriber[Int]

		integral.run(input).subscribe(testObserver)

		input.onNext(1)
		input.onNext(2)
		dt.onNext(2)
		input.onNext(3)
		dt.onNext(3)
		input.onNext(4)
		dt.onNext(1)
		input.onNext(5)
		input.onNext(6)
		input.onCompleted()

		testObserver.assertValues(1, 3, 9, 21, 26, 32)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
