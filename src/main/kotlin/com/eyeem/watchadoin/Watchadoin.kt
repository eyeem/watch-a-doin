package com.eyeem.watchadoin

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.roundToLong

/**
 *
 * **WATCHADOIN?!**
 *
 * Simplistic stopwatch for identifying time spent in methods
 *
 * @param name The name of this stopwatch
 */
class Stopwatch(val name: String) {

    /**
     * Time of start
     */
    private var start : Long = 0

    /**
     * Time of end
     */
    private var end : Long = 0

    /**
     * Whether or not this stopwatch is still running
     */
    private var isRunning = false

    /**
     * Time at which timeout occurred
     */
    private var timeoutAt : Long = 0L

    /**
     * Child watches (a.k.a. nested timelines)
     */
    var children = CopyOnWriteArrayList<Stopwatch>()

    /**
     * Measure time of the block, store results in the [Stopwatch] instance:
     *
     * ```kotlin
     * val stopwatch = Stopwatch("myWatch")
     *
     * stopwatch {
     *   expensiveOperation()
     * }
     *s
     * ```
     * @param block block of code to be measured
     */
    inline operator fun <T> invoke(block: () -> T) : T {
        start()
        val t = block()
        end()
        return t
    }

    /**
     * Start watch measurement
     */
    fun start() {
        if (isRunning) return
        start = System.currentTimeMillis()
        isRunning = true
    }

    /**
     * End watch measurement
     */
    fun end() {
        if (!isRunning) return
        end = System.currentTimeMillis()
        isRunning = false
        children.forEach {
            if (it.isRunning) {
                it.timeout(false)
            }
        }
    }

    /**
     * Total duration of the stopwatch
     */
    fun duration() : Long = max(max(end, timeoutAt) - start, 0)

    /**
     * Timeout the watch
     *
     * @param shouldEnd force stop watch otherwise it will wait till [end] is called
     */
    fun timeout(shouldEnd: Boolean = true) {
        if (!isRunning || timeoutAt > 0L) return
        if (shouldEnd) {
            end = System.currentTimeMillis()
            isRunning = false
        }
        timeoutAt = System.currentTimeMillis()
        children.forEach { it.timeout(false) }
    }

    operator fun get(name: String) : Stopwatch {
        val childStopwatch = Stopwatch(name)
        children.add(childStopwatch)
        return childStopwatch
    }

    /**
     * Produce a report for this [Stopwatch] and its children
     */
     fun timelines(nestLvl: Int = 0, startTime: Long = start, parent: Timeline? = null, includeParent : Boolean = false) : List<Timeline> {
        val relativeStartTime = start - startTime
        //

        val timelines = ArrayList<Timeline>()
        val timeline = Timeline(
                name = name,
                duration = duration(),
                relativeStart = relativeStartTime,
                timeout = timeoutAt > 0L,
                parent = if (includeParent) parent else null,
                nestLvl = nestLvl
        )
        timelines += timeline

        children.forEach {
            timelines += it.timelines(nestLvl + 1, startTime, timeline)
        }

        return timelines
    }
}

/**
 * Report counterpart of the [Stopwatch] class
 *
 * @param name name of the timeline
 * @param duration duration in ms of this timeline
 * @param relativeStart relativeStart (counting from the first timeline in the group) in ms of this timeline
 * @param timeout whether or not this timeline timed out
 * @param parent parent of this timeline
 * @param nestLvl nesting level related to the first timeline in the group
 */
data class Timeline(
        val name: String,
        val duration : Long,
        val relativeStart : Long,
        val timeout: Boolean,
        val parent: Timeline?,
        val nestLvl: Int) {

    /**
     * One liner about this timeline
     */
    fun report() : String = "${" ".repeat(nestLvl)}$name [${duration}ms @${relativeStart}ms]${if (timeout) "!!" else ""}"
}

/**
 * Log Timelines using [println]
 *
 * @param timelines output of [Stopwatch.report]
 */
class TxtReport(val timelines: List<Timeline>) {

    /**
     * Print the report to stdout, line by line
     */
    fun print() {
        timelines.forEach {
            println(it.report())
        }
    }
}

/**
 * Convert Timelines report to a simple SVG
 *
 * @param timelines output of [Stopwatch.report]
 */
class SvgReport(val timelines: List<Timeline>) {

    private val padding = 10
    private val timelineHeight = 30
    private val fontSize = 12
    private val xAxisHeight = 8
    private val scaleGridDistance = 50

    private val maxPageWidth = 1200
    private var xScale = 1.0f

    /**
     * @return SVG formatted string with the report in it
     */
    fun print() : String {
        val scaleLength = timelines.map { it.duration + it.relativeStart }.maxBy { it } ?: throw IllegalStateException("no maximum found")
        val scaleSteps = scaleLength / scaleGridDistance
        val svgWidth = scaleLength + padding * 2
        val svgHeight = (timelines.size + 1) * padding + timelines.size * timelineHeight + xAxisHeight + padding

        if (svgWidth > maxPageWidth) {
            xScale = maxPageWidth.toFloat() / svgWidth.toFloat()
        }

        var output = """<svg xmlns="http://www.w3.org/2000/svg" width="${svgWidth*xScale}" height="$svgHeight">"""

        // draw the timeline grid and axis
        val omit = (1f / xScale).roundToLong()

        for (scaleStep in 0..scaleSteps) {

            if (scaleStep % omit != 0L) {
                continue
            }

            var x = scaleGridDistance * scaleStep + padding
            x = (x * xScale).toLong()
            val y1 = padding
            val yLength = (timelines.size - 1) * padding + timelines.size * timelineHeight


            output += """<g>
                            <line stroke-dasharray="5, 5" x1="$x" y1="$y1" x2="$x" y2="${y1 + yLength}" style="stroke-width:1;stroke:rgba(0,0,0,0.5)"/>
                            <text x="$x" y="${y1 + yLength + padding}" font-family="Verdana" font-size="$xAxisHeight" fill="#00000077">${scaleStep*scaleGridDistance}ms</text>
                         </g>""".trimIndent()
        }

        // draw the timelines
        timelines.forEachIndexed { index, timeline ->

            var x1 = timeline.relativeStart + padding
            x1 = (x1 * xScale).toLong()
            val y1 = (index + 1) * padding + index * timelineHeight
            val y1Text = y1 + (timelineHeight - fontSize)
            val sqColor = if (!timeline.timeout) {
                "76,175,80"
            } else {
                "244,67,54"
            }

            val alpha = 0.25f + max(0f, 0.75f - 0.2f * timeline.nestLvl)

            output += """<g>
                           <rect x="$x1" y="$y1" width="${(timeline.duration * xScale).toLong()}" height="$timelineHeight" style="fill:rgba($sqColor,$alpha);"></rect>
                           <text x="${x1 + padding}" y="$y1Text" font-family="Verdana" font-size="$fontSize" fill="#000000">${timeline.name}</text>
                         </g>""".trimIndent()
        }

        output += "</svg>"
        return output
    }

}
