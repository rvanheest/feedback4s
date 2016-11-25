package nl.rvanheest.feedback4s.demo.ballcontrol

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap

object Draw {

  def draw(pos: Position, setpoint: Position, acc: Acceleration, history: History)(implicit gc: GraphicsContext): Unit = {
    drawBackground
    drawHistory(history)
    drawLine(pos, setpoint)
    drawSetpoint(setpoint)
    drawBall(pos)
    drawVectors(pos, acc)
  }

  def drawInit(implicit gc: GraphicsContext): Unit = {
    drawBackground
    drawBall(ballRadius, ballRadius)
  }

  def drawBackground(implicit gc: GraphicsContext): Unit = {
    gc.setFill(Color.rgb(231, 212, 146))
    gc.fillRect(0, 0, width, height)
  }

  def drawBall(point: Position)(implicit gc: GraphicsContext): Unit = {
    val (x, y) = point
    val diameter = 2 * ballRadius

    gc.setFill(Color.rgb(123, 87, 71))
    gc.fillOval(x - ballRadius, y - ballRadius, diameter, diameter)
  }

  def drawSetpoint(setpoint: Position)(implicit gc: GraphicsContext): Unit = {
    val (x, y) = setpoint

    val radius = ballRadius / 4
    val diameter = radius * 2

    gc.setFill(Color.rgb(161, 90, 90))
    gc.fillOval(x - radius, y - radius, diameter, diameter)
  }

  def drawLine(ball: Position, setpoint: Position)(implicit gc: GraphicsContext): Unit = {
    gc.setStroke(Color.rgb(96, 185, 154))
    gc.setLineWidth(1.0)
    gc.setLineDashes(8.0, 14.0)

    gc.beginPath()
    (gc.moveTo _).tupled(setpoint)
    (gc.lineTo _).tupled(ball)
    gc.stroke()
    gc.setLineDashes()
  }

  def drawVectors(pos: Position, acc: Acceleration)(implicit gc: GraphicsContext): Unit = {
    val (px, py) = pos
    val (ax, ay) = acc

    gc.setStroke(Color.rgb(247, 120, 37))
    gc.setLineWidth(8)
    gc.setLineCap(StrokeLineCap.ROUND)

    gc.beginPath()
    gc.moveTo(px, py)
    gc.lineTo(px - ax * 300, py)
    gc.stroke()

    gc.beginPath()
    gc.lineTo(px, py)
    gc.lineTo(px, py - ay * 300)
    gc.stroke()
  }

  def drawHistory(history: History)(implicit gc: GraphicsContext): Unit = {
    history.synchronized {
      history.zipWithIndex.foreach(item => {
        val ((x, y), index) = item
        val size = history.size
        val alpha = (index: Double) / size

        gc.setFill(Color.rgb(96, 185, 154, alpha))
        gc.fillOval(x, y, 10, 10)
      })
    }
  }
}
