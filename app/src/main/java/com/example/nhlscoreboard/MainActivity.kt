package com.example.nhlscoreboard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var gameIDs = mutableMapOf<String, Int>()
    private var rowIDs = mutableMapOf<String, Int>()
    private lateinit var queue: RequestQueue
    private lateinit var date: Date
    private lateinit var previous: View
    private lateinit var next: View
    private lateinit var contextButton: Button


    class PreviousDayClick() : View.OnClickListener {
        private var date = Date()
        var bar: (d: Date, b: Boolean) -> Unit = fun (_: Date, _: Boolean) {}

        constructor(date: Date, bar: (d: Date, b: Boolean) -> Unit) : this() {
            this.date = Date(date.time - (1000 * 60 * 60 * 24))
            this.bar = bar
        }

        override fun onClick(v: View) {
            bar(date, true)
        }
    }

    class NextDayClick() : View.OnClickListener {
        private var date = Date()
        var bar: (d: Date, b: Boolean) -> Unit = fun (_: Date, _: Boolean) {}

        constructor(date: Date, bar: (d: Date, b: Boolean) -> Unit) : this() {
            this.date = Date(date.time + (1000 * 60 * 60 * 24))
            this.bar = bar
        }

        override fun onClick(v: View) {
            bar(date, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        queue = Volley.newRequestQueue(this)
        date = Date()
        previous = findViewById(R.id.previousDay)
        next = findViewById(R.id.nextDay)
        contextButton = findViewById(R.id.contextButton)

        getGamesForDate(date, true)

        contextButton.setOnClickListener {
            getGamesForDate(date, false)
        }
    }

    private fun deleteGameViews(l: LinearLayout) {
        l.removeAllViews()
    }

    private fun getGamesForDate(start: Date, create: Boolean) {
        val locale = Locale.getDefault()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", locale)

        val startDate = dateFormat.format(start)
        val endDate = dateFormat.format(start)

        previous.setOnClickListener(PreviousDayClick(start, ::getGamesForDate))
        next.setOnClickListener(NextDayClick(start, ::getGamesForDate))

        val currentDateFormat = SimpleDateFormat("MMMM d yyyy", locale)
        val currentDateText = currentDateFormat.format(start)
//        supportActionBar?.title = currentDateText

        val currentDate = findViewById<TextView>(R.id.CurrentDate)
        currentDate.text = currentDateText

        if(dateFormat.format(start) != dateFormat.format(Date())){
            contextButton.text = resources.getString(R.string.today)
            contextButton.textSize = 16F
            contextButton.setOnClickListener {
                getGamesForDate(Date(), true)
            }
        } else {
            contextButton.text = resources.getString(R.string.refresh)
            contextButton.textSize = 24F
            contextButton.setOnClickListener {
                getGamesForDate(start, false)
            }
        }

        val url = "https://nhl-score-api.herokuapp.com/api/scores?startDate=$startDate&endDate=$endDate"
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                if(response.length() == 0) {
                    val layout = findViewById<LinearLayout>(R.id.gameContainer)
                    val noGames = TextView(this)
                    noGames.text = resources.getString(R.string.no_games)
                    noGames.textSize = 24f
                    noGames.gravity = Gravity.CENTER
                    noGames.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
                    noGames.setPadding(0, 64, 0, 0)

                    val param = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.0f
                    )
                    noGames.layoutParams = param

                    layout.removeAllViews()
                    layout.addView(noGames)
                }
                else {
                    updateUI(response.getJSONObject(0), create)
                }
            },
            { error ->
                println("There was a request error: " + error.message)
            })
        queue.add(jsonArrayRequest)
    }

    private fun getGameTag(games: JSONArray, index: Int): String {
        val game = games.getJSONObject(index)
        val homeTeamAbbr =
            game.getJSONObject("teams").getJSONObject("home").getString("abbreviation")
        val awayTeamAbbr =
            game.getJSONObject("teams").getJSONObject("away").getString("abbreviation")

        return homeTeamAbbr + awayTeamAbbr
    }

    private fun updateUI(response: JSONObject, create: Boolean) {
        val layout = findViewById<LinearLayout>(R.id.gameContainer)
        if(create)
            deleteGameViews(layout)

        val currentDate = Date()
        val locale = Locale.getDefault()
        val format = SimpleDateFormat("yyyy-MM-dd", locale)
        val dateF = format.format(date)
        val currentDateF = format.format(currentDate)
        if(dateF != currentDateF)
            contextButton.visibility = View.INVISIBLE

        val games = response.getJSONArray("games")

        val firstGameTag = getGameTag(games, 0)

        var horizontalLayout: LinearLayout
        if (create) {
            horizontalLayout = createHorizontalLayout(layout, 0, 500)
            horizontalLayout.id = 0
            rowIDs[firstGameTag] = horizontalLayout.id
        }
        else
            horizontalLayout = findViewById(rowIDs.getOrDefault(firstGameTag, 0))

        for (i in 0 until games.length()) {
            val game = games.getJSONObject(i)
            val awayTeam = game.getJSONObject("teams").getJSONObject("away")
            val homeTeam = game.getJSONObject("teams").getJSONObject("home")

            val viewTag = getGameTag(games, i)

            if (i > 0 && i % 2 == 0) {

                if (create) {
                    createHorizontalLayout(layout, i - 1, 25)
                    horizontalLayout = createHorizontalLayout(layout, i, 500)
                    horizontalLayout.id = i
                    rowIDs[viewTag] = horizontalLayout.id
                } else {
                    horizontalLayout = findViewById(rowIDs.getOrDefault(viewTag, 0))
                }
            }

            var view: View
            if (create) {
                view = layoutInflater.inflate(R.layout.game_view, null)
                view.id = i + 100
                gameIDs[viewTag] = view.id
            } else {
                view = findViewById(gameIDs.getOrDefault(viewTag, 0))
            }

            view.setOnClickListener {
                val intent = Intent(this, GameDetail::class.java).apply {
                    putExtra("game", game.toString())
                }
                startActivity(intent)
            }

            val awayLogo = view.findViewById<ImageView>(R.id.awayTeamLogo)
            awayLogo.setImageResource(
                resources.getIdentifier(
                    awayTeam.getString("abbreviation").lowercase(), "drawable", packageName
                )
            )

            val homeLogo = view.findViewById<ImageView>(R.id.homeTeamLogo)
            homeLogo.setImageResource(
                resources.getIdentifier(
                    homeTeam.getString("abbreviation").lowercase(), "drawable", packageName
                )
            )

            val awayText = view.findViewById<TextView>(R.id.awayTeam)
            awayText.text = awayTeam.getString("locationName")
            val homeText = view.findViewById<TextView>(R.id.homeTeam)
            homeText.text = homeTeam.getString("locationName")

            val scores = game.getJSONObject("scores")
            val awayScore = scores.getInt(awayTeam.getString("abbreviation"))
            val homeScore = scores.getInt(homeTeam.getString("abbreviation"))

            val aScoreText = view.findViewById<TextView>(R.id.awayTeamScore)
            aScoreText.text = awayScore.toString()
            val hScoreText = view.findViewById<TextView>(R.id.homeTeamScore)
            hScoreText.text = homeScore.toString()

            val statusObject = game.getJSONObject("status")

            val status = statusObject.getString("state")
            val statusText = view.findViewById<TextView>(R.id.gameStatus)

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
                    val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", locale)
                    val outputDateFormat = SimpleDateFormat("h:mm a", locale)
                    inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val datetime = inputDateFormat.parse(startTime)
                    statusText.text = outputDateFormat.format(datetime)
                }
                "FINAL" -> {
                    when {
                        scores.has("overtime") -> statusText.text = resources.getString(R.string.final_overtime_text)
                        scores.has("shootout") -> statusText.text = resources.getString(R.string.final_shootout_text)
                        else -> statusText.text = resources.getString(R.string.final_text)
                    }
                }
            }

            view.minimumWidth = (layout.width + layout.paddingLeft + layout.paddingRight) / 2
            var param = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT / 2,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
            )
            view.layoutParams = param

            if(create) {
                if (i % 2 == 0) {
                    horizontalLayout.addView(view, 0)
                    val empty = View(this)
                    param = LinearLayout.LayoutParams(
                        1,
                        1,
                        .025f
                    )
                    empty.layoutParams = param
                    horizontalLayout.addView(empty, 1)
                } else {
                    horizontalLayout.addView(view, 2)
                }

                if (i == games.length() - 1 && games.length() % 2 != 0) {
                    val empty = View(this)
                    param = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT / 2,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.0f
                    )
                    empty.layoutParams = param
                    horizontalLayout.addView(empty, 1)
                }
            }
        }
    }

    private fun createHorizontalLayout(layout: LinearLayout, i: Int, h: Int): LinearLayout {
        val horizontalLayout = LinearLayout(this)
        val param = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            h,
        )
        horizontalLayout.layoutParams = param

        layout.addView(horizontalLayout, i)

        return horizontalLayout
    }
}