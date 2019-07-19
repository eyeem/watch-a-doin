package com.eyeem.watchadoin

import org.junit.Test
import java.io.File

class WatchadoinTest {
    @Test
    fun test1() {
        val loopWatch = Stopwatch("loop")

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
        val svgFile = File("test1.svg")
        svgFile.printWriter().use { out -> out.println(svgOutput) }
        println("SVG timeline report saved to file://${svgFile.absolutePath}")
    }
}