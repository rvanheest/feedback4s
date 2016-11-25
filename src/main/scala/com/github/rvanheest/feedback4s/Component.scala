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
package com.github.rvanheest.feedback4s

import rx.lang.scala.Observable

/**
	* A `Component` is defined as a transformation of `Observable`s. Given an input `Observable`
	* (lambda parameter), you can use the operators defined on it to transform the stream.
	*
	* Example:
	* {{{
	*   import nl.rvanheest.feedback4s.Component
  *   import rx.lang.scala.Observable
  *
  *   val x = new Component[Int, String](in => in.map(_.toChar).scan("")(_ + _).drop(1))
  *   x.run(Observable.just(97, 98, 99, 100))
  *     .subscribe(println(_))
	*
	*   // prints "a", "ab", "abc", "abcd"
	* }}}
	*
	* A `Component` can be depicted as shown below.
	*
	* <img src="https://www.haskell.org/arrows/arrow.png" alt="Component">
	*
	* @param transform the transform function
	* @tparam I type of data that comes in the `Component`
	* @tparam O type of data that comes out of the `Component`
	* @see [[ArrowOperators]] - composition operators
	* @see [[ApplicativeOperators]] - functorial and applicative operators
	* @see [[RxOperators]] - Rx operators for `Component`
	* @see [[FeedbackOperators]] - feedback operators
	*/
class Component[I, O](transform: Observable[I] => Observable[O]) {

	/**
		* Executes the `transform` function with a given input `Observable`.
		*
		* @param is the input `Observable`
		* @return the result of applying `is` to the `transform` function
		*/
	def run(is: Observable[I]): Observable[O] = transform(is)
}

/**
	* This object provides a set of functions to create `Component` values.
	*/
object Component {

	/**
		* Creates a `Component` with a transformation function on `Observable`s.
		*
		* <img src="https://www.haskell.org/arrows/arr.png" alt="apply">
		*
		* @param transform the transformation function
		* @tparam I the type of data in the input stream of the transformation
		* @tparam O the type of data in the output stream of the transformation
		* @return a `Component` holding a transformation
		*/
	def apply[I, O](transform: Observable[I] => Observable[O]): Component[I, O] = {
		new Component(transform)
	}

	/**
		* Creates a `Component` from a function `I => O`. This function is applied to every element
		* in the input `Observable` and emitted by the output `Observable`.
		*
		* <img src="https://www.haskell.org/arrows/arr.png" alt="create">
		*
		* @param f the transformation function applied to every element in the input stream
		* @tparam I the type of input elements
		* @tparam O the result type of the transformation
		* @return a `Component` that applies `f` to each element of the input stream
		*/
	def create[I, O](f: I => O): Component[I, O] = {
		Component(_ map f)
	}

	/**
		* Creates a `Component` that just re-emits the values it receives in the input stream
		*
		* @tparam T both the input and output type of the streams
		* @return a `Component` that re-emits the values it receives
		*/
	def identity[T]: Component[T, T] = Component(Predef.identity)
}
