package com.oryareach.core.ui.text

private const val FIRST_STRONG_ISOLATE = '⁨'
private const val POP_DIRECTIONAL_ISOLATE = '⁩'

/**
 * Wraps a run of text in Unicode directional isolates so it keeps its own direction when
 * embedded in a paragraph of the opposite direction.
 *
 * Without this, a left-to-right value inside a Hebrew sentence is reordered by the BiDi
 * algorithm: "0.0.0-dev" renders as "dev-0.0.0", and "12-15" as "15-12". It bites version
 * strings, dates, ranges, prices, file names and counts — anything where the characters are
 * neutral or Latin but the surrounding paragraph is RTL.
 *
 * Apply at the point of interpolation, to the value only, never to the whole sentence.
 */
fun String.asLtrIsolate(): String = "$FIRST_STRONG_ISOLATE$this$POP_DIRECTIONAL_ISOLATE"
