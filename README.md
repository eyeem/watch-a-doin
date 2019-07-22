# Watch-A-Doin'?

![](https://media.giphy.com/media/l0MYOUI5XfRk4LLWM/giphy.gif)

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
val loopWatch = Stopwatch("🔁 loop")

fun expensiveOperation(stopwatch: Stopwatch) = stopwatch {
    Thread.sleep(100)
}

fun moreExpensiveOperation(stopwatch: Stopwatch) = stopwatch {
    Thread.sleep(500)
}

loopWatch {
    for (i in 0 until 4) {
        val iterationWatch = loopWatch["⏭️ iteration $i"]
        iterationWatch {
            expensiveOperation(iterationWatch["🕰️"])

            moreExpensiveOperation(iterationWatch["🕰 x3"])
        }
    }
}

println(loopWatch.toStringPretty())

loopWatch.saveAs(File("loopWatch.svg"))

```

Will print this:

```
🔁 loop [2018ms @0ms]
 ⏭️ iteration 0 [506ms @0ms]
  🕰️ [127ms @0ms]
  🕰 x3 [379ms @127ms]
 ⏭️ iteration 1 [506ms @506ms]
  🕰️ [127ms @506ms]
  🕰 x3 [379ms @633ms]
 ⏭️ iteration 2 [503ms @1012ms]
  🕰️ [128ms @1012ms]
  🕰 x3 [375ms @1140ms]
 ⏭️ iteration 3 [503ms @1515ms]
  🕰️ [128ms @1515ms]
  🕰 x3 [375ms @1643ms]
```

and create the following SVG:

![Screenshot 2019-07-22 at 17 18 31](https://user-images.githubusercontent.com/121164/61644309-c0286e80-aca4-11e9-9e30-083eb22a35fd.png)