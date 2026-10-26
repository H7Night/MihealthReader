package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.parse.Workout

fun workoutTypeOptions(workouts: List<Workout>): List<String> =
    workouts.groupingBy { it.typeName }.eachCount()
        .entries.sortedByDescending { it.value }
        .map { it.key }

fun filterWorkouts(workouts: List<Workout>, type: String?): List<Workout> =
    if (type == null) workouts else workouts.filter { it.typeName == type }
