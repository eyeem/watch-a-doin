package com.eyeem.watchadoin

import java.io.File
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Convert Timelines report to a simple SVG
 *
 * @param timelines output of [Stopwatch.report]
 */
class SvgReport(val timelines: List<Timeline>) {

    private val padding = 10
    private val timelineHeight = 30
    private val fontSize = 12
    private val smallFontSize = 8
    private val xAxisHeight = 8
    private val scaleGridDistance = 50

    private val maxPageWidth = 1200


    /**
     * @return SVG formatted string with the report in it
     */
    override fun toString(): String {
        var xScale = 1.0f
        val scaleLength = timelines.map { it.duration + it.relativeStart }.maxBy { it }
            ?: throw IllegalStateException("no maximum found")
        val scaleSteps = scaleLength / scaleGridDistance
        val svgWidth = scaleLength + padding * 2
        if (svgWidth > maxPageWidth) {
            xScale = maxPageWidth.toFloat() / svgWidth.toFloat()
        }

        var timelinesSvg: String = "" // timelines as svg tags

        // collects timelines as we draw them, int here is a raw in which we draw timeline
        val rects = HashMap<Int, ArrayList<Rect>>()
        timelines.forEachIndexed { index, timeline ->

            val heightIndex = rects.firstAvailableRow(timeline)

            val _y1 = (heightIndex + 1) * padding + heightIndex * timelineHeight
            val _y1Text = (_y1 + (timelineHeight - fontSize)).toLong()
            val _x1 = ((timeline.relativeStart + padding) * xScale).toLong()
            val _rectWidth = (timeline.duration * xScale).toLong()
            val _rectHeight = timelineHeight.toLong()

            val rect = Rect(
                x1 = _x1,
                y1 = _y1.toLong(),
                y1Text = _y1Text,
                x2 = _x1 + _rectWidth,
                y2 = _y1 + _rectHeight,
                timeline = timeline,
                fillColor = if (!timeline.timeout) {
                    "76,175,80"
                } else {
                    "244,67,54"
                },
                alpha = 0.25f + max(0f, 0.75f - 0.2f * timeline.nestLvl),
                fontSize = fontSize,
                smallFontSize = smallFontSize,
                padding = padding,
                clipIndex = index,
                rowIndex = heightIndex
            )

            val rowTimelines = rects[heightIndex] ?: ArrayList<Rect>().apply {
                rects[heightIndex] = this
            }
            rowTimelines += rect

            timelinesSvg += rect.asSvgTimelineTag()
        }

        val rowCount = rects.values.size
        val svgHeight = (rowCount + 1) * padding + rowCount * timelineHeight + xAxisHeight + padding

        var output = """<svg xmlns="http://www.w3.org/2000/svg" width="${svgWidth * xScale}" height="$svgHeight">"""

        // draw the timeline grid and axis
        val omit = (1f / xScale).roundToLong()

        for (scaleStep in 0..scaleSteps) {

            if (scaleStep % omit != 0L) {
                continue
            }

            var x = scaleGridDistance * scaleStep + padding
            x = (x * xScale).toLong()
            val y1 = padding
            val yLength = (rowCount - 1) * padding + rowCount * timelineHeight


            output += """<g>
                            <line stroke-dasharray="5, 5" x1="$x" y1="$y1" x2="$x" y2="${y1 + yLength}" style="stroke-width:1;stroke:rgba(0,0,0,0.5)"/>
                            <text x="$x" y="${y1 + yLength + padding}" font-family="Verdana" font-size="$xAxisHeight" fill="#00000077">${scaleStep * scaleGridDistance}ms</text>
                         </g>""".trimIndent()
        }

        output += timelinesSvg

        output += "</svg>"
        return output
    }

}

fun Stopwatch.asSvgReport(): SvgReport = SvgReport(timelines(includeParent = true))

fun Stopwatch.saveAsSvg(file: File) {
    val svgOutput = asSvgReport().toString()
    file.printWriter().use { out -> out.println(svgOutput) }
}

private data class Rect(
    val x1: Long, val y1: Long,
    val y1Text: Long,
    val x2: Long, val y2: Long,
    val timeline: Timeline,
    val fillColor: String,
    val alpha: Float,
    val padding: Int,
    val clipIndex: Int,
    val rowIndex: Int,
    val fontSize: Int,
    val smallFontSize: Int
)

private val Rect.width
    get() = x2 - x1

private val Rect.height
    get() = y2 - y1

private fun Rect.asSvgTimelineTag() =
    """<g>
         <rect x="$x1" y="$y1" width="$width" height="$height" style="fill:rgba($fillColor,$alpha);"></rect>
         <rect x="${x2 - 1}" y="$y1" width="2" height="$height" style="fill:rgba(0,0,0,1);"></rect>
         <text x="${x1 + padding}" y="$y1Text" font-family="Verdana" font-size="$fontSize" fill="#000000" clip-path="url(#clip$clipIndex)">${timeline.name}</text>
         <text x="${x1 + padding}" y="${y1Text+fontSize * 0.8}" font-family="Verdana" font-size="$smallFontSize" fill="#000000" clip-path="url(#clip$clipIndex)">tid=${timeline.tid}</text>
         <clipPath id="clip$clipIndex">
           <rect x="$x1" y="$y1" width="$width" height="$height"/>
         </clipPath>
       </g>""".trimIndent()


private fun Long.between(lower: Long, upper: Long): Boolean = this > lower && this < upper

private infix fun Timeline.collidesWith(other: Timeline): Boolean {
    val start = this.relativeStart
    val end = this.relativeStart + this.duration
    val otherStart = other.relativeStart
    val otherEnd = other.relativeStart + other.duration

    if (start == otherStart || end == otherEnd) return true

    return start.between(otherStart, otherEnd) || end.between(otherStart, otherEnd) || otherStart.between(
        start,
        end
    ) || otherEnd.between(start, end)
}

private fun HashMap<Int, ArrayList<Rect>>.findParentRect(timeline: Timeline): Rect? {
    var parent = timeline.parent ?: return null
    val n = keys.maxBy { it } ?: 0
    for (i in 0..n) {
        val timelines = this[i] ?: return null
        timelines.forEach {
            if (it.timeline == parent) {
                return it
            }
        }
    }

    return null
}

private fun List<Rect>?.isColliding(timeline: Timeline) : Boolean {

    if (this == null)
        return false

    forEach {
        if (it.timeline collidesWith timeline) {
            return true
        }
    }
    return false
}

private fun HashMap<Int, ArrayList<Rect>>.firstAvailableRow(timeline: Timeline): Int {



    val parentRect = findParentRect(timeline)

    var currentRow = parentRect?.rowIndex?.let { it + 1 } ?: 0



    while (true) {

        if (!this[currentRow].isColliding(timeline)) {
            return currentRow
        }

        currentRow++
    }
}