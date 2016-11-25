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
package nl.rvanheest.test.feedback4s

import nl.rvanheest.feedback4s.Component
import org.junit.Test
import rx.lang.scala.Subject
import rx.lang.scala.observers.TestSubscriber

import scala.language.postfixOps

class FeedbackTest {

	@Test
	def testFeedbackIdentity() = {
		val k = 160

		val feedback = Component[Double, Double](_.scan(0.0)(_ + _))
			.map(k *)
			.map(x => math.max(0, math.min(1, x / 100)))
			.feedback(Component.identity[Double])
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		feedback.run(input).take(5).subscribe(testObserver)

		input.onNext(0.6)

		testObserver.assertValues(0.0, 0.96, 0.384, 0.7295999999999999, 0.52224)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFeedbackComponent() = {
		val k = 160

		val feedback = Component[Double, Double](_.scan(0.0)(_ + _))
			.map(k *)
			.map(x => math.max(0, math.min(1, x / 100)))
			.feedback(Component.create[Double, Double](_ - 0.01))
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		feedback.run(input).take(5).subscribe(testObserver)

		input.onNext(0.6)

		testObserver.assertValues(0.0, 0.976, 0.39039999999999997, 0.74176, 0.5309440000000001)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFeedbackLambda() = {
		val k = 160

		val feedback = Component[Double, Double](_.scan(0.0)(_ + _))
			.map(k *)
			.map(x => math.max(0, math.min(1, x / 100)))
			.feedback(_ - 0.01)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		feedback.run(input).take(5).subscribe(testObserver)

		input.onNext(0.6)

		testObserver.assertValues(0.0, 0.976, 0.39039999999999997, 0.74176, 0.5309440000000001)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFeedbackWithComponent() = {
		val k = 160

		val feedback = Component[Double, Double](_.scan(0.0)(_ + _))
			.map(k *)
			.map(x => math.max(0, math.min(1, x / 100)))
			.feedbackWith[Double, Double](Component.create[Double, Double](_ - 0.01))((t, s) => s - t)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		feedback.run(input).take(5).subscribe(testObserver)

		input.onNext(0.6)

		testObserver.assertValues(0.0, 0.976, 0.39039999999999997, 0.74176, 0.5309440000000001)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}

	@Test
	def testFeedbackWithLambda() = {
		val k = 160

		val feedback = Component[Double, Double](_.scan(0.0)(_ + _))
			.map(k *)
			.map(x => math.max(0, math.min(1, x / 100)))
			.feedbackWith[Double, Double]((x: Double) => x - 0.01)((t, s) => s - t)
		val input = Subject[Double]()
		val testObserver = TestSubscriber[Double]

		feedback.run(input).take(5).subscribe(testObserver)

		input.onNext(0.6)

		testObserver.assertValues(0.0, 0.976, 0.39039999999999997, 0.74176, 0.5309440000000001)
		testObserver.assertNoErrors()
		testObserver.assertCompleted()
		testObserver.assertUnsubscribed()
	}
}
