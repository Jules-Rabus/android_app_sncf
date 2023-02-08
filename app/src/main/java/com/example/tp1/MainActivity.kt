package com.example.tp1

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val stationList : ArrayList<Station> = ArrayList()  // On stock dans un arrayList les stations
    private var station: Station? = null                        // On stock la station actuellement sélectionné

    // Fonction pour remplir l'arrayList en utilisant le fichier gares.csv
    private fun initListStations() {

        // On récupere le fichier gares.csv et on itère sur chaque ligne
        val inputStream = resources.openRawResource(R.raw.gares)
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {

                // On convertit en tableau en utilisant ';' comme délimiteur
                val lineArray = it.split(";")

                //On vérifie qu'on a bien 4 éléments et qu'on ne récupère pas la 1ere ligne du fichier
                if (lineArray.size == 4 && lineArray[0] != "CODE_UIC") {
                    val station = Station(lineArray[0].toInt(),lineArray[1],lineArray[2].toDouble(),lineArray[3].toDouble())         // On crée un objet station
                    this.stationList.add(station)               // On le rajoute à l'arrayList
                }
            }
        }
        inputStream.close()
    }

    // Fonction permettant de traiter les données de l'api SNCF
    private fun traitementApi() {

        val trainList : ArrayList<Train> = station!!.traitementApi()

        // On change la listView avec le nouvel arrayList
        val listViewTrain = findViewById<ListView>(R.id.list_view)
        listViewTrain.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, trainList)

        listViewTrain.setOnItemClickListener { _, _, position, _ ->

            // On vérifie qu'il n'y a pas d'erreur avant d'ouvrir la map
            if(trainList[position].getTo() != null && trainList[position].getFrom() != null && trainList[position].getStops() != null ){
                val intent = Intent(this@MainActivity, MapsActivity::class.java).apply {
                    putExtra("train", trainList[position])
                }
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // On initialise l'arrayList stationList avec le fichier des gares
        this.initListStations()

        // On change l'AutoCompleteTextView avec l'arrayList des stations
        val autocomplete = findViewById<AutoCompleteTextView>(R.id.myAutoCompleteTextView)
        autocomplete.setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, stationList))

        // On rajoute un eventListener lors d'un click sur un élément
        autocomplete.setOnItemClickListener { adapterView, _, position, _ ->

            // On récupère l'élement, on change la station sélectionné et on fait l'appel à l'api pour changer l'affichage
            val selectedItem = adapterView.getItemAtPosition(position) as Station
            this.station = selectedItem
            this.station!!.run()
            this.traitementApi()

            }

    }

}
