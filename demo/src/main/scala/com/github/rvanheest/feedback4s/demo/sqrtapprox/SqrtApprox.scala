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
package com.github.rvanheest.feedback4s.demo.sqrtapprox

import com.github.rvanheest.feedback4s.Component
import rx.lang.scala.subjects.PublishSubject

object SqrtApprox extends App {

  type Number = Double
  type Guess = Double
  type Accuracy = Double
  type GoodEnough = Boolean

  def sqrtSystem(x: Number, firstGuess: Guess = 1.0): Component[Number, Guess] = {
    def isGoodEnough(guess: Guess, setpoint: Accuracy): GoodEnough = {
      scala.math.abs(guess * guess - x) / x < setpoint
    }

    def goodEnough: Component[(Guess, Accuracy), (Guess, GoodEnough)] = {
      Component.create { case (guess, setpoint) => (guess, isGoodEnough(guess, setpoint)) }
    }

    goodEnough.takeWhile { case (_, goodEnough) => !goodEnough }
      .map { case (guess, _) => (guess + x / guess) / 2 }
      .startWith(firstGuess)
      .feedbackWith(Component.identity[Double])((_, _))
  }

  val subj = PublishSubject[Accuracy]()
  sqrtSystem(4.0)
    .run(subj)
    .subscribe(println(_), e => println(e.getMessage), () => println("completed"))
  subj.onNext(0.001)
}
