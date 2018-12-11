package com.worthlesscog.tv.data

import java.io.File

import com.worthlesscog.tv.Maybe

trait MediaPlayer {

    def playerId: String

    def playerName: String

    def generate(series: TvSeries, target: File): Maybe[String]

    def merge(series: TvSeries, series2: TvSeries, target: File): Maybe[String]

    def resize(target: File): Maybe[String]

}
