package com.eyeem.watchadoin

/**
 * Support for Trace Event Format
 *
 * https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit
 *
 * WANRING: Looks super bad if you try using it with one thread and multiple coroutines running in parallel.
 *
 * USAGE: open chrome and type `chrome://tracing` in the address bar
 */
data class TraceEventsReport(val traceEvents: List<TraceEvent>, val displayTimeUnit: String = "ns")

data class TraceEvent(val cat: String? = null, val pid: Int, val tid: Long, val id: Long? = null, val ts: Long, val dur: Long? = null, val ph: String, val name: String, val cname: String? = null)


fun Stopwatch.asTraceEventsReport() : TraceEventsReport {
    val traces = traceEventList().sortedBy { it.ts }
    return TraceEventsReport(traceEvents = traces)
}

private fun Stopwatch.traceEventList(startTime: Long = start) : List<TraceEvent> = timelines().map {
    TraceEvent(
        name = it.name,
        cat = "watchadoin",
        ph = "X",
        ts = it.relativeStart * 1000,
        dur = it.duration * 1000,
        pid = 1,
        tid = it.tid
    )
}