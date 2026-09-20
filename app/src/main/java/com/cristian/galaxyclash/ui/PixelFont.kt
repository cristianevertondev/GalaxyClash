package com.cristian.galaxyclash.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Tiny 5x7 bitmap pixel font for the retro menu. Letters are drawn as crisp
 * squares on a [DrawScope] so the whole menu stays genuinely pixel-art (no
 * anti-aliased system text on the main actions).
 *
 * Supports A–Z, 0–9, a few punctuation marks, and the accented letters used in
 * the UI (accents are composed as an extra row so the base grid stays 5 wide).
 */
object PixelFont {

    // 5x7 uppercase letters -------------------------------------------------
    private val base: Map<Char, List<String>> = mapOf(
        'A' to g(".###.","#...#","#...#","#####","#...#","#...#","#...#"),
        'B' to g("####.","#...#","#...#","####.","#...#","#...#","####."),
        'C' to g(".###.","#...#","#....","#....","#....","#...#",".###."),
        'D' to g("####.","#...#","#...#","#...#","#...#","#...#","####."),
        'E' to g("#####","#....","#....","####.","#....","#....","#####"),
        'F' to g("#####","#....","#....","####.","#....","#....","#...."),
        'G' to g(".###.","#...#","#....","#.###","#...#","#...#",".###."),
        'H' to g("#...#","#...#","#...#","#####","#...#","#...#","#...#"),
        'I' to g("#####","..#..","..#..","..#..","..#..","..#..","#####"),
        'J' to g("..###","...#.","...#.","...#.","...#.","#..#.",".###."),
        'K' to g("#...#","#..#.","#.#..","##...","#.#..","#..#.","#...#"),
        'L' to g("#....","#....","#....","#....","#....","#....","#####"),
        'M' to g("#...#","##.##","#.#.#","#.#.#","#...#","#...#","#...#"),
        'N' to g("#...#","##..#","#.#.#","#..##","#...#","#...#","#...#"),
        'O' to g(".###.","#...#","#...#","#...#","#...#","#...#",".###."),
        'P' to g("####.","#...#","#...#","####.","#....","#....","#...."),
        'Q' to g(".###.","#...#","#...#","#...#","#.#.#","#..#.",".##.#"),
        'R' to g("####.","#...#","#...#","####.","#.#..","#..#.","#...#"),
        'S' to g(".####","#....","#....",".###.","....#","....#","####."),
        'T' to g("#####","..#..","..#..","..#..","..#..","..#..","..#.."),
        'U' to g("#...#","#...#","#...#","#...#","#...#","#...#",".###."),
        'V' to g("#...#","#...#","#...#","#...#","#...#",".#.#.","..#.."),
        'W' to g("#...#","#...#","#...#","#.#.#","#.#.#","##.##","#...#"),
        'X' to g("#...#","#...#",".#.#.","..#..",".#.#.","#...#","#...#"),
        'Y' to g("#...#","#...#",".#.#.","..#..","..#..","..#..","..#.."),
        'Z' to g("#####","....#","...#.","..#..",".#...","#....","#####"),
        '0' to g(".###.","#..##","#.#.#","##..#","#...#","#...#",".###."),
        '1' to g("..#..",".##..","..#..","..#..","..#..","..#..",".###."),
        '2' to g(".###.","#...#","....#","...#.","..#..",".#...","#####"),
        '3' to g("#####","....#","...#.","..##.","....#","#...#",".###."),
        '4' to g("...#.",".##..",".#.#.","#..#.","#####","...#.","...#."),
        '5' to g("#####","#....","####.","....#","....#","#...#",".###."),
        '6' to g("..##.",".#...","#....","####.","#...#","#...#",".###."),
        '7' to g("#####","....#","...#.","..#..","..#..","..#..","..#.."),
        '8' to g(".###.","#...#","#...#",".###.","#...#","#...#",".###."),
        '9' to g(".###.","#...#","#...#",".####","....#","...#.",".##.."),
        ' ' to g(".....",".....",".....",".....",".....",".....","....."),
        '.' to g(".....",".....",".....",".....",".....","..##.","..##."),
        '!' to g("..#..","..#..","..#..","..#..","..#..",".....","..#.."),
        ':' to g(".....","..#..","..#..",".....","..#..","..#..","....."),
        '-' to g(".....",".....",".....",".###.",".....",".....","....."),
        ',' to g(".....",".....",".....",".....","..#..",".#...","#...."),
        '>' to g("..#..",".#...","#....",".#...","..#..",".....","....."),
    )

    // Accent marks (5-wide rows added above the base letter, which 8 tall).
    private val acute = listOf("...#.")
    private val grave = listOf(".#...")
    private val circum = listOf(".#.#.")
    private val tilde = listOf(".~~~.")
    private val cedilla = listOf("#....", ".#...")

    private fun g(vararg rows: String): List<String> = rows.toList()

        private fun compose(baseGlyph: List<String>, markRows: List<String>): List<String> =
            markRows + baseGlyph

        /** Returns the bitmap rows for a character (uppercased), or a space. */
        private fun glyph(ch: Char): List<String> {
        val c = ch.uppercaseChar()
        return base[c] ?: when (c) {
            // composed accents
            'Á' -> compose(base.getValue('A'), acute)
            'À' -> compose(base.getValue('A'), grave)
            'Â' -> compose(base.getValue('A'), circum)
            'Ã' -> compose(base.getValue('A'), tilde)
            'É' -> compose(base.getValue('E'), acute)
            'Ê' -> compose(base.getValue('E'), circum)
            'Í' -> compose(base.getValue('I'), acute)
            'Ó' -> compose(base.getValue('O'), acute)
            'Ô' -> compose(base.getValue('O'), circum)
            'Õ' -> compose(base.getValue('O'), tilde)
            'Ú' -> compose(base.getValue('U'), acute)
            'Ü' -> compose(base.getValue('U'), listOf(".#.#."))
            'Ç' -> base.getValue('C') + cedilla
            else -> base.getValue(' ')
        }
    }

    const val CELL = 5          // glyph grid width

    /** Height in cells of a single line given the tallest accent used. */
    private fun lineHeight(text: String): Int {
        var h = 7
        for (ch in text) {
            val g = glyph(ch)
            if (g.size > h) h = g.size
        }
        return h
    }

    fun lineHeightPx(scale: Float, text: String = "A"): Float = lineHeight(text) * scale

    fun textWidth(text: String, scale: Float, spacing: Float = 0f): Float {
        var w = 0f
        var first = true
        for (ch in text) {
            val g = glyph(ch)
            val cw = if (g.isEmpty()) 5 else g.maxOf { it.length }
            w += cw * scale
            if (!first) w += spacing
            first = false
        }
        return w
    }

    fun textSize(text: String, scale: Float, spacing: Float = 0f): Size =
        Size(textWidth(text, scale, spacing), lineHeightPx(scale, text))

    /** Draws [text] with its top-left at [origin]. */
    fun DrawScope.drawPixelText(
        text: String,
        origin: Offset,
        scale: Float,
        color: Color,
        spacing: Float = 0f,
    ) {
        var x = origin.x
        for (ch in text) {
            val g = glyph(ch)
            val cw = if (g.isEmpty()) 5 else g.maxOf { it.length }
            for (ry in g.indices) {
                val row = g[ry]
                for (cx in 0 until row.length) {
                    if (row[cx] == '#') {
                        drawRect(
                            color = color,
                            topLeft = Offset(x + cx * scale, origin.y + ry * scale),
                            size = Size(scale, scale),
                        )
                    }
                }
            }
            x += cw * scale + spacing
        }
    }
}