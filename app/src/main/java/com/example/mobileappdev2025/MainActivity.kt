package com.example.mobileappdev2025

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileInputStream
import java.util.Random
import java.util.Scanner

data class WordDefinition(val word: String, val definition: String, var correctStreak: Int = 0);

class MainActivity : AppCompatActivity() {
    private val ADD_WORD_CODE = 1234; // 1-65535
    private lateinit var myAdapter : ArrayAdapter<String>; // connect from data to gui
    private var dataDefList = ArrayList<String>(); // data
    private var wordDefinition = mutableListOf<WordDefinition>();
    private var score : Int = 1;
    private var streak : Int = 2;
    private var totalCorrect : Int = 3;
    private var totalWrong : Int = 4;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadStatsFromDisk()
        loadWordsFromDisk()

        pickNewWordAndLoadDataList();
        setupList();

        val defList = findViewById<ListView>(R.id.dynamic_def_list);
        defList.setOnItemClickListener { _, _, index, _ ->
            pickNewWordAndLoadDataList();
            myAdapter.notifyDataSetChanged();
        };
    }
    override fun onDestroy(){
        super.onDestroy()

        saveStatsToFile()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_WORD_CODE && resultCode == RESULT_OK && data != null){
            val word = data.getStringExtra("word")?:""
            val def = data.getStringExtra("def")?:""

            Log.d("MAD", word)
            Log.d("MAD", def)

            if ( word == "" || def == "")
                return

            saveWordToFile(word, def, correctStreak = 0)

            wordDefinition.add(WordDefinition(word, def))

            pickNewWordAndLoadDataList()
            myAdapter.notifyDataSetChanged()
        }
    }
    private fun saveWordToFile(word: String, def: String, correctStreak: Int){
        val file = File(applicationContext.filesDir, "user_data.csv")
        file.appendText("$word|$def|$correctStreak\n")


    }
    private fun loadWordsFromDisk()
    {
        // user data
        val file = File(applicationContext.filesDir, "user_data.csv")

        if (file.exists()) {
            val readResult = FileInputStream(file)
            val scanner = Scanner(readResult)

            while(scanner.hasNextLine()){
                val line = scanner.nextLine()
                val wd = line.split("|")
                wordDefinition.add(WordDefinition(wd[0], wd[1], wd[2].toIntOrNull() ?:0))
            }
        } else { // default data

            file.createNewFile()

            val reader = Scanner(resources.openRawResource(R.raw.default_words))
            while(reader.hasNextLine()){
                val line = reader.nextLine()
                val wd = line.split("|")
                wordDefinition.add(WordDefinition(wd[0], wd[1]))
                file.appendText("${wd[0]}|${wd[1]}|${wd[2]}\n")
            }
        }
    }
    private fun saveStatsToFile(){
        val file = File(applicationContext.filesDir, "user_stats.csv")

        file.writeText("score=$score\n")
        file.writeText("streak=$streak\n")
        file.writeText("totalCorrect=$totalCorrect\n")
        file.writeText("totalWrong=$totalWrong\n")

    }
    private fun loadStatsFromDisk() {
        val file = File(applicationContext.filesDir, "user_stats.csv")

        if (file.exists()) {
            val scanner = Scanner(file)

            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val parts = line.split("=")

                if (parts.size == 2) {
                    when (parts[0]) {
                        "score" -> score = parts[1].toIntOrNull() ?: 0
                        "streak" -> streak = parts[1].toIntOrNull() ?: 0
                        "totalCorrect" -> totalCorrect = parts[1].toIntOrNull() ?: 0
                        "totalWrong" -> totalWrong = parts[1].toIntOrNull() ?: 0
                    }

                }
            }
        }
    }

    private fun pickNewWordAndLoadDataList()
    {
        wordDefinition.sortBy{ it.correctStreak}
        wordDefinition.shuffle();

        dataDefList.clear();

        for(wd in wordDefinition){
            dataDefList.add(wd.definition);
        }
        for (i in 0 until minOf(4, wordDefinition.size)){
            dataDefList.add(wordDefinition[i].definition)
        }

        findViewById<TextView>(R.id.word).text = wordDefinition[0].word;

        dataDefList.shuffle();
    }

    private fun setupList()
    {
        myAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataDefList);

        // connect to list
        val defList = findViewById<ListView>(R.id.dynamic_def_list);
        defList.adapter = myAdapter;
    }

    fun openStats(view : View)
    {
        var myIntent = Intent(this, StatsActivity::class.java);
        myIntent.putExtra("score", score.toString());
        myIntent.putExtra("streak", streak.toString());
        myIntent.putExtra("totalCorrect", totalCorrect.toString());
        myIntent.putExtra("totalWrong", totalWrong.toString());
        startActivity(myIntent)
    }

    fun openAddWord(view : View)
    {
        var myIntent = Intent(this, AddWordActivity::class.java);
        startActivityForResult(myIntent, ADD_WORD_CODE)
    }

    fun streak(view : View)
    {
        val correctAnswer = wordDefinition[0].definition

        val clickedDefinition = dataDefList[view.tag as Int]

        val word = wordDefinition[0]

        if(clickedDefinition == correctAnswer) {
            word.correctStreak++
            streak++
            totalCorrect++
            score += streak
        } else {
            word.correctStreak = 0
            streak = 0
            totalWrong++

        }
        saveStatsToFile()
        updateStats()

        pickNewWordAndLoadDataList()
        myAdapter.notifyDataSetChanged()
    }
    private fun saveAllWordsToFile(){
        val file = File(applicationContext.filesDir, "user_data.csv")
        file.writeText("")
        for (wd in wordDefinition){
            saveWordToFile(wd.word, wd.definition, wd.correctStreak)
        }
    }
    private fun updateStats(){
    val scoreText = findViewById<TextView>(R.id.score_text)
    val streakText = findViewById<TextView>(R.id.streak_text)

    scoreText.text = "score: $score"
    streakText.text = "streak: $streak"
   }
}