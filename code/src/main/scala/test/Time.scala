package test

import zio._
import zio.stream.ZStream

import java.util.concurrent.TimeUnit

trait Time {
  def now: UIO[Long]
}

object Time {
  val noop: ULayer[Time] = ZLayer.succeed(new Time {
    def now: UIO[Long] = ZIO.succeed(0L)
  })

  val live = ZLayer.succeed(new Time {
    override def now: UIO[Long] = ZIO.succeed(java.lang.System.currentTimeMillis())
  })

  // accessor method
  def now: ZIO[Time, Nothing, Long] =
    ZIO.environmentWithZIO(_.get.now)

  val stream: ZStream[Time, Nothing, Long] = ZStream.repeatZIOWithSchedule(Time.now, Schedule.fixed(Duration(5, TimeUnit.SECONDS)))

  val result: ZStream[Any, Nothing, Long] = stream.provideLayer(Time.noop)
}
