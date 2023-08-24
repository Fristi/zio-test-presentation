package test

import zio.test._
import zio.test.magnolia.DeriveDiff._

case class Point(x: Int, y: Int)

object DiffSpec extends ZIOSpecDefault{
  def spec = suite("DiffSpec")(
    test("show diff from point")(
      assertTrue(Point(1, 1) == Point(3, 1))
    ) @@ TestAspect.failing
  )
}
