package com.eyeem.watchadoin

import org.junit.Test
import java.io.File

class WatchadoinTest {
    @Test
    fun test1() {
        val loopWatch = Stopwatch("🔁 loop")

        fun expensiveOperation(stopwatch: Stopwatch) = stopwatch {
            Thread.sleep(125)
        }

        fun moreExpensiveOperation(stopwatch: Stopwatch) = stopwatch {
            Thread.sleep(375)
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

        val svgFile = File("test1.svg")
        loopWatch.saveAsSvg(svgFile)
        println("SVG timeline report saved to file://${svgFile.absolutePath}")
    }
}