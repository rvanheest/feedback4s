package nl.rvanheest.feedback4s.demo.sqrtapprox

import nl.rvanheest.feedback4s.Component
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
