package com.michaeo04.spendlikeamillionaire.domain

import java.text.Normalizer

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/** Lowercase and strip accents so "ca phe" finds "Cà phê"; also maps đ -> d. */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace('đ', 'd')
