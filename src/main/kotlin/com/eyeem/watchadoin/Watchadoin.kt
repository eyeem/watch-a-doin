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
     * Thread on which this stopwatch was executed
     */
    var tid : Long = 0
        private set

    /**
     * Time of start
     */
    var start : Long = 0
        private set

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
    var timeoutAt : Long = 0L
        private set

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
        tid = Thread.currentThread().id
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
                tid = tid,
                duration = duration(),
                relativeStart = relativeStartTime,
                timeout = timeoutAt > 0L,
                parent = if (includeParent) parent else null,
                nestLvl = nestLvl
        )
        timelines += timeline

        children.forEach {
            timelines += it.timelines(nestLvl + 1, startTime, timeline, includeParent)
        }

        return timelines
    }

    fun toStringPretty() : String = timelines().joinToString(separator = "\n") { it.report() }
}

/**
 * Report counterpart of the [Stopwatch] class
 *
 * @param name name of the timeline
 * @param tid thread ID
 * @param duration duration in ms of this timeline
 * @param relativeStart relativeStart (counting from the first timeline in the group) in ms of this timeline
 * @param timeout whether or not this timeline timed out
 * @param parent parent of this timeline
 * @param nestLvl nesting level related to the first timeline in the group
 */
data class Timeline(
        val name: String,
        val tid: Long,
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


