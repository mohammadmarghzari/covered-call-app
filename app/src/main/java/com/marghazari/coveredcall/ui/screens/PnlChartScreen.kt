package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.domain.CoveredCallCalculator

@Composable
fun PnlChartScreen(contract: OptionContract) {
    val points = remember(contract) { CoveredCallCalculator.profitLossCurve(contract) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "نمودار سود و زیان کاورد کال — ${contract.symbol}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "سهم پایه: ${contract.underlyingSymbol} | قیمت اعمال: ${formatRial(contract.strikePrice)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (points.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("داده‌ای برای رسم نمودار موجود نیست")
            }
            return@Column
        }

        val minPnl = points.minOf { it.second }
        val maxPnl = points.maxOf { it.second }
        val minPrice = points.minOf { it.first }
        val maxPrice = points.maxOf { it.first }

        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val w = size.width
                val h = size.height

                fun xFor(price: Long): Float {
                    if (maxPrice == minPrice) return w / 2f
                    return ((price - minPrice).toFloat() / (maxPrice - minPrice).toFloat()) * w
                }
                fun yFor(pnl: Long): Float {
                    if (maxPnl == minPnl) return h / 2f
                    return h - ((pnl - minPnl).toFloat() / (maxPnl - minPnl).toFloat()) * h
                }

                val zeroY = yFor(0L)
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 2f
                )

                for (i in 0 until points.size - 1) {
                    val (p1, v1) = points[i]
                    val (p2, v2) = points[i + 1]
                    val color = if (v1 >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    drawLine(
                        color = color,
                        start = Offset(xFor(p1), yFor(v1)),
                        end = Offset(xFor(p2), yFor(v2)),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                val curX = xFor(contract.underlyingPrice)
                drawLine(
                    color = Color.Blue,
                    start = Offset(curX, 0f),
                    end = Offset(curX, h),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendDot(color = Color(0xFF2E7D32), label = "سود")
            LegendDot(color = Color(0xFFC62828), label = "زیان")
            LegendDot(color = Color.Blue, label = "قیمت فعلی سهم")
        }

        val result = CoveredCallCalculator.calculate(contract)
        if (result != null) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("نقطه سر به سر", formatRial(result.breakEvenPrice))
                    InfoRow("حداکثر سود در سررسید", formatRial(result.maxProfitIfExercised))
                    InfoRow("بازده ایستا", "%.2f%%".format(result.staticReturnPercent))
                    InfoRow("بازده موثر سالانه", "%.2f%%".format(result.annualizedReturnPercent))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = color)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
