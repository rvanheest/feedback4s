package nl.rvanheest.feedback4s.demo

import nl.rvanheest.feedback4s.Component

import scala.collection.mutable

/**
  * The BallControl demo shows how a feedback system is created and applied in a simple, reactive
  * setup. The system under control is the position of a ball. Given its current position it has
  * to move to the position indicated by the user. To do so, we take the difference between the
  * ''current position'' and the ''desired position''. Using a
  * [[http://rvanheest.github.io/Literature-Study-Feedback-Control/Controllers.html#pid-control PID-controller]]
  * this ''error'' in transformed in a new ''acceleration''. By integrating twice (''acceleration''
  * to ''velocity'' to ''position'') we get the ball's next position. Repeating this process using
  * a feedback system ultimately brings the ball to a halt at the desired position.
  */
package object ballcontrol {

  val ballRadius = 20.0
  val width = 1024
  val height = 768

  type Pos = Double
  type Vel = Double
  type Acc = Double
  type Position = (Pos, Pos)
  type Velocity = (Vel, Vel)
  type Acceleration = (Acc, Acc)
  type History = mutable.Queue[Position]
  type BallFeedbackSystem = Component[Pos, Ball1D]

  case class AccVel(acceleration: Acc = 0.0, velocity: Vel = 0.0) {
    def accelerate(acc: Acc): AccVel = {
      AccVel(acc, velocity + acc)
    }
  }

  case class Ball1D(acceleration: Acc, velocity: Vel, position: Pos) {
    def move(av: AccVel): Ball1D = {
      Ball1D(av.acceleration, av.velocity, position + av.velocity)
    }
  }
  object Ball1D {
    def apply(position: Pos): Ball1D = Ball1D(0.0, 0.0, position)
  }

  case class Ball2D(acceleration: Acceleration, velocity: Velocity, position: Position)
  object Ball2D {
    def apply(x: Ball1D, y: Ball1D): Ball2D = {
      Ball2D((x.acceleration, y.acceleration), (x.velocity, y.velocity), (x.position, y.position))
    }
  }
}
