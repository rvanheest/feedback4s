package nl.rvanheest

import rx.lang.scala.schedulers.{NewThreadScheduler, TrampolineScheduler}
import rx.lang.scala.{Observable, Observer, Scheduler, Subject}

import scala.concurrent.duration.Duration

package object feedback4s {

	/**
		* Adds operators for composition of `Component`s in various ways.
		*
		* Examples:
		* {{{
		*   import nl.rvanheest.feedback4s.Component
    *
    *   val x = new Component[Int, String](in => in.map(_.toChar).scan("")(_ + _).drop(1))
    *   val y = Component.create[String, Int](_.length)
    *   val z = Component.identity[Int]
    *
    *   val a1: Component[Int, Int] = x >>> y
    *   val a2: Component[Int, Int] = x.concat(y)
    *   val b: Component[(Int, Long), (String, Long)] = x.first[Long]
    *   val c: Component[(Long, Int), (Long, String)] = x.second[Long]
    *   val d1: Component[(Int, String), (String, Int)] = x *** y
    *   val d2: Component[(Int, String), (String, Int)] = x.split(y)
    *   val e1: Component[Int, (String, Int)] = x &&& z
    *   val e2: Component[Int, (String, Int)] = x.fanout(z)
    *   val f: Component[Int, Int] = x.combine(z)((s, i) => s.length + i)
		* }}}
		*
		* @param src the `Component` to compose with
		* @tparam I the input type of `src`
		* @tparam O the output type of `src`
		*/
	implicit class ArrowOperators[I, O](val src: Component[I, O]) {

		/**
			* Concatenates two `Component`s horizontally. The output of `src` will be the input of `other`.
			* The resulting `Component` will be a wrapper around the two combined `Component`s.
			*
			* ''This is the symbolic equivalent of the [[concat]] operator.''
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/concat-operator.png?raw=true" alt="concat" height="100">
			*
			* @param other the right-hand `Component` in this composition
			* @tparam X the output type of `other`
			* @return a `Component` that wraps around the concatenation of `src` and `other`
			* @see [[concat]]
			*/
		def >>>[X](other: Component[O, X]): Component[I, X] = this concat other

		/**
			* Concatenates two `Component`s horizontally. The output of `src` will be the input of `other`.
			* The resulting `Component` will be a wrapper around the two combined `Component`s.
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/concat-operator.png?raw=true" alt="concat" height="100">
			*
			* @param other the right-hand `Component` in this composition
			* @tparam X the output type of `other`
			* @return a `Component` that wraps around the concatenation of `src` and `other`
			* @see [[>>>]]
			*/
		def concat[X](other: Component[O, X]): Component[I, X] = {
			Component(other.run _ compose src.run)
		}

		/**
			* Vertically composes `src` with an identity `Component` of type `X`.
			*
			* ''Note that this operator is the opposite of [[second]]: here the `X` is the second
			* element in the tuple.''
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/first-operator.png?raw=true" alt="first" height="200">
			*
			* @tparam X the input and output type of the identity `Component`
			* @return a `Component` that wraps around the composition of `src` and `identity[X]`
			*/
		def first[X]: Component[(I, X), (O, X)] = {
			this *** Component.identity[X]
		}

		/**
			* Vertically composes an identity `Component` of type `X` with `src`.
			*
			* ''Note that this operator is the opposite of [[first]]: here the `X` is the first
			* element in the tuple.''
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/second-operator.png?raw=true" alt="second" height="200">
			*
			* @tparam X the input and output type of the identity `Component`
			* @return a `Component` that wraps around the composition of `src` and `identity[X]`
			*/
		def second[X]: Component[(X, I), (X, O)] = {
			Component.identity[X] *** src
		}

		/**
			* Vertically composes `src` with `other`. The `Observable`s in each `Component` are zipped
			* and wrapped in a new `Component`.
			*
			* ''This is the symbolic equivalent of the [[split]] operator.''
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/split-operator.png?raw=true" alt="split" height="200">
			*
			* @param other the other `Component` in this composition
			* @tparam X the input type of `other`
			* @tparam Y the output type of `other`
			* @return a `Component` that wraps around the composition of `src` and `other`
			* @see [[split]]
			*/
		/*
			this way of implementing (first and second dependend on ***) is more efficient in case of
			Observables since you only need 1 zipWith operator
			see also: http://hackage.haskell.org/package/base-4.9.0.0/docs/src/Control.Arrow.html#first
		 */
		def ***[X, Y](other: Component[X, Y]): Component[(I, X), (O, Y)] = this split other

		/**
			* Vertically composes `src` with `other`. The `Observable`s in each `Component` are zipped
			* and wrapped in a new `Component`.
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/split-operator.png?raw=true" alt="split" height="200">
			*
			* @param other the other `Component` in this composition
			* @tparam X the input type of `other`
			* @tparam Y the output type of `other`
			* @return a `Component` that wraps around the composition of `src` and `other`
			* @see [[***]]
			*/
		def split[X, Y](other: Component[X, Y]): Component[(I, X), (O, Y)] = {
			Component(_.publish(ixs => {
				src.run(ixs.map(_._1)).zipWith(other.run(ixs.map(_._2)))((_, _))
			}))
		}

		/**
			* Vertically composes `src` with `other` such that the input of the wrapping `Component`
			* is emitted to both `src` and `other`. For this to work, both `Component`s must have the
			* same input type. The outputs of the components are combined in a tuple.
			*
			* ''This is the symbolic equivalent of the [[fanout]] operator.''
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/fanout-operator.png?raw=true" alt="fanout" height="200">
			*
			* @param other the other `Component` in this composition
			* @tparam X the output type of `other`
			* @return a `Component` that wraps around the composition of `src` and `other` and sends
			*         its input to both `src` and `other`
			* @see [[fanout]]
			*/
		def &&&[X](other: Component[I, X]): Component[I, (O, X)] = this fanout other

		/**
			* Vertically composes `src` with `other` such that the input of the wrapping `Component`
			* is emitted to both `src` and `other`. For this to work, both `Component`s must have the
			* same input type. The outputs of the components are combined in a tuple.
			*
			* <img src="https://github.com/rvanheest/feedback4s/blob/gh-pages/img/fanout-operator.png?raw=true" alt="fanout" height="200">
			*
			* @param other the other `Component` in this composition
			* @tparam X the output type of `other`
			* @return a `Component` that wraps around the composition of `src` and `other`
			* @see [[&&&]]
			*/
		def fanout[X](other: Component[I, X]): Component[I, (O, X)] = {
			Component.create[I, (I, I)](a => (a, a)) >>> (src *** other)
		}

		/**
			* Vertically composes `src` with `other` such that the input of the wrapping `Component`
			* is emitted to both `src` and `other`, while their output types are combined using the
			* function `f`. For this to work, both `Component`s must have the same input type.
			*
			* <img src="https://www.haskell.org/arrows/addA.png" alt="combine">
			*
			* @param other the other `Component` in this composition
			* @param f the combine function that computes the overall output given the outputs of
			*          both `src` and `other`
			* @tparam X the output type of `other`
			* @tparam Y the output type of the wrapping `Component`
			* @return a `Component` that wraps around the composition of `src` and `other`
			*/
		// called LiftA2 in Haskell
		def combine[X, Y](other: Component[I, X])(f: (O, X) => Y): Component[I, Y] = {
			(src &&& other) >>> Component.create(f.tupled)
		}
	}

	/**
		* Adds functorial and applicative operators for `Component`
		*
		* @param src
		* @tparam I the input type of `src`
		* @tparam O the output type of `src`
		*/
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

	/**
		*
		* @param src
		* @tparam I the input type of `src`
		* @tparam O the output type of `src`
		*/
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

	/**
		*
		* @param src
		* @tparam I the input type of `src`
		* @tparam O the output type of `src`
		*/
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
