package com.example.nhlscoreboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class GameDetail : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_detail)

        val extras = intent.extras
        var game = JSONObject()
        if(extras != null) {
            game = JSONObject(extras.getString("game", "{}"))
        }

        updateUI(game)
    }

    private fun updateUI(game:JSONObject) {
        val awayTeam = game.getJSONObject("teams").getJSONObject("away")
        val homeTeam = game.getJSONObject("teams").getJSONObject("home")

        val awayLogo = findViewById<ImageView>(R.id.awayLogoDetail)
        awayLogo.setImageResource(resources.getIdentifier(awayTeam.getString("abbreviation").lowercase(), "drawable", packageName))

        val homeLogo = findViewById<ImageView>(R.id.homeLogoDetail)
        homeLogo.setImageResource(resources.getIdentifier(homeTeam.getString("abbreviation").lowercase(), "drawable", packageName))

        val awayText = findViewById<TextView>(R.id.awayTeamDetail)
        val awayTextScoring = findViewById<TextView>(R.id.awayTeamNameScoring)
        val awayFullName = awayTeam.getString("locationName") + " " + awayTeam.getString("teamName")
        awayText.text = awayFullName
        awayTextScoring.text = awayFullName

        val homeText = findViewById<TextView>(R.id.homeTeamDetail)
        val homeTextScoring = findViewById<TextView>(R.id.homeTeamNameScoring)
        val homeFullName = homeTeam.getString("locationName") + " " + homeTeam.getString("teamName")
        homeText.text = homeFullName
        homeTextScoring.text = homeFullName

        val scores = game.getJSONObject("scores")
        val awayScore = scores.getInt(awayTeam.getString("abbreviation"))
        val homeScore = scores.getInt(homeTeam.getString("abbreviation"))

        val aScoreText = findViewById<TextView>(R.id.awayTeamScoreDetail)
        aScoreText.text = awayScore.toString()
        val hScoreText = findViewById<TextView>(R.id.homeTeamScoreDetail)
        hScoreText.text = homeScore.toString()

        val statusObject = game.getJSONObject("status")

        val status = statusObject.getString("state")
        val statusText = findViewById<TextView>(R.id.gameStatusDetail)

        when (status) {
            "LIVE" -> {
                val progressObject = statusObject.getJSONObject("progress")
                val period = progressObject.getString("currentPeriodOrdinal")
                val time =
                    progressObject.getJSONObject("currentPeriodTimeRemaining").getString("pretty")
                val description = period + "\t" + time

                statusText.text = description
            }
            "PREVIEW" -> {
                var startTime = game.getString("startTime")
                startTime = startTime.substring(0, startTime.length - 1)
                val locale = Locale.getDefault()
                val checkFormat = SimpleDateFormat("yyyy-MM-dd", locale)
                val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", locale)
                val todayDateFormat = SimpleDateFormat("'Today' h:mm a", locale)
                val futureDateFormat = SimpleDateFormat("MMMM d, yyyy h:mm a", locale)
                inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val datetime = inputDateFormat.parse(startTime)
                val today = Date()
                if(checkFormat.format(today) == checkFormat.format(datetime))
                    statusText.text = todayDateFormat.format(datetime)
                else
                    statusText.text = futureDateFormat.format(datetime)
            }
            "FINAL" -> {
                when {
                    scores.has("overtime") -> statusText.text = resources.getString(R.string.final_overtime_text)
                    scores.has("shootout") -> statusText.text = resources.getString(R.string.final_shootout_text)
                    else -> statusText.text = resources.getString(R.string.final_text)
                }
            }
        }

        updateScoring(game)

    }

    private fun updateScoring(game: JSONObject) {
        val statusObject = game.getJSONObject("status")

        val scores = game.getJSONObject("scores")

        val homeTeamAbbr = game.getJSONObject("teams").getJSONObject("home").getString("abbreviation")
        val awayTeamAbbr = game.getJSONObject("teams").getJSONObject("away").getString("abbreviation")

        val status = statusObject.getString("state")
        var awayScoring = ""
        var homeScoring = ""

        val awayScoreMap = mutableMapOf<String, Int>()
        val homeScoreMap = mutableMapOf<String, Int>()

        var homeKeyVal: String
        var awayKeyVal: String

        val scoringLegend = findViewById<TextView>(R.id.scoringLegend)

        if(status == "PREVIEW") {
            for(i in 1..3) {
                homeKeyVal = "0\t\t\t"
                homeScoring += homeKeyVal

                awayKeyVal = "0\t\t\t"
                awayScoring += awayKeyVal
            }
        } else {
            var goals = JSONArray()
            if(game.has("goals")){
                goals = game.getJSONArray("goals")
            }

            for (i in 0 until goals.length()) {
                val goal = goals.getJSONObject(i)
                val goalTeam = goal.getString("team")
                val period = goal.getString("period")
                if (goalTeam == homeTeamAbbr) {
                    if (homeScoreMap.containsKey(period)) {
                        homeScoreMap.merge(period, 1, Int::plus)
                    } else {
                        homeScoreMap[period] = 1
                    }
                } else {
                    if (awayScoreMap.containsKey(period)) {
                        awayScoreMap.merge(period, 1, Int::plus)
                    } else {
                        awayScoreMap[period] = 1
                    }
                }
            }

            if (status == "LIVE") {
                val currentPeriod = statusObject.getJSONObject("progress").getInt("currentPeriod")
                for (i in 1..currentPeriod) {
                    val period = i.toString()

                    homeKeyVal = if (homeScoreMap.containsKey(period))
                        homeScoreMap[period].toString() + "\t\t\t"
                    else
                        "0\t\t\t"
                    homeScoring += homeKeyVal

                    awayKeyVal = if (awayScoreMap.containsKey(period))
                        awayScoreMap[period].toString() + "\t\t\t"
                    else
                        "0\t\t\t"
                    awayScoring += awayKeyVal
                }
                for (i in currentPeriod + 1..3) {
                    homeScoring += "0\t\t\t"
                    awayScoring += "0\t\t\t"
                }
            } else {
                var numberPeriods = 3
                if (scores.has("overtime") || scores.has("shootout"))
                    numberPeriods = 4
                for (i in 1..numberPeriods) {
                    var period = i.toString()
                    if (i == 4) {
                        if (scores.has("shootout"))
                            period = "SO"
                        else if (scores.has("overtime"))
                            period = "OT"
                    }

                    homeKeyVal = if (homeScoreMap.containsKey(period))
                        homeScoreMap[period].toString()
                    else
                        "0"
                    homeScoring += homeKeyVal + "\t\t\t"

                    awayKeyVal = if (awayScoreMap.containsKey(period))
                        awayScoreMap[period].toString()
                    else
                        "0"
                    awayScoring += awayKeyVal + "\t\t\t"
                }
            }
        }

        if(scores.has("overtime") || scores.has("shootout")) {
            homeScoring += "\t "
            awayScoring += "\t "
        }

        homeScoring += scores.getInt(homeTeamAbbr)
        awayScoring += scores.getInt(awayTeamAbbr)

        val homeScoreText = findViewById<TextView>(R.id.homeScoringText)
        val awayScoreText = findViewById<TextView>(R.id.awayScoringText)

        homeScoreText.text = homeScoring
        awayScoreText.text = awayScoring

        when {
            scores.has("overtime") -> scoringLegend.text = resources.getString(R.string.scoring_summary_overtime)
            scores.has("shootout") -> scoringLegend.text = resources.getString(R.string.scoring_summary_shootout)
            else -> scoringLegend.text = resources.getString(R.string.scoring_summary)
        }

    }
}