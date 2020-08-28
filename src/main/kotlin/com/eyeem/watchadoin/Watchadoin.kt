package com.eyeem.watchadoin

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.time.*

/**
 *
 * **WATCHADOIN?!**
 *
 * Simplistic stopwatch for identifying time spent in methods
 *
 * @param name The name of this stopwatch
 */
@OptIn(ExperimentalTime::class)
class Stopwatch(val name: String, private val parent: Stopwatch? = null) {

    /**
     * Thread on which this stopwatch was executed
     */
    var tid: Long = 0
        private set

    /**
     * Marks the time of start
     */
    private lateinit var mark: TimeMark

    private var relativeToParent: Duration = Duration.ZERO

    /**
     * Duration of clock
     * [Duration.ZERO] until Stopwatch completed
     */
    private var timeElapsed: Duration = Duration.ZERO

    /**
     * Whether or not this stopwatch is still running
     */
    private var isRunning = false

    /**
     * Time at which timeout occurred
     */
    var timeoutAt: Duration = Duration.ZERO
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
    inline operator fun <T> invoke(block: Stopwatch.() -> T): T {
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
        mark = TimeSource.Monotonic.markNow()
        parent?.mark?.elapsedNow()?.let {
            relativeToParent = it
        }
        isRunning = true
        tid = Thread.currentThread().id
    }

    /**
     * End watch measurement
     */
    fun end() {
        if (!isRunning) {
            if (timeoutAt > Duration.ZERO) {
                // we finished but we managed to get timed out
                timeoutAt = mark.elapsedNow()
            }
            return
        }
        timeElapsed = mark.elapsedNow()
        isRunning = false

        // check recursively for watches that did not call end()
        if (parent == null) {
            timeoutAllRunningChildren(timeElapsed)
        }
    }

    /**
     * Times out all running child watches
     */
    fun timeoutAllRunningChildren(timeoutAt: Duration) {
        children.forEach {
            if (it.isRunning) {
                it.timeout(timeoutAt)
            }
            it.timeoutAllRunningChildren(timeoutAt)
        }
    }

    /**
     * Total duration of the stopwatch
     */
    fun duration(): Duration {
        return if(timeElapsed > timeoutAt) {
            timeElapsed
        }else {
            timeoutAt
        }
    }

    /**
     * Timeout the watch
     *
     * @param shouldEnd force stop watch otherwise it will wait till [end] is called
     */
    fun timeout(duration: Duration) {
        if (!isRunning) return
        isRunning = false
        timeoutAt = duration
    }

    val String.watch: Stopwatch
        get() = createChild(this)

    private fun createChild(name: String): Stopwatch {
        val childStopwatch = Stopwatch(name, this)
        children.add(childStopwatch)
        return childStopwatch
    }

    /**
     * Produce a report for this [Stopwatch] and its children
     */
    fun timelines(
        nestLvl: Int = 0,
        relativeStartTime: Duration = Duration.ZERO,
        parent: Timeline? = null,
        includeParent: Boolean = false
    ): List<Timeline> {
        val timelines = ArrayList<Timeline>()
        val timeline = Timeline(
            name = name,
            tid = tid,
            duration = duration(),
            relativeStart = relativeStartTime + relativeToParent,
            timeout = timeoutAt > Duration.ZERO && timeElapsed <= Duration.ZERO,
            parent = if (includeParent) parent else null,
            nestLvl = nestLvl
        )
        timelines += timeline

        children.forEach {
            timelines += it.timelines(nestLvl + 1, relativeStartTime, timeline, includeParent)
        }

        return timelines
    }

    fun toStringPretty(): String = timelines().joinToString(separator = "\n") { it.report() }

    companion object {
        /**
         * TL;DR: You should avoid using this method. There might be use cases when you need it.
         *
         * Stopwatch “forces” creating children within the parent’s context by design - otherwise at some
         * point it is getting confusing who is the owner of the child. Nesting scopes makes code readable
         * and easier to follow
         *
         * Nonetheless, there might be cases where creating a watch outside of a scope might be
         * desirable, e.g. imagine using Stopwatch with Android's Activity lifecycle.
         *
         * ```kotlin
         * class MyActivty : Activity {
         *   lateinit activityWatch : Stopwatch
         *   fun onCreate() {
         *     activityWatch = Stopwatch("activity")
         *     activityWatch.start()
         *   }
         *
         *   fun onClickSomething() {
         *     val clickWatch = Stopwatch.bastard(activityWatch, "clickWatch")
         *     clickWatch {
         *       // some operation
         *     }
         *   }
         *
         *   fun onDestroy() {
         *     activityWatch.stop()
         *   }
         * }
         * ```
         */
        fun bastard(parent: Stopwatch, name: String): Stopwatch = parent.createChild(name)
    }
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
@OptIn(ExperimentalTime::class)
data class Timeline(
    val name: String,
    val tid: Long,
    val duration: Duration,
    val relativeStart: Duration,
    val timeout: Boolean,
    val parent: Timeline?,
    val nestLvl: Int
) {

    /**
     * One liner about this timeline
     */
    fun report(): String = "${" ".repeat(nestLvl)}$name [${duration}ms @${relativeStart}ms]${if (timeout) "!!" else ""}"
}


