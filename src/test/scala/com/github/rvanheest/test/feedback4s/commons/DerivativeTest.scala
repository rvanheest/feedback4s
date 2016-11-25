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
package com.github.rvanheest.test.feedback4s.commons

import nl.rvanheest.feedback4s.commons.Controllers
import org.junit.Test
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.{Observable, Subject}

class DerivativeTest {

	@Test
	def testDerivativeWithNoDT() = {
		val derivative = Controllers.derivativeController[Double]()
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		derivative.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(6.0)
		input.onNext(3.0)
		input.onNext(9.0)
		input.onCompleted()

		testObserver.assertValues(1.0, 5.0, -3.0, 6.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testDerivativeWithSingleDT() = {
		val dt = Observable.just(2.0)
		val derivative = Controllers.derivativeController[Double](dt)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		derivative.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(6.0)
		input.onNext(3.0)
		input.onNext(9.0)
		input.onCompleted()

		testObserver.assertValues(0.5, 2.5, -1.5, 3.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testDerivativeWithMultipleDT() = {
		val dt = Subject[Double]()
		val derivative = Controllers.derivativeController[Double](dt)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		derivative.run(input).subscribe(testObserver)

		input.onNext(1.0)
		input.onNext(6.0)
		dt.onNext(2.0)
		input.onNext(3.0)
		dt.onNext(3.0)
		input.onNext(9.0)
		input.onCompleted()

		testObserver.assertValues(1.0, 5.0, -1.5, 2.0)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
