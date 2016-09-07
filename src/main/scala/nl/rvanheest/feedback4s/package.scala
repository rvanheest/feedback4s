package nl.rvanheest

import rx.lang.scala.schedulers.{NewThreadScheduler, TrampolineScheduler}
import rx.lang.scala.{Observable, Observer, Scheduler, Subject}

import scala.concurrent.duration.Duration

package object feedback4s {

	implicit class ArrowOperators[I, O](val src: Component[I, O]) {
		def >>>[X](other: Component[O, X]): Component[I, X] = this concat other

		def concat[X](other: Component[O, X]): Component[I, X] = {
			Component(other.run _ compose src.run)
		}

		def first[X]: Component[(I, X), (O, X)] = {
			this *** Component.identity[X]
		}

		def second[X]: Component[(X, I), (X, O)] = {
			Component.identity[X] *** src
		}

		/*
			this way of implementing (first and second dependend on ***) is more efficient in case of
			Observables since you only need 1 zipWith operator
			see also: http://hackage.haskell.org/package/base-4.9.0.0/docs/src/Control.Arrow.html#first
		 */
		def ***[X, Y](other: Component[X, Y]): Component[(I, X), (O, Y)] = this split other

		def split[X, Y](other: Component[X, Y]): Component[(I, X), (O, Y)] = {
			Component(_.publish(ixs => {
				src.run(ixs.map(_._1)).zipWith(other.run(ixs.map(_._2)))((_, _))
			}))
		}

		def &&&[X](other: Component[I, X]): Component[I, (O, X)] = this fanout other

		def fanout[X](other: Component[I, X]): Component[I, (O, X)] = {
			Component.create[I, (I, I)](a => (a, a)) >>> (src *** other)
		}

		// called LiftA2 in Haskell
		def combine[X, Y](other: Component[I, X])(f: (O, X) => Y): Component[I, Y] = {
			(src &&& other) >>> Component.create(f.tupled)
		}
	}

	implicit class ApplicativeOperators[I, O](val src: Component[I, O]) {
		def map[X](f: O => X): Component[I, X] = {
			src >>> Component.create(f)
		}

		def <*>[X, Y](other: Component[I, X])(implicit ev: O <:< (X => Y)): Component[I, Y] = {
			src.combine(other)(ev(_)(_))
		}

		def *>[X](other: Component[I, X]): Component[I, X] = {
			src.map[X => X](_ => identity) <*> other
		}

		def <*[X](other: Component[I, X]): Component[I, O] = {
			src.map[X => O](o => _ => o) <*> other
		}

		def <**>[X](other: Component[I, O => X]): Component[I, X] = {
			other <*> src
		}
	}

	implicit class RxOperators[I, O](val src: Component[I, O]) {
		def doOnCompleted(consumer: => Unit): Component[I, O] = {
			liftRx(_.doOnCompleted(consumer))
		}

		def doOnEach(observer: Observer[O]): Component[I, O] = {
			liftRx(_.doOnEach(observer))
		}

		def doOnEach(onNext: O => Unit): Component[I, O] = {
			liftRx(_.doOnEach(onNext))
		}

		def doOnEach(onNext: O => Unit, onError: Throwable => Unit): Component[I, O] = {
			liftRx(_.doOnEach(onNext, onError))
		}

		def doOnEach(onNext: O => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Component[I, O] = {
			liftRx(_.doOnEach(onNext, onError, onCompleted))
		}

		def doOnError(consumer: Throwable => Unit): Component[I, O] = {
			liftRx(_.doOnError(consumer))
		}

		def doOnNext(consumer: O => Unit): Component[I, O] = {
			liftRx(_.doOnNext(consumer))
		}

		def drop(n: Int): Component[I, O] = {
			liftRx(_.drop(n))
		}

		def dropWhile(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.dropWhile(predicate))
		}

		def filter(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.filter(predicate))
		}

		def liftRx[Y](f: Observable[O] => Observable[Y]) = {
			src >>> Component(f)
		}

		def sample(interval: Duration, scheduler: Scheduler = NewThreadScheduler()) = {
			liftRx(_.sample(interval, scheduler))
		}

		def startWith(o: O): Component[I, O] = {
			liftRx(o +: _)
		}

		def scan[Y](seed: Y)(combiner: (Y, O) => Y): Component[I, Y] = {
			liftRx(_.scan(seed)(combiner))
		}

		def take(n: Int): Component[I, O] = {
			liftRx(_.take(n))
		}

		def takeUntil(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.takeUntil(predicate))
		}

		def takeWhile(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.takeWhile(predicate))
		}

		def throttleFirst(duration: Duration, scheduler: Scheduler = NewThreadScheduler()) = {
			liftRx(_.throttleFirst(duration, scheduler))
		}
	}

	implicit class FeedbackOperators[I, O](val src: Component[I, O]) {
		private def loop[T, S](transOut: Observable[T], setpoint: Observable[S])(combinator: (T, S) => I): Observable[I] = {
			transOut.publish(tos => setpoint.publish(sps =>
				tos.combineLatestWith(sps)((_, _))
					.take(1)
					.flatMap { case (t, s) =>
						Observable[I](observer => {
							tos.withLatestFrom(s +: sps)(combinator).subscribe(observer)
							observer.onNext(combinator(t, s))
						})
					}))
		}

		def feedback(transducerFunc: O => I)(implicit n: Numeric[I]): Component[I, O] = {
			feedback(Component.create(transducerFunc))
		}

		def feedback(transducer: Component[O, I])(implicit n: Numeric[I]): Component[I, O] = {
			feedbackWith(transducer)((t, s) => n.minus(s, t))
		}

		def feedbackWith[T, S](transducerFunc: O => T)(combinatorFunc: (T, S) => I): Component[S, O] = {
			feedbackWith(Component.create(transducerFunc))(combinatorFunc)
		}

		def feedbackWith[T, S](transducer: Component[O, T])(combinatorFunc: (T, S) => I): Component[S, O] = {
			Component(setpoint => {
				val srcIn = Subject[I]()

				src.run(srcIn)
					.publish(out => {
						loop(transducer.run(out), setpoint)(combinatorFunc)
							.observeOn(TrampolineScheduler())
							.subscribe(srcIn)

						out
					})
			})
		}
	}
}
