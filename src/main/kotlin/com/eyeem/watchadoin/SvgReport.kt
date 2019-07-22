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
    private val xAxisHeight = 8
    private val scaleGridDistance = 50

    private val maxPageWidth = 1200
    private var xScale = 1.0f

    /**
     * @return SVG formatted string with the report in it
     */
    override fun toString() : String {
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

            val rectWidth = (timeline.duration * xScale).toLong()
            val rectHeight = timelineHeight

            output += """<g>
                           <rect x="$x1" y="$y1" width="$rectWidth" height="$rectHeight" style="fill:rgba($sqColor,$alpha);"></rect>
                           <text x="${x1 + padding}" y="$y1Text" font-family="Verdana" font-size="$fontSize" fill="#000000" clip-path="url(#clip$index)">${timeline.name}</text>
                           <clipPath id="clip$index">
                             <rect x="$x1" y="$y1" width="$rectWidth" height="$rectHeight"/>
                           </clipPath>
                         </g>""".trimIndent()
        }

        output += "</svg>"
        return output
    }

}

fun Stopwatch.asSvgReport() : SvgReport = SvgReport(timelines())

fun Stopwatch.saveAsSvg(file: File) {
    val svgOutput = asSvgReport().toString()
    file.printWriter().use { out -> out.println(svgOutput) }
}