package nl.rvanheest.feedback4s.commons

import nl.rvanheest.feedback4s.Component
import rx.lang.scala.Observable

import scala.language.postfixOps

object Controllers {

	def integralController[T](dt: Observable[T] = Observable.empty)(implicit n: Numeric[T]): Component[T, T] = {
		Component[T, T](_.withLatestFrom(n.one +: dt)(n.times))
			.scan(n.zero)(n.plus)
			.drop(1)
	}

	def derivativeController[T](dt: Observable[T] = Observable.empty)(implicit n: Fractional[T]): Component[T, T] = {
		Component.identity[T]
			.startWith(n.zero)
			.liftRx(_.slidingBuffer(2, 1))
			.filter(_.size == 2)
			.map {
				case Seq(fst, snd) => n.minus(snd, fst)
				case xs => throw new IllegalStateException(s"list $xs should have length two by this point")
			}
			.liftRx(_.withLatestFrom(n.one +: dt)(n.div))
	}

	def piController[T](kp: T, ki: T, dt: Observable[T] = Observable.empty)(implicit n: Numeric[T]): Component[T, T] = {
		import n._

		Component.create(kp *).combine(integralController(dt).map(ki *))(_ + _)
	}

	def pidController[T](kp: T, ki: T, kd: T, dt: Observable[T] = Observable.empty)(implicit n: Fractional[T]): Component[T, T] = {
		import n._

		val proportional = Component.create(kp *)
		val integral = integralController(dt).map(ki *)
		val derivative = derivativeController(dt).map(kd *)

		proportional.combine(integral)((c1, c2) => c1 + c2 + _) <*> derivative
	}
}
