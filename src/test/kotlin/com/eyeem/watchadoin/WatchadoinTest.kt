package com.eyeem.watchadoin

import com.google.gson.Gson
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Test
import java.io.File

fun expensiveSleep(stopwatch: Stopwatch): Int = stopwatch {
    Thread.sleep(125)
    return@stopwatch 1
}

fun moreExpensiveSleep(stopwatch: Stopwatch): Int = stopwatch {
    Thread.sleep(375)
    return@stopwatch 2
}

suspend fun expensiveDelay(stopwatch: Stopwatch): Int = stopwatch {
    delay(125)
    return@stopwatch 1
}

suspend fun moreExpensiveDelay(stopwatch: Stopwatch): Int = stopwatch {
    delay(375)
    return@stopwatch 2
}

class WatchadoinTest {

    val dryRun = false

    @Before
    fun warmUp() {
        runBlocking {
            launch {
                Stopwatch("warm up").invoke {
                    delay(10)
                }
            }
        }
    }

    @Test
    fun `Test 0 - API usage`() {
        val stopwatch = Stopwatch("main")

        val sum = stopwatch {
            expensiveSleep("expensiveOperation".watch) +
                    moreExpensiveSleep("moreExpensiveOperation".watch)
        }
        print(sum)

        stopwatch.asTestReport("test0", dryRun)
    }

    @Test
    fun `Test 1 - Linear Sleep`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            for (i in 0 until 4) {
                "⏭️ iteration $i".watch {
                    expensiveSleep("💤".watch)
                    moreExpensiveSleep("💤 x3".watch)
                }
            }
        }

        loopWatch.asTestReport("test1", dryRun)

    }

    @Test
    fun `Test 2 - Sleep On Coroutines + GlobalScope`() = runBlocking {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            val jobs = mutableListOf<Job>()

            for (i in 0 until 4) {
                jobs += GlobalScope.async {
                    "⏭️ iteration $i".watch {
                        expensiveSleep("💤".watch)
                        moreExpensiveSleep("💤 x3".watch)
                    }
                }
            }

            jobs.forEach { it.join() }
        }

        loopWatch.asTestReport("test2", dryRun)
    }

    @Test
    fun `Test 3 - Sleep On Coroutines + Run Blocking`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            runBlocking {
                for (i in 0 until 4) {
                    launch {
                        "⏭️ iteration $i".watch {
                            expensiveSleep("💤".watch)
                            moreExpensiveSleep("💤 x3".watch)
                        }
                    }
                }
            }
        }

        loopWatch.asTestReport("test3", dryRun)
    }

    @Test
    fun `Test 4 - Delay On Coroutines + Run Blocking`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            runBlocking {
                for (i in 0 until 4) {
                    launch {
                        "⏭️ iteration $i".watch {
                            expensiveDelay("🕰".watch)
                            moreExpensiveDelay("🕰️ x3".watch)
                        }
                    }
                }
            }
        }

        loopWatch.asTestReport("test4", dryRun)
    }

    @Test
    fun `Test 5 - Delay On Coroutines + Run Blocking + WTF`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            val initWatch =  "🚀 init".watch.apply { start() }
            runBlocking {
                initWatch.end()
                for (i in 0 until 4) {
                    launch {
                        "⏭️ iteration $i".watch {
                            expensiveDelay("🕰".watch)
                            moreExpensiveDelay("🕰️ x3".watch)
                        }
                    }
                }
            }
        }

        loopWatch.asTestReport("test5", dryRun)
    }

    @Test
    fun `Test 6 - Delay On Coroutines + Run Blocking + Failing Async`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            runBlocking {
                for (i in 0 until 4) {
                    launch {
                        "⏭️ iteration $i".watch {
                            val expensiveResult = async {  expensiveDelay("🕰".watch) }.await()
                            val moreExpensiveResult = async { moreExpensiveDelay("🕰️ x3".watch) }.await()
                            println("combined result -> ${expensiveResult + moreExpensiveResult}")
                        }
                    }
                }
            }
        }

        loopWatch.asTestReport("test6", dryRun)
    }

    @Test
    fun `Test 7 - Delay On Coroutines + Run Blocking + Proper Async`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            runBlocking {
                for (i in 0 until 4) {
                    launch {
                        "⏭️ iteration $i".watch {
                            val expensiveResult = async {  expensiveDelay("🕰".watch) }
                            val moreExpensiveResult = async { moreExpensiveDelay("🕰️ x3".watch) }
                            println("combined result -> ${expensiveResult.await() + moreExpensiveResult.await()}")
                        }
                    }
                }
            }
        }

        loopWatch.asTestReport("test7", dryRun)
    }

    @Test
    fun `Test 8 - Timeout Scenario 1`() {
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

        baseWatch.asTestReport("test8", dryRun)
    }

    @Test
    fun `Test 9 - Timeout Scenario 2, end never called`() {
        val baseWatch = Stopwatch("runBlocking")

        baseWatch {
            runBlocking {
                "launch".watch {
                    launch {
                        val insideWatch = "insideLaunch".watch
                        insideWatch.start()
                        delay(500)
                        // insideWatch.end()
                    }
                }
            }
        }

        baseWatch.asTestReport("test9", dryRun)
    }
}

private fun Stopwatch.asTestReport(name: String, dryRun: Boolean) {
    println(this.toStringPretty())

    val svgFile = File("$name.svg")
    this.saveAsSvg(svgFile, dryRun)
    println("SVG timeline report saved to file://${svgFile.absolutePath}")

    val htmlFile = File("$name.html")
    this.saveAsHtml(htmlFile, dryRun)
    println("HTML timeline report saved to file://${htmlFile.absolutePath}")

    val traceJson = Gson().toJson(this.asTraceEventsReport())
    val traceJsonFile = File("$name.json")
    traceJsonFile.printWriter().use { it.write(traceJson) }
    println("Trace event report file://${traceJsonFile.absolutePath}")
}