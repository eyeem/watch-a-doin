package com.eyeem.watchadoin

import java.io.File
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Convert Timelines report to a simple SVG
 *
 * @param timelines output of [Stopwatch.report]
 */
class SvgReport(val timelines: List<Timeline>, htmlEmbed: Boolean = false) {

    val padding = 10
    private val timelineHeight = 30
    private val fontSize = 12
    private val smallFontSize = 8
    val xAxisHeight = 8
    val timelineAxisHeight = 12
    val scaleGridDistance = 50
    val relations = timelines.relations()

    private val maxPageWidth = 1200
    private val renderedSvg : String

    private val svgWidth : Long
    val svgHeight : Long
    var xScale : Float
        private set
    val totalDurationMs : Long

    val svgWidthNormalized
        get() = svgWidth * xScale

    val title: String
        get() = timelines.firstOrNull()?.name ?: "Empty Report"

    init {
        xScale = 1.0f
        totalDurationMs = timelines.map { it.duration + it.relativeStart }.maxBy { it }
            ?: throw IllegalStateException("no maximum found")
        val scaleSteps = totalDurationMs / scaleGridDistance
        svgWidth = totalDurationMs + padding * 2
        if (svgWidth > (maxPageWidth - padding * 2)) {
            xScale = maxPageWidth.toFloat() / totalDurationMs.toFloat()
        }

        var timelinesSvg = "" // timelines as svg tags

        // collects timelines as we draw them, int here is a raw in which we draw timeline
        val rects = HashMap<Int, ArrayList<Rect>>()
        timelines.forEachIndexed { index, timeline ->

            val heightIndex = rects.firstAvailableRow(timeline)

            val _y1 = (heightIndex + 1) * padding + heightIndex * timelineHeight
            val _y1Text = (_y1 + (timelineHeight - fontSize)).toLong()
            val _x1 = padding + (timeline.relativeStart * xScale).toLong()
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

            timelinesSvg += rect.asSvgTimelineTag(htmlEmbed, index)
        }

        val rowCount = rects.values.size
        svgHeight = ((rowCount + 1) * padding + rowCount * timelineHeight + xAxisHeight + padding).toLong()

        var output = """<svg id="stopwatch" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $svgWidthNormalized $svgHeight" width="$svgWidthNormalized" height="$svgHeight">"""

        // draw the timeline grid and axis
        if (htmlEmbed) {
            output += """<g id="timeaxis"></g>"""
        } else {
            val omit = (1f / xScale).roundToLong()

            for (scaleStep in 0..scaleSteps) {

                if (scaleStep % omit != 0L) {
                    continue
                }

                val x = (scaleGridDistance * scaleStep * xScale).toLong() + padding
                val y1 = padding
                val yLength = (rowCount - 1) * padding + rowCount * timelineHeight


                output += """<g>
                            <line stroke-dasharray="5, 5" x1="$x" y1="$y1" x2="$x" y2="${y1 + yLength}" style="stroke-width:1;stroke:rgba(0,0,0,0.5)"/>
                            <text x="$x" y="${y1 + yLength + padding}" font-family="Verdana" font-size="$xAxisHeight" fill="#00000077">${scaleStep * scaleGridDistance}ms</text>
                         </g>""".trimIndent()
            }
        }

        output += timelinesSvg

        output += "</svg>"
        renderedSvg = output
    }


    /**
     * @return SVG formatted string with the report in it
     */
    override fun toString(): String = renderedSvg
}

fun Stopwatch.asSvgReport(timeaxisPlaceholder: Boolean = false): SvgReport = SvgReport(timelines(includeParent = true), timeaxisPlaceholder)

fun Stopwatch.saveAsSvg(file: File, dryRun: Boolean = false) {
    val svgOutput = asSvgReport().toString()
    if (dryRun) {
        println(svgOutput)
    } else {
        file.printWriter().use { out -> out.println(svgOutput) }
    }
}

fun Stopwatch.saveAsHtml(file: File, dryRun: Boolean = false) {
    val svgReport = asSvgReport(timeaxisPlaceholder = true)
    val htmlOutput = htmlTemplate(report = svgReport)
    if (dryRun) {
        println(svgReport)
    } else {
        file.printWriter().use { out -> out.println(htmlOutput) }
    }
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

private fun Rect.asSvgTimelineTag(htmlEmbed: Boolean, timelineIndex: Int) : String {

    fun listeners(): String {
        if (!htmlEmbed) return ""

        return """ onclick="onTimeBoxClick('$timelineIndex')" onmouseover="onTimeBoxHover('$timelineIndex')" onmouseout="onDefaultHint()""""
    }

    return """<g id="timebox_$timelineIndex">
         <rect x="$x1" y="$y1" width="$width" height="$height" style="fill:rgba($fillColor,$alpha);"${listeners()}></rect>
         <rect x="${x2 - 1}" y="$y1" width="2" height="$height" style="fill:rgba(0,0,0,1);"${listeners()}></rect>
         <text x="${x1 + padding}" y="$y1Text" font-family="Verdana" font-size="$fontSize" fill="#000000" clip-path="url(#clip$clipIndex)"${listeners()}>${timeline.name.escapeXml()}</text>
         <text x="${x1 + padding}" y="${y1Text + fontSize * 0.8}" font-family="Verdana" font-size="$smallFontSize" fill="#000000" clip-path="url(#clip$clipIndex)"${listeners()}>tid=${timeline.tid}</text>
         <clipPath id="clip$clipIndex">
           <rect x="$x1" y="$y1" width="$width" height="$height" class="clipRect"/>
         </clipPath>
       </g>""".trimIndent()
}

private fun List<Timeline>.relations(list : ArrayList<Timeline> = ArrayList()) : List<HashSet<Int>> {
    val ancestorsArray = map { it.ancestors() }
    val relations = ArrayList<HashSet<Timeline>>()
    forEachIndexed { index, timeline ->
        relations += HashSet<Timeline>()
        ancestorsArray.forEach { ancestors ->
            if (ancestors.contains(timeline)) {
                relations[index].addAll(ancestors)
            }
        }
    }
    return relations.map { relationSet -> HashSet(relationSet.map { timeline -> indexOf(timeline) }) }
}

private fun Timeline.ancestors(list : ArrayList<Timeline> = ArrayList()) : List<Timeline> {
    list.add(0, this)
    parent?.ancestors(list)
    return list
}

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

internal val xmlEscapeMap = mapOf(
    '"' to "&quot;",
    '\'' to "&apos;",
    '<' to "&lt;",
    '>' to "&gt;",
    '&' to "&amp;"
)
internal fun String.escapeXml() : String {
    val sb = StringBuilder()

    forEach { char ->
        sb.append(xmlEscapeMap[char] ?: char)
    }

    return sb.toString()
}

private fun htmlTemplate(report: SvgReport) = """
<!DOCTYPE html>
<html><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

  <title>${report.title.escapeXml()}</title>

  <style i type="text/css">
    svg * {
      -webkit-user-select: none; /* Safari 3.1+ */
        -moz-user-select: none; /* Firefox 2+ */
        -ms-user-select: none; /* IE 10+ */
        user-select: none; /* Standard syntax */
    }

    svg text {
      fill: #000;
      font-family: "Verdana";
    }

    div.sticky {
      font-family: "Verdana";
      position: -webkit-sticky;
      position: sticky;
      top: 0;
      background-color: #fff;
    }

  </style>

  <script src="https://code.easypz.io/easypz.latest.min.js"></script>

</head>
<body>

  <div class="sticky">
    <div id="navHint" style="font-size: 10px; color: #777; padding-left: 10px; height: 15px;">Zoom In [Hold Left Click] | Zoom Out [Double Left Click]</div>
    <div>
    <svg id="navSvg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${report.svgWidthNormalized} ${report.timelineAxisHeight}" width="${report.svgWidthNormalized}" height="${report.timelineAxisHeight}">
      <g id="timeaxis"></g>
    </svg>
    </div>
  </div>

$report

<script src="https://cdnjs.cloudflare.com/ajax/libs/svg.js/2.7.1/svg.min.js"></script>
<script type="text/javascript">
    var defaultHint = "Zoom In [Hold Left Click] | Zoom Out [Double Left Click]"
    var svg = document.getElementById('stopwatch')
    var stopwatch = SVG.get('stopwatch')
    var timeaxis = SVG.adopt(svg.getElementById('timeaxis'))

    var navSvgElement = document.getElementById('navSvg')
    var navSvg = SVG.get('navSvg')
    var navTimeaxis = SVG.adopt(navSvgElement.getElementById('timeaxis'))

    var groups = Array.from(svg.getElementsByTagName('g'))
    var width = ${report.svgWidthNormalized}
    var height = ${report.svgHeight}
    var padding = ${report.padding}
    var xScale = ${report.xScale}
    var totalDurationMs = ${report.totalDurationMs}
    var xAxisHeight = ${report.xAxisHeight}
    var timelineAxisHeight = ${report.timelineAxisHeight}

    function d(name, tid, time, relations) {
    	var d = {}
    	d["name"] = name
    	d["tid"] = tid
    	d["time"] = time
        d["relations"] = relations
    	return d
    }

    var data = [
      ${report.timelines.mapIndexed { index, timeline ->
    """d("${timeline.name.escapeXml()}", ${timeline.tid}, ${timeline.duration}, ${report.relations[index].joinToString(separator = ",", prefix = "[", postfix = "]")})"""
        }.joinToString(separator = ",\n")
      }
    ]

    var selectedIndex = -1
    function onTimeBoxClick(index) {
    	if (new Date().getTime() - lastTransformationAt < 300) {
        return // debounce
      }

      if (selectedIndex === index) {
        selectedIndex = -1
      } else {
        selectedIndex = index
      }
      var relations = data[index].relations

      var index
      for (i = 0; i < data.length; i++) {
        var timebox = document.getElementById('timebox_'+i)
        if (selectedIndex === -1 || relations.indexOf(i) > -1) {
          timebox.style.display = "block"
        } else {
          timebox.style.display = "none"
        }
      }
    }

    function onTimeBoxHover(index) {
    	var d = data[index]
    	navHint.innerText = d.name + " | " + d.time + "ms | tid = " + d.tid
    }

    function onDefaultHint() {
    	navHint.innerText = defaultHint
    }

    var lastScale
    function drawTimeAxis(scale) {
      if (lastScale === scale) {
      	return
      }
      lastScale = scale
      timeaxis.clear()
      navTimeaxis.clear()
      var scaleGridDistance = 50
      var omitNotRounded = 1.0 / xScale / scale
      var omit = Math.round(omitNotRounded)

      // when omit is under 0.5 we must set it to 1 but decrease scale grid distance
      if (omitNotRounded < 0.5) {
      	omit = 1.0
      	if (omitNotRounded < 0.25) {
      		scaleGridDistance = 10
      	} else {
      		scaleGridDistance = 25
      	}
      }

      var scaleSteps = totalDurationMs / scaleGridDistance

      var scaleStep;
      for (scaleStep = 0; scaleStep < scaleSteps; scaleStep++) { 
        if (scaleStep % omit != 0) {
        	continue
        }

        var x = ((scaleGridDistance * scaleStep * xScale) + padding) * scale
        var y1 = padding
        var y2 = height - padding - xAxisHeight
        timeaxis.line(x, y1, x, y2).stroke({ width: 1, color: '#0000007f', dasharray: '5, 5'})

        var timeMs = scaleStep * scaleGridDistance
        timeaxis.text(timeMs + "ms").attr({"font-family": "Verdana", "font-size": xAxisHeight, "x": x, "y": y2 - padding})
        navTimeaxis.text(timeMs + "ms").attr({"font-family": "Verdana", "font-size": xAxisHeight, "x": x, "y": 0})
      }
    }

    drawTimeAxis(1.0)
    onDefaultHint()

    var maxScale = Math.max(1.0, Math.round((totalDurationMs/width)*12))

    var lastTransformationAt = new Date().getTime()
    var lastX

    new EasyPZ(svg, function(transform) {

      if (lastScale !== transform.scale || lastX !== transform.translateX) {
      	 lastTransformationAt = new Date().getTime()
      }
      lastX = transform.translateX

      drawTimeAxis(transform.scale)

      // Use transform.scale, transform.translateX, transform.translateY to update your visualization
      stopwatch.viewbox(-transform.translateX, 0, width, height)
      navSvg.viewbox(-transform.translateX, 0, width, timelineAxisHeight)
      groups.forEach(function(group) {
      	if(group.getAttribute('id') === "timeaxis") {
      		return
      	}
        group.setAttribute("transform", "scale(" + transform.scale + " 1)");

        var rects = Array.from(group.getElementsByTagName('rect'))

        var firstRect = rects[0]
        if (firstRect === undefined) {
          return;
        }

        var x = firstRect.getAttribute('x') * 1
        var blockWidth = firstRect.getAttribute('width') * 1

        var markingRect = rects[1]
        if (markingRect === undefined) {
          return;
        }

        markingRect.setAttribute("transform", "scale(" + 1/transform.scale +" 1)");
        markingRect.setAttribute("x", (x + blockWidth) * transform.scale - 1);

        var clipRect = Array.from(group.getElementsByClassName('clipRect'))[0]
        clipRect.setAttribute("transform", "scale(" + transform.scale +" 1)");

        var texts = Array.from(group.getElementsByTagName('text'))
        texts.forEach(function(text) {
            text.setAttribute("transform", "scale(" + 1/transform.scale +" 1)");
            text.setAttribute("x", x * transform.scale + 10);
        });
      })
    },
    { minScale: 0.5, maxScale: maxScale, bounds: { top: 0, right: 0, bottom: 0, left: 0 } }, ["FLICK_PAN", "HOLD_ZOOM_IN", "DBLCLICK_ZOOM_OUT"]);

</script>

</body></html>
""".trimIndent()