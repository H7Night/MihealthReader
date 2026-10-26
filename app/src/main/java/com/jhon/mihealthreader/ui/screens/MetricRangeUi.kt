package com.jhon.mihealthreader.ui.screens

import java.time.LocalDate

fun canMoveNext(end: LocalDate, today: LocalDate = LocalDate.now()): Boolean = end < today
