package com.gaitguard.app.ui.components

import android.graphics.Color as AColor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun WaveformChart(
    samples: FloatArray,
    stepIndices: List<Int> = emptyList(),
    markIndices: List<Int> = emptyList(),
    stumbleIndices: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    val maxVisible = 200

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setDrawGridBackground(false)
                isAutoScaleMinMaxEnabled = true

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = AColor.parseColor("#737783")
                    textSize = 10f
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = AColor.parseColor("#E6E8EB")
                    textColor = AColor.parseColor("#737783")
                    textSize = 9f
                }
                axisRight.isEnabled = false

                setVisibleXRangeMaximum(maxVisible.toFloat())
            }
        },
        update = { chart ->
            if (samples.isEmpty()) return@AndroidView

            val entries = samples.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val waveSet = LineDataSet(entries, "ACC").apply {
                color = AColor.parseColor("#003178")
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            // Step marker entries
            val stepEntries = stepIndices
                .filter { it < samples.size }
                .map { Entry(it.toFloat(), samples[it]) }
            val stepSet = LineDataSet(stepEntries, "Steps").apply {
                color = AColor.parseColor("#44DDC1")
                setCircleColor(AColor.parseColor("#44DDC1"))
                circleRadius = 4f
                lineWidth = 0f
                setDrawValues(false)
            }

            val markEntries = markIndices
                .filter { it in samples.indices }
                .map { Entry(it.toFloat(), samples[it]) }
            val markSet = LineDataSet(markEntries, "Marks").apply {
                color = AColor.parseColor("#FFB020")
                setCircleColor(AColor.parseColor("#FFB020"))
                circleRadius = 6f
                lineWidth = 0f
                setDrawValues(false)
            }

            val stumbleEntries = stumbleIndices
                .filter { it in samples.indices }
                .map { Entry(it.toFloat(), samples[it]) }
            val stumbleSet = LineDataSet(stumbleEntries, "Stumbles").apply {
                color = AColor.parseColor("#E5484D")
                setCircleColor(AColor.parseColor("#E5484D"))
                circleRadius = 6f
                lineWidth = 0f
                setDrawValues(false)
            }

            chart.data = LineData(waveSet, stepSet, markSet, stumbleSet)
            chart.moveViewToX(entries.size.toFloat())
            chart.invalidate()
        }
    )
}
