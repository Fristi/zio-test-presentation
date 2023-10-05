---
layout: cover
theme: purplin
highlighter: shiki
colorSchema: light
---

# zio-test and ScalaTest

<p text="2xl" class="!leading-8">
What's zio-test and how does it compare to ScalaTest?
</p>

<div class="abs-br mx-14 my-12 flex">
  <!-- <logos:vue text="3xl"/> -->
  <div class="ml-3 flex flex-col text-left gap-1">
    <div class="text-sm opacity-50">September 2023</div>
  </div>
</div>

<BarBottom  title="zio-test">
  <Item text="">
    <img
      src="https://vectos.net/img/logo.png"
      class="w-16"
    />
  </Item>
</BarBottom>

---
layout: 'intro'
---

<h1 text="!5xl">Mark de Jong</h1>

<div class="leading-8 opacity-80">
Functional Programming 🥑<br>
Company site: <a href="https://vectos.net" target="_blank">Vectos</a>.<br>
</div>

<img src="https://vectos.net/img/mark.jpg" class="rounded-full w-40 abs-tr mt-30 mr-20"/>

<BarBottom  title="zio-test">
  <Item text="">
    <img
      src="https://vectos.net/img/logo.png"
      class="w-16"
    />
  </Item>
</BarBottom>


---

# Agenda

<v-clicks>

- ZIO Recap
- ZIO Test features
- ZIO Test and Scalatest

</v-clicks>

---

# ZIO Recap

<v-clicks>

- ZIO is functional effect system with different primitives
- For (a)sync effect `ZIO[R, E, A]` and stream effect (pull-based) `ZStream[R, E, A]`
  - Where `R` = environment type
  - Where `E` = error type
  - Where `A` = success type
- For environment `R`, there is a construction/destruction abstraction `ZLayer[R, E, A]`
- These abstractions offer a rich set of various constructors and combinators

</v-clicks>

---
layout: center
class: 'text-center pb-5'
---

# ZIO Effects

---

# ZIO effects

### Creating

```scala
ZIO.succeed(42)
ZIO.fail("Oh noes")
ZIO.fromEither(Right("Success!"))
ZIO.fromTry(Try(42 / 0))
```

### Operations

```scala
// transform
ZIO.succeed(21).map(_ * 2)

// zipped
ZIO.succeed("4").zip(ZIO.succeed(2))

// sequential
for {
  _    <- Console.printLine("Hello! What is your name?")
  name <- Console.readLine
  _    <- Console.printLine(s"Hello, ${name}, welcome to ZIO!")
} yield ()
```

---

# ZIO effects

### Failures

```scala
// partial catch
openFile("primary.data").catchSome {
  case _ : FileNotFoundException =>
    openFile("backup.data")
}

// fallback
openFile("primary.data").orElse(openFile("backup.data"))

// retry
openFile("primary.data").retry(Schedule.recurs(5))

// timeout
ZIO.succeed("Hello").timeout(10.seconds)
```

---

# ZIO effects

### Create service

```scala
trait Time {
  def now: UIO[Long]
}

object Time {
  val noop = ZLayer.succeed(new Time {
    def now: UIO[Long] = ZIO.succeed(0)
  })
  
  // accessor method
  def now: ZIO[Time, Nothing, Long] = 
    ZIO.environmentWithZIO(_.get.now)
}
```

### Environment

```scala
// construct a program with a environment (Time)
def program: ZIO[Time, Nothing, Long] = Time.now

// eliminate the environment by providing the layer
def result: ZIO[Any, Nothing, Long] = program.provideLayer(Time.noop)
```

---
layout: center
class: 'text-center pb-5'
---

# ZIO Streams

---

# ZIO streams

### Create streams

```scala
// stream with 1, 2, 3
ZStream(1, 2, 3)

// natural numbers
ZStream.iterate(1)(_ + 1)

// range
ZStream.range(1, 5)

// from a ZIO effect
ZStream.fromZIO(Random.nextInt)

// unfold a stream
ZStream.unfoldZIO(()) { _ =>
  Console.readLine.map {
    case "exit"  => None
    case i => Some((i, ()))
  }
}
```

---

# ZIO streams

### Operations on stream

```scala
// Output: 0, 1, 2, 3, 4
ZStream.iterate(0)(_ + 1).take(5)

// effectfull map over a stream 
ZStream.fromIterableZIO(getUrls).mapZIOPar(8)(fetchUrl)

// tail of each chunk is the output (2, 3, 5, 7, 8, 9)
ZStream
  .fromChunks(Chunk(1, 2, 3), Chunk(4, 5), Chunk(6, 7, 8, 9))
  .mapChunks(_.tail)

// filtering
ZStream.range(1, 11).filter(_ % 2 == 0)

// scan
ZStream(1, 2, 3, 4, 5).scan(0)(_ + _)

```

---

# ZIO streams

### Pipeline

```scala
// split on '-'
ZStream("1-2-3", "4-5", "6", "7-8-9-10").via(ZPipeline.splitOn("-")).map(_.toInt)

// compressing a file
ZStream
  .fromFileName("file.txt")
  .via(
    ZPipeline.gzip(
      bufferSize = 64 * 1024,
      level = CompressionLevel.DefaultCompression,
      strategy = CompressionStrategy.DefaultStrategy,
      flushMode = FlushMode.NoFlush
    )
  )

// composing pipelines (>>>)
ZStream.fromFileName("file.txt")
  .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)

```

---

# ZIO streams

### Sinks

```scala
// sum (output: 15)
ZStream(1, 2, 3, 4, 5).run(Sink.sum[Int])

// ignore output
ZSink.drain

// foreach
ZStream(1, 2, 3, 4, 5).run(ZSink.foreach((i: Int) => printLine(i)))

// collectAll (output: Chunk(1, 2, 3, 4, 5))
ZStream(1, 2, 3, 4, 5).run(ZSink.collectAll[Int])

// folding (output: 15)
ZStream(1, 2, 3, 4, 5).run(ZSink.foldLeft[Int, Int](0)(_ + _))
```

---

# ZIO streams

### Environment

```scala
// construct a program with a environment (Time)
val program: ZStream[Time, Nothing, Long] = 
  ZStream.repeatZIOWithSchedule(Time.now, Schedule.fixed(Duration(5, TimeUnit.SECONDS)))

// eliminate the environment by providing the layer
val result: ZStream[Any, Nothing, Long] = stream.provideLayer(Time.noop)
```


---
layout: center
class: 'text-center pb-5'
---

# ZIO Test

---

# ZIO Test

### Features

<v-clicks>

- Testable versions of `System`, `Clock`, `Random`, `Console`
- Test aspects, which annotate tests with `timeout`, `repeat`, `retry` or `flaky`
- Dynamic test generation
- Strong integration with `ZLayer`
- Built-in property-based testing
- Test reporting utilities like `zio.test.diff.Diff`

</v-clicks>

---

# ZIO Test

### Classic assertions

```scala
test("Check assertions") {
  assert(Right(Some(2)))(Assertion.isRight(Assertion.isSome(Assertion.equalTo(2))))
}
```

- Discoverable by the `Assertion` object
- Composes well 
  - Operations like `&&` and `||`
  - Generic combinator design e.g. `def exists[A](assertion: Assertion[A]): Assertion[Iterable[A]]`

### Smart assertions

```scala
test("sum"){
  assertTrue(1 + 1 == 2)
}
```

It uses the `assertTrue` function, which uses _macro_ under the hood.
---

# ZIO Test

### First test with TestConsole

```scala
object HelloWorld {
  def sayHello = Console.printLine("Hello, World!")
}

object HelloWorldSpec extends ZIOSpecDefault {
  def spec = suite("HelloWorldSpec")(
    test("sayHello correctly displays output") {
      for {
        _      <- HelloWorld.sayHello
        output <- TestConsole.output
      } yield assertTrue(output == Vector("Hello, World!\n"))
    }
  )
}
```

---

# ZIO Test

### Dynamic test generation

```scala
def add(a: Int, b: Int): Int = a + b

// loads a csv with 3 columns each returning an int
def loadTestData: Task[List[((Int, Int), Int)]] =
  ???
  
def makeTest(a: Int, b: Int)(expected: Int): Spec[Any, Nothing] =
  test(s"test add($a, $b) == $expected") {
    assertTrue(add(a, b) == expected)
  }

def makeTests: ZIO[Any, Throwable, List[Spec[Any, Nothing]]] =
  loadTestData.map { testData =>
    testData.map { case ((a, b), expected) => makeTest(a, b)(expected) }
  } 

object AdditionSpec extends ZIOSpecDefault {
  override def spec = suite("add")(makeTests)
}
```

---

# ZIO Test

### Property based testing

```scala
object AdditionSpec extends ZIOSpecDefault {

  def add(a: Int, b: Int): Int = ???

  def spec = suite("Add Spec")(
    test("add is commutative") {
      check(Gen.int, Gen.int) { case (a, b) => assertTrue(add(a, b) == add(b, a)) }
    },
    test("add is associative") {
      check(Gen.int, Gen.int, Gen.int) { case (a, b, c) => assertTrue(add(add(a, b), c) == add(a, add(b, c))) }
    },
    test("add is identitive") {
      check(Gen.int)(a => assertTrue(add(a, 0) == a))
    }
  )
}
```

--- 

# ZIO Test

### Aspects 1/2

```scala
// nonFlaky
test("random value is always greater than zero") {
  for {
    random <- Random.nextIntBounded(100)
  } yield assertTrue(random > 0)
} @@ nonFlaky

// retry
test("retrying a failing test based on the schedule until it succeeds") {
  ZIO.debug("retrying a failing test").map(_ => assertTrue(true))
} @@ TestAspect.retry(Schedule.recurs(5))

```

---

# ZIO Test

### Aspects 2/2

```scala
// timeout
test("effects can be safely interrupted") {
  for {
    _ <- ZIO.attempt(println("Still going ...")).forever
  } yield assertTrue(true)
} @@ TestAspect.timeout(1.second)

// conditional
test("env")(assertTrue(true)) @@ TestAspect.ifEnv("ENV")(_ == "testing")

// diagnose - dump fiber dumps
test("dump")(asserTrue(true)) @@ TestAspect.diagnose
```

---

# ZIO Test

### Sharing layers - Base class

```scala
abstract class SharedCounterSpec extends ZIOSpec[Counter] {
  override val bootstrap: ZLayer[Any, Nothing, Counter] = Counter.layer
}
```

### Spec1.scala
```scala
object Spec1 extends SharedCounterSpec {
  def spec = test("test1")(assertTrue(true)) @@ TestAspect.after(Counter.inc)
}
```

### Spec2.scala
```scala
object Spec2 extends SharedCounterSpec {
  def spec = test("test2")(assertTrue(true)) @@ TestAspect.after(Counter.inc)
}
```

### Ouput

```text
Counter initialized!
+ test1
+ test2
Number of tests executed: 2
```

---

# ZIO Test

### zio.test.diff.Diff type-class

```scala

trait Diff[A] {
  def diff(x: A, y: A): DiffResult
}

sealed trait DiffResult
object DiffResult {
  case class Nested(label: String, fields: List[(Option[String], DiffResult)]) extends DiffResult
  case class Different(oldValue: Any, newValue: Any, customRender: Option[String] = None) extends DiffResult
  case class Removed(oldValue: Any) extends DiffResult
  case class Added(newValue: Any) extends DiffResult
  case class Identical(value: Any) extends DiffResult
}
```

---

# ZIO Test


### Custom types

```scala
// somewhere defined in your domain package
case class Percentage(repr: Int)

implicit val diffPercentage: Diff[Percentage] = 
  Diff[Int].contramap(_.repr)
```

### Be wary of `LowPriDiff`

```scala
implicit def anyValDiff[A <: AnyVal]: Diff[A] = anyDiff[A]
```

---

# ZIO Test - Diff

### Derive `Diff` for case classes and algebraic data types

Include the module `zio-test-magnolia`

### Output

```scala
import zio.test._
import zio.test.magnolia.DeriveDiff._

case class Point(x: Int, y: Int)

object DiffSpec extends ZIOSpecDefault{
  def spec = suite("DiffSpec")(
    test("show diff from point")(assertTrue(Point(1, 1) == Point(3, 1)))
  )
}
```

#### Shows where it's different

```text
Diff -expected +obtained
Point(
  x = 3 → 1
)
```

---

# ScalaTest - Diff

```scala
case class Point(x: Int, y: Int)

import org.scalatest.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScalaTestDiffSpec extends AnyWordSpec with Matchers {
  "Point" should {
    "show difference" in {
      Point(1, 2) shouldBe Point(2, 3)
    }
  }
}
```

#### Dump's the whole case class

```text
Point(1, 2) was not equal to Point(2, 3)
Expected :Point(2, 3)
Actual   :Point(1, 2)
```

---

# ZIO Test and Scalatest

### Retry - Scalatest

```scala

import org.scalatest._
import tagobjects.Retryable

class SetSpec extends FlatSpec with Retries {

  override def withFixture(test: NoArgTest) = {
    if (isRetryable(test))
      withRetry { super.withFixture(test) }
    else
      super.withFixture(test)
  }

  "An empty Set" should "have size 0" taggedAs(Retryable) in {
    assert(Set.empty.size === 0)
  }
}
```

---

# ZIO Test and Scalatest

### Retry - ZIO

```scala
test("repeating a test based on the scheduler to ensure it passes every time") {
  ZIO.debug("repeating successful tests")
    .map(_ => assertTrue(true))
} @@ TestAspect.repeat(Schedule.recurs(5))
```

---

# ZIO Test and Scalatest

### Option checking - Scalatest

DSL which you need to read and learn about

```scala
maybeValue shouldBe Some("Hello") // Checks for Some("Hello")
maybeValue shouldNot be(None)     // Checks that it's not None
```

```scala
import org.scalatest.OptionValues._
import org.scalatest.matchers.should.Matchers._

maybeValue.value should be("Hello")  // Access the value and test it
```

---

# ZIO Test and Scalatest

### Option checking - zio-test

#### Smart assertions

```scala
assertTrue(maybeValue == Some(3))
```

#### Classic assertions

```scala
assert(maybeValue)(Assertion.isNone)
assert(maybeValue)(Assertion.isSome(Assertions.isGreaterThan(0)))
```

---

# ZIO Test and Scalatest

|                      | ZIO test        | Scalatest                  |
|----------------------|-----------------|----------------------------|
| Property-based-test  | ✅               | scalacheck                 |
| Test aspects         | ✅               | ad-hoc                     |
| Layer support        | ✅               | ❌                          |
| Readable diffs       | ✅               | ❌                          |
| Mocking              | zio-mock        | intergration modules       |
| Different test styles | define yourself | `FunSpec`, `FlatSpec`, etc |
| Custom assertions    | ✅               | ✅                          |
| Assert macros        | ✅               | ✅                          |
| Async testing | ✅               | via trait `Async{XXX}Spec` |

---

# ZIO Test and Scalatest

### Test aspects

|                | ZIO test               | Scalatest                      |
|----------------|------------------------|--------------------------------|
| `beforeAll`    | `TestAspect.beforeAll` | via trait `BeforeAll`          |
| `afterAll`     | `TestAspect.afterAll`  | via trait `AfterAll`           |
| Retries        | `TestAspect.retry`     | via trait `Retries`    |
| Async timeouts | `TestAspect.timeout`   | via overriding `PatienceConfig` |
| Flaky tests    | `TestAspect.flaky`     | No support for it              |
| Ignoring tests | `TestAspect.ignore`    | Special method and syntax `ignored` |
| Fiber dumps    | `TestAspect.diagnose`  | No support for it              |

---

# ZIO Test and Scalatest

### Conclusion

<v-clicks>

- ZIO test has **strong** integration with `ZLayer` 
- ZIO test has support for ZIO services like `Console`
- In scalatest the trait jungle gives you async tests, specific matchers, etcetra
- Cross-cutting concerns are defined in ZIO by test aspects, while scalatest is **ad-hoc** (clunky)
- ZIO test comes with **property based testing** and **diffing** out of the box
- Scalatest comes with fixtures and test styles
  - These concepts you need to _learn_ or _fight_ over
  - While in ZIO test you just use functions
- Scalatest _lacks_ support for **flaky tests** and **fiber dumps** 

</v-clicks>

---
layout: center
class: 'text-center pb-5'
---

# Thank You!

[https://github.com/Fristi/zio-test-presentation](https://github.com/Fristi/zio-test-presentation)