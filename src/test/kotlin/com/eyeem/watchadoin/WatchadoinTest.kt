package com.eyeem.watchadoin

import kotlinx.coroutines.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors

fun expensiveSleep(stopwatch: Stopwatch) = stopwatch {
    Thread.sleep(125)
}

fun moreExpensiveSleep(stopwatch: Stopwatch) = stopwatch {
    Thread.sleep(375)
}

class WatchadoinTest {

    @Test
    fun `Test 0 - API usage`() {
        val stopwatch = Stopwatch("main")

        stopwatch {
            expensiveSleep("expensiveOperation".watch)

            moreExpensiveSleep("moreExpensiveOperation".watch)
        }

        println(stopwatch.toStringPretty())

        val svgFile = File("test0.svg")
        stopwatch.saveAsSvg(svgFile)
        println("SVG timeline report saved to file://${svgFile.absolutePath}")
    }

    @Test
    fun `Test 1 - Linear Sleep`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            for (i in 0 until 4) {
                "⏭️ iteration $i".watch {
                    expensiveSleep("🕰️".watch)

                    moreExpensiveSleep("🕰 x3".watch)
                }
            }
        }

        println(loopWatch.toStringPretty())

        val svgFile = File("test1.svg")
        loopWatch.saveAsSvg(svgFile)
        println("SVG timeline report saved to file://${svgFile.absolutePath}")
    }

    @Test
    fun `Test 2 - Sleep On Coroutines`() = runBlocking {
        val loopWatch = Stopwatch("🔁 loop")

        val threadCount = 4
        val job = Job()
        val scope = CoroutineScope(job + Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher())

        loopWatch {
            val jobs = mutableListOf<Job>()

            for (i in 0 until 4) {
                jobs += scope.async {
                    "⏭️ iteration $i".watch {
                        expensiveSleep("🕰️".watch)

                        moreExpensiveSleep("🕰 x3".watch)
                    }
                }
            }

            jobs.forEach { it.join() }
        }

        println(loopWatch.toStringPretty())

        val svgFile = File("test2.svg")
        loopWatch.saveAsSvg(svgFile)
        println("SVG timeline report saved to file://${svgFile.absolutePath}")

        job.cancel()
    }
}