package com.umaiz.calexpenses.util

object Palette {
    val COLORS = intArrayOf(
        0xFF3D8BFD.toInt(), 0xFF22D3EE.toInt(), 0xFF34D399.toInt(), 0xFFFBBF24.toInt(),
        0xFFF87171.toInt(), 0xFFA78BFA.toInt(), 0xFFF472B6.toInt(), 0xFF60A5FA.toInt()
    )
    fun color(i: Int): Int = COLORS[i % COLORS.size]
}
