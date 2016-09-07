package nl.rvanheest.feedback4s

import rx.lang.scala.Observable

class Component[I, O](transform: Observable[I] => Observable[O]) {
	def run(is: Observable[I]): Observable[O] = transform(is)
}

object Component {
	def apply[I, O](transform: Observable[I] => Observable[O]): Component[I, O] = {
		new Component(transform)
	}

	def create[I, O](f: I => O): Component[I, O] = {
		Component(_ map f)
	}

	def identity[T]: Component[T, T] = Component(Predef.identity)
}
