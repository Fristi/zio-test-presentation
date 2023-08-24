package test

import zio.test._

object TimeSpec extends ZIOSpecDefault {
  def spec = suite("TimeSpec")(
    test("should return 0") {
      Time.now.map(out => assertTrue(out == 0))
    }
  ).provideLayerShared(Time.noop)
}
