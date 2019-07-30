# Watch-A-Doin'?

![](https://user-images.githubusercontent.com/121164/62126227-7c9ab980-b2cf-11e9-970b-316bd8028ba7.png)

Simple Kotlin library that can be used to monitor timings of some expensive operations and nested calls they make.

Using this library can help you in identifying the most offending timings and figuring out future solutions towards general betterness.

## Getting started

This repository is hosted via [jitpack](https://jitpack.io/) since it's by far the easiest delivery method while also being pretty transparent to the developer.

Make sure you have added jitpack to the list of your repositories:

```kotlin
maven("https://jitpack.io")
```

Then simply add the `watch-a-doin` dependency

```kotlin
dependencies {
    compile("com.github.eyeem:watch-a-doin:master-SNAPSHOT")
}
```

## Example usage

```kotlin
val loopWatch = Stopwatch("🔁 loop")

fun expensiveOperation(stopwatch: Stopwatch) = stopwatch {
    Thread.sleep(125)
}

fun moreExpensiveOperation(stopwatch: Stopwatch) = stopwatch {
    Thread.sleep(375)
}

loopWatch {
    for (i in 0 until 4) {
        "⏭️ iteration $i".watch {
            expensiveOperation("🕰️".watch)
            moreExpensiveOperation("🕰 x3".watch)
        }
    }
}

println(loopWatch.toStringPretty())

loopWatch.saveAsSvg(File("loopWatch.svg"))
loopWatch.saveAsHtml(File("loopWatch.html")) // interactive svg with pan and zoom

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

![Screenshot 2019-07-22 at 17 53 01](https://user-images.githubusercontent.com/121164/61646360-a76e8780-aca9-11e9-92f3-cf3181f259d2.png)

### Timeouts

Some stopwatches might finish after others, some might never finish. Consider following example using `launch`.

```kotlin
val baseWatch = Stopwatch("runBlocking")

baseWatch {
    runBlocking {
        "launch".watch {
            launch {
                "insideLaunch".watch {
                    delay(500)
                }
            }
        }
    }
}
```

![Screenshot 2019-07-29 at 12 28 44](https://user-images.githubusercontent.com/121164/62041686-80a9d700-b1fc-11e9-8e8c-914382c9e182.png)

Now if we make `insideLaunch` watch never call end (e.g. due to some error) the top watch will timeout it on its end.


```kotlin
val insideWatch = "insideLaunch".watch
insideWatch.start()
delay(500)
// insideWatch.end()
```

![Screenshot 2019-07-29 at 12 33 44](https://user-images.githubusercontent.com/121164/62041942-22312880-b1fd-11e9-98dc-e22618e860b4.png)

Timeout timelines are marked red.

You can explicitly timeout any watch with `timeout()` method or timeout any running child watches with `timeoutAllRunningChildren()`.

__NOTE__: If `end()` is called after `timeout()` it will only update timeout time.

## More Resources

- [Watcha Doin'? Inspecting Kotlin coroutines with timing graphs.](https://proandroiddev.com/watcha-doin-inspecting-kotlin-coroutines-with-timing-graphs-1676132d940f) - Medium Article
