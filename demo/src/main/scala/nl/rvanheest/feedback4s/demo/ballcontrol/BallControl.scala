package nl.rvanheest.feedback4s.demo.ballcontrol

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.stage.Stage

import nl.rvanheest.feedback4s.Component
import nl.rvanheest.feedback4s.commons.Controllers
import rx.lang.scala.JavaConverters._
import rx.observables.JavaFxObservable
import rx.schedulers.JavaFxScheduler

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class BallControl extends Application {

  val kp = 3.0
  val ki = 0.0001
  val kd = 80.0

  /**
    * Creates a one-dimensional feedback system for controlling the ball's movement.
    * To create two-dimensional control, two of these systems are combined in [[feedback]].
    *
    * @return a one-dimensional feedback system
    * @see [[feedback]]
    */
  def feedbackSystem: BallFeedbackSystem = {
    Controllers.pidController(kp, ki, kd)
      .map(d => scala.math.max(scala.math.min(d * 0.001, 0.2), -0.2))
      .scan(new AccVel)(_ accelerate _)
      .drop(1)
      .scan(Ball1D(ballRadius))(_ move _)
      .sample(16 milliseconds)
      .feedback(_.position)
  }

  /**
    * Creates a two-dimentional feedback system for controlling the ball's movement.
    * This is done by combining two instances of the one-dimensional feedback system in [[feedbackSystem]].
    *
    * @return a two-dimensional feedback system
    */
  def feedback: Component[Position, Ball2D] = {
    val fbcX = Component.create[Position, Pos] { case (x, _) => x } >>> feedbackSystem
    val fbcY = Component.create[Position, Pos] { case (_, y) => y } >>> feedbackSystem

    fbcX.combine(fbcY)(Ball2D(_, _))
  }

  def start(stage: Stage): Unit = {
    val canvas = new Canvas(width, height)
    implicit val gc = canvas.getGraphicsContext2D
    Draw.drawInit

    val root = new StackPane(canvas)
    root.setAlignment(javafx.geometry.Pos.TOP_LEFT)

    val history = new History

    JavaFxObservable.fromNodeEvents(root, MouseEvent.MOUSE_CLICKED)
      .asScala
      .map(event => (event.getX, event.getY))
      .publish(clicks => feedback.run(clicks).withLatestFrom(clicks)((_, _)))
      .observeOn(JavaFxScheduler.getInstance().asScala)
      .doOnNext { case (ball, goal) => Draw.draw(ball.position, goal, ball.acceleration, history) }
      .map { case (ball, _) => ball.position }
      .tumblingBuffer(5)
      .map(_.last)
      .subscribe(pos => {
        history.synchronized {
          if (history.size >= 50)
            history.dequeue()
          history.enqueue(pos)
        }
      })

    stage.setScene(new Scene(root, width, height))
    stage.setTitle("Ball Control")
    stage.show()
  }
}

object BallTracker extends App {
  Application.launch(classOf[BallControl])
}
