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
		* @param src the `Component` to apply the operators on
		* @tparam I the input type of `src`
		* @tparam O the output type of `src`
		*/
	implicit class ApplicativeOperators[I, O](val src: Component[I, O]) {

		/**
			* Transforms the output type of `src` using `f`.
			*
			* @param f the transformer function
			* @tparam X the output type of the resulting `Component`
			* @return the `Component` resulting from applying `f` to the data in the output stream
			*/
		def map[X](f: O => X): Component[I, X] = {
			src >>> Component.create(f)
		}

		/**
			* Combines `src` and `other` by applying the output of `src` (of type `Observable[X => Y]`)
			* to the output of `other`.
			*
			* ''If `other` returns a stream of functions, rather than `src`, use [[<**>]] instead.''
			*
			* @param other the other `Component` in this composition
			* @param ev ''implicit'' evidence that `src`'s output type is `X => Y`
			* @tparam X the output type of `other`, as well as the input type of the functions
			*           emitted by `src`
			* @tparam Y the output type of the wrapping `Component`, as well as the output type
			*           of the functions emitted by `src`
			* @return the `Component` resulting from combining `src` and `other`
			* @see [[ArrowOperators.combine]]
			* @see [[<**>]]
			*/
		def <*>[X, Y](other: Component[I, X])(implicit ev: O <:< (X => Y)): Component[I, Y] = {
			src.combine(other)(ev(_)(_))
		}

		/**
			* Composes `src` and `other` such that the output of `src` is discarded.
			*
			* @param other the other `Component` in this composition
			* @tparam X the output type of `other` and the wrapping `Component`
			* @return the `Component` resulting from combining `src` and `other`
			* @see [[<*>]]
			*/
		def *>[X](other: Component[I, X]): Component[I, X] = {
			src.map[X => X](_ => identity) <*> other
		}

		/**
			* Composes `src` and `other` such that the output of `other` is discarded.
			*
			* @param other the other `Component` in this composition
			* @tparam X the output type of `other`
			* @return the `Component` resulting from combining `src` and `other`
			* @see [[<*>]]
			*/
		def <*[X](other: Component[I, X]): Component[I, O] = {
			src.map[X => O](o => _ => o) <*> other
		}

		/**
			* Combines `src` and `other` by applying the output of `other` to the output of `src`.
			*
			* ''If `src` returns a stream of functions, rather than `other`, use [[<*>]] instead.''
			*
			* @param other the other `Component` in this composition
			* @tparam X the output type of the functions emitted by `other`,
			*           as well as the wrapping `Component`
			* @return the `Component` resulting from combining `src` and `other`
			* @see [[ArrowOperators.combine]]
			* @see [[<*>]]
			*/
		def <**>[X](other: Component[I, O => X]): Component[I, X] = {
			other <*> src
		}
	}

	/**
		* Lifts several Rx operators to `Component`. If the required operator is not implemented,
		* use the [[liftRx]] operator to do so instead.
		*
		* @param src the `Component` to apply the operators on
		* @tparam I the input type of `src`
		* @tparam O the output type of `src`
		*/
	implicit class RxOperators[I, O](val src: Component[I, O]) {

		/**
			* Modifies the source `Component` so that it invokes an action when it calls onCompleted.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnCompleted.png" alt="doOnCompleted" height="250">
			*
			* @param onCompleted the action to invoke when the source `Component` calls onCompleted
			* @return the source `Component` with the side-effecting behavior applied
			*/
		def doOnCompleted(onCompleted: => Unit): Component[I, O] = {
			liftRx(_.doOnCompleted(onCompleted))
		}

		/**
			* Modifies the source `Component` so that it invokes an action for each item and terminal event it emits.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.o.png" alt="doOnEach" height="250">
			*
			* @param observer the `Observer` to be notified about onNext, onError and onCompleted events
			*                 on its respective methods before the actual downstream Subscriber gets notified.
			* @return the source `Component` with the side-effecting behavior applied
			*/
		def doOnEach(observer: Observer[O]): Component[I, O] = {
			liftRx(_.doOnEach(observer))
		}

		/**
			* Modifies the source `Component` so that it invokes an action if it calls onError.
			* In case the onError action throws, the downstream will receive a composite exception
			* containing the original exception and the exception thrown by onError.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnError.png" alt="doOnError" height="250">
			*
			* @param onError the action to invoke if the source `Component` calls onError
			* @return the source `Component` with the side-effecting behavior applied
			*/
		def doOnError(onError: Throwable => Unit): Component[I, O] = {
			liftRx(_.doOnError(onError))
		}

		/**
			* Modifies the source `Component` so that it invokes an action when it calls onNext.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnNext.png" alt="doOnNext" height="250">
			*
			* @param onNext the action to invoke when the source `Component` calls onNext
			* @return the source `Component` with the side-effecting behavior applied
			*/
		def doOnNext(onNext: O => Unit): Component[I, O] = {
			liftRx(_.doOnNext(onNext))
		}

		/**
			* Returns an `Component` that skips the first count items emitted by the source `Component`
			* and emits the remainder.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.png" alt="drop" height="250">
			*
			* @param n the number of items to skip
			* @return an `Component` that is identical to the source `Component` except that it does not
			*         emit the first count items that the source Observable emits
			*/
		def drop(n: Int): Component[I, O] = {
			liftRx(_.drop(n))
		}

		/**
			* Returns an `Component` that skips all items emitted by the source `Component` as long as a
			* specified condition holds true, but emits all further source items as soon as the
			* condition becomes false.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipWhile.png" alt="dropWhile" height="250">
			*
			* @param predicate a function to test each item emitted from the source `Component`
			* @return an `Component` that begins emitting items emitted by the source `Component`
			*         when the specified predicate becomes false
			*/
		def dropWhile(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.dropWhile(predicate))
		}

		/**
			* Filters items emitted by an `Component` by only emitting those that satisfy a specified predicate.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/filter.png" alt="filter" height="250">
			*
			* @param predicate a function that evaluates each item emitted by the source `Component`,
			*                  returning true if it passes the filter
			* @return an `Component` that emits only those items emitted by the source `Component` that
			*         the filter evaluates as true
			*/
		def filter(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.filter(predicate))
		}

		/**
			* Lifts an operator from Rx into a `Component` and concats this with `src`
			*
			* @param f the Rx operator
			* @tparam Y the output type of the Rx operator
			* @return the `Component` wrapping the concatenation between `src` and the Rx operator
			*/
		def liftRx[Y](f: Observable[O] => Observable[Y]): Component[I, Y] = {
			src >>> Component(f)
		}

		/**
			* Returns an `Component` that emits the most recently emitted item (if any) emitted
			* by the source `Component` within periodic time intervals, where the intervals are
			* defined on a particular `Scheduler`.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.s.png" alt="sample" height="250">
			*
			* @param interval the sample rate
			* @param scheduler the `Scheduler` to use when sampling
			* @return an `Component` that emits the results of sampling the items emitted by the
			*         source `Component` at the specified time interval
			*/
		def sample(interval: Duration, scheduler: Scheduler = NewThreadScheduler()): Component[I, O] = {
			liftRx(_.sample(interval, scheduler))
		}

		/**
			* Returns an `Component` that emits a specified item before it begins to emit
			* items emitted by the source `Component`.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.o.png" alt="startWith" height="250">
			*
			* @param o the item to emit
			* @return an `Component` that emits the specified item before it begins to emit
			*         items emitted by the source `Component`
			*/
		def startWith(o: O): Component[I, O] = {
			liftRx(o +: _)
		}

		/**
			* Returns an `Component` that applies a specified accumulator function to the first
			* item emitted by a source `Component` and a seed value, then feeds the result of
			* that function along with the second item emitted by the source `Component` into
			* the same function, and so on until all items have been emitted by the source
			* `Component`, emitting the result of each of these iterations.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scanSeed.png" alt="scan" height="250">
			*
			* @param seed the initial (seed) accumulator item
			* @param combiner an accumulator function to be invoked on each item emitted by the
			*                 source `Component`, whose result will be emitted to Observers via
			*                 onNext and used in the next accumulator call
			* @tparam Y the initial, accumulator and result type
			* @return an `Component` that emits initialValue followed by the results of each call
			*         to the accumulator function
			*/
		def scan[Y](seed: Y)(combiner: (Y, O) => Y): Component[I, Y] = {
			liftRx(_.scan(seed)(combiner))
		}

		/**
			* Returns an `Component` that emits only the first count items emitted by the source
			* `Component`. If the source emits fewer than count items then all of its items are emitted.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.png" alt="take" height="250">
			*
			* @param n the maximum number of items to emit
			* @return an `Component` that emits only the first count items emitted by the source
			*         `Component`, or all of the items from the source `Component` if that `Component` emits fewer than count items
			*/
		def take(n: Int): Component[I, O] = {
			liftRx(_.take(n))
		}

		/**
			* Returns an `Component` that emits items emitted by the source `Component`,
			* checks the specified predicate for each item, and then completes when the condition
			* is satisfied.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.p.png" alt="takeUntil" height="250">
			*
			* @param predicate a function that evaluates an item emitted by the source `Component`
			*                  and returns a Boolean
			* @return an `Component` that first emits items emitted by the source `Component`,
			*         checks the specified condition after each item, and then completes when the
			*         condition is satisfied
			*/
		def takeUntil(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.takeUntil(predicate))
		}

		/**
			* Returns an `Component` that emits items emitted by the source `Component` so
			* long as each item satisfied a specified condition, and then completes as
			* soon as this condition is not satisfied.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeWhile.png" alt="takeWhile" height="250">
			*
			* @param predicate a function that evaluates an item emitted by the source `Component` and returns a Boolean
			* @return an `Component` that emits the items from the source `Component`
			*         so long as each item satisfies the condition defined by predicate, then completes
			*/
		def takeWhile(predicate: O => Boolean): Component[I, O] = {
			liftRx(_.takeWhile(predicate))
		}

		/**
			* Returns an `Component` that emits only the first item emitted by the source `Component`
			* during sequential time windows of a specified duration, where the windows are managed
			* by a specified Scheduler.
			*
			* This differs from throttleLast(long, java.util.concurrent.TimeUnit) in that this only
			* tracks passage of time whereas throttleLast(long, java.util.concurrent.TimeUnit) ticks
			* at scheduled intervals.
			*
			* <img src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.s.png" alt="throttleFirst" height="250">
			*
			* @param duration time to wait before emitting another item after emitting the last item
			* @param scheduler the Scheduler to use internally to manage the timers that handle timeout for each event
			* @return an `Component` that performs the throttle operation
			*/
		def throttleFirst(duration: Duration, scheduler: Scheduler = NewThreadScheduler()): Component[I, O] = {
			liftRx(_.throttleFirst(duration, scheduler))
		}
	}

	/**
		*
		* @param src the `Component` to apply the operators on
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
