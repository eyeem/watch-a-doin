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

    private lateinit var job : Job
    private lateinit var scope : CoroutineScope

    @Before
    fun setup() {
        job = Job()
        scope = CoroutineScope(job + Executors.newFixedThreadPool(1).asCoroutineDispatcher())
    }

    @After
    fun tearDown() {
        job.cancel()
    }

    @Test
    fun `Test 1 - Linear Sleep`() {
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            for (i in 0 until 4) {
                val iterationWatch = loopWatch["⏭️ iteration"]
                iterationWatch {
                    expensiveSleep(iterationWatch["🕰️"])

                    moreExpensiveSleep(iterationWatch["🕰 x3"])
                }
            }
        }

        println(loopWatch.toStringPretty())

        val svgFile = File("test1.svg")
        loopWatch.saveAsSvg(svgFile)
        println("SVG timeline report saved to file://${svgFile.absolutePath}")
    }

    @Test
    fun `Test 2 - Linear Sleep On Coroutines`() = runBlocking{
        val loopWatch = Stopwatch("🔁 loop")

        loopWatch {
            val jobs = mutableListOf<Job>()

            for (i in 0 until 4) {
                jobs += scope.async {
                    val iterationWatch = loopWatch["⏭️ iteration"]
                    iterationWatch {
                        expensiveSleep(iterationWatch["🕰️"])

                        moreExpensiveSleep(iterationWatch["🕰 x3"])
                    }
                }
            }

            jobs.forEach { it.join() }
        }

        println(loopWatch.toStringPretty())

        val svgFile = File("test2.svg")
        loopWatch.saveAsSvg(svgFile)
        println("SVG timeline report saved to file://${svgFile.absolutePath}")
    }
}