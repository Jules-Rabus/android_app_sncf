package com.example.tp1

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val stationList : ArrayList<Station> = ArrayList()  // On stock dans un arrayList les stations
    private var station: Station? = null                        // On stock la station actuellement sélectionné

    // Fonction pour remplir l'arrayList en utilisant le fichier gares.csv
    private fun initListStations() {

        // On récupere le fichier gares.csv et on itère sur chaque ligne
        val inputStream = resources.openRawResource(R.raw.gares)
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {

                // On converti en tableau en utilisant ';' comme délimiteur
                val lineArray = it.split(";")

                //On vérifie qu'on a bien 4 élémeents et qu'on ne récupère pas la 1ere ligne du fichier
                if (lineArray.size == 4 && lineArray[0] != "CODE_UIC") {
                    val station = Station(lineArray[0].toInt(),lineArray[1],lineArray[2].toDouble(),lineArray[3].toDouble())         // On crée un objet station
                    this.stationList.add(station)               // On le rajoute à l'arrayList
                }
            }
        }
        inputStream.close()
    }

    // fonction permettant de traiter les données de l'api SNCF
    private fun traitementApi() {

        // On récupère la requete
        val body = station!!.getBody()      // le !!, fait que l'élement ne peut être null

        // On vérifie que la requete n'est pas vide
        if (body != null) {

            // On récupere et itère sur chaque départ de la gare
            val departs = body.getJSONArray("departures")
            val trainList: ArrayList<Train> = ArrayList()
            for (i in 0 until departs.length()) {

                //On récupère chaque élement qui nous intéresse, et fait le traitement nécessaire pour le transformer en objet train
                val trainJson = departs.getJSONObject(i)
                val trainId = trainJson.getJSONObject("display_informations").getString("trip_short_name").toInt()
                val typeTrain = trainJson.getJSONObject("display_informations").getString("physical_mode").split(" ")[0]

                val vehicle_journey = trainJson.getJSONArray("links").getJSONObject(1).getString("id")

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val departDate = trainJson.getJSONObject("stop_date_time").getString("departure_date_time")
                val departHeure = LocalDateTime.parse(departDate, formatter)
                val departHeures = departHeure.format(DateTimeFormatter.ofPattern("HH"))
                val departMinutes = departHeure.format(DateTimeFormatter.ofPattern("mm"))

                //On crée l'objet train et l'ajoute à l'arrayList
                val train = Train(trainId, TypeTrain.valueOf(typeTrain), departHeures, departMinutes)
                if(!typeTrain.equals("Autocar") && !typeTrain.equals("additional") && !typeTrain.equals("Bus")){
                    train.run(vehicle_journey)
                }
                else{

                    val from = Stop("","","","",station!!)
                    train.setFrom(from);
                    val direction = trainJson.getJSONObject("route").getJSONObject("direction").getJSONObject("stop_area")
                    val stationLat = direction.getJSONObject("coord").getString("lat").toDouble()
                    val stationLon = direction.getJSONObject("coord").getString("lon").toDouble()
                    val stationUic = direction.getString("id").split(":")[2].toInt()
                    val stationLibelle = direction.getString("name").split("(")[0]
                    val to = Station(stationUic,stationLibelle,stationLon,stationLat)
                    val stop = Stop("","","","",to);
                    train.setTo(stop)
                }
                trainList.add(train)

            }

            // On change la listView avec le nouvel arrayList
            val listViewTrain = findViewById<ListView>(R.id.list_view)
            listViewTrain.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, trainList)

            listViewTrain.setOnItemClickListener { _, _, position, _ ->
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
