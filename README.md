![](https://media.giphy.com/media/XcYIX8UQuhKRbdEp2z/giphy.gif)

Simple Kotlin library that can be used to monitor timings of some expensive operations and nested calls they make.

Using this library can help you in identifying the most offending timings and figuring out future solutions towards general betterness.

## Getting started

This repository is hosted via [jitpack](https://jitpack.io/) since it's by far the easiest delivery method while also being pretty transparent to the developer.

Make sure you have added jitpack to the list of your repositories:

```kotlin
maven("https://jitpack.io")
```

Then simply add the `watchadoin` dependency

```kotlin
dependencies {
    compile("com.github.eyeem:watchadoin:master-SNAPSHOT")
}
```

## Example usage

```kotlin
val loopWatch = Stopwatch("root")

fun expensiveOperation() {
    Thread.sleep(100)
}

fun moreExpensiveOperation() {
    Thread.sleep(500)
}

loopWatch {
    for (i in 0 until 4) {
        val iterationWatch = loopWatch["iteration $i"]
        iterationWatch {
            iterationWatch["expensiveOperation"] {
                expensiveOperation()
            }

            iterationWatch["moreExpensiveOperation"] {
                moreExpensiveOperation()
            }
        }
    }
}

val timelines = loopWatch.timelines()
timelines.forEach {
    println(it.report())
}

val svgOutput = SvgReport(timelines).print()
svgFile.printWriter().use { out -> out.println(svgOutput) }

```

Will print this:

```
loop [2410ms @0ms]
 iteration 0 [602ms @0ms]
  expensiveOperation [102ms @0ms]
  moreExpensiveOperation [500ms @102ms]
 iteration 1 [601ms @602ms]
  expensiveOperation [100ms @602ms]
  moreExpensiveOperation [501ms @702ms]
 iteration 2 [601ms @1203ms]
  expensiveOperation [100ms @1203ms]
  moreExpensiveOperation [500ms @1303ms]
 iteration 3 [606ms @1804ms]
  expensiveOperation [102ms @1804ms]
  moreExpensiveOperation [504ms @1906ms]
```

and create the following SVG:

![](https://user-images.githubusercontent.com/121164/61558196-a7cb1080-aa66-11e9-9cf7-4ec83199f5f1.png)