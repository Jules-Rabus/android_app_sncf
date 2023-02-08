package com.example.tp1

import android.os.Parcel
import android.os.Parcelable
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch


class Station(private val codeUIC: Int, private val name: String, private val lon: Double, private val lat: Double) : Parcelable {

    private val httpclient = OkHttpClient()         // Client http
    private var body :JSONObject? = null            // Retour de l'api en Json

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readDouble(),
        parcel.readDouble()
    ) {

    }

    // Fonction faisait l'appel api permettant de récupérer les départs de cette station / "gare"
    fun run() {
        val credential = Credentials.basic("xxxxx", "") // Clé api sncf à mettre dans l'username

        // Requête pour api
        val request = Request.Builder()
            .url("https://api.sncf.com/v1/coverage/sncf/stop_areas/stop_area:SNCF:$codeUIC/departures/?count=8")
            .header("Authorization", credential)
            .build()

        val countDownLatch = CountDownLatch(1)             // permet de d'attendre le retour de la requete async
        httpclient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                countDownLatch.countDown()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        println("Erreur 404 : ${request.url}")
                    }

                    // On récupère et transforme le retour de la requete en Json
                    body = JSONObject(response.body!!.string())
                }
                countDownLatch.countDown()          // On débloque l'éxécution une fois la requête reçu
            }
        })

        countDownLatch.await()

    }

    // // Fonction qui traite le retour de l'api
    fun traitementApi() : ArrayList<Train>{

        val trainList: ArrayList<Train> = ArrayList()

        // On vérifie que le retour de la requete n'est pas vide
        if (body != null) {

            // On récupere et itère sur chaque départ de la gare
            val departs = body!!.getJSONArray("departures")
            for (i in 0 until departs.length()) {

                //On récupère chaque élement qui nous intéresse, et fait le traitement nécessaire pour le transformer en objet train
                val trainJson = departs.getJSONObject(i)
                val trainId = trainJson.getJSONObject("display_informations").getString("trip_short_name").toInt()
                var typeTrain = trainJson.getJSONObject("display_informations").getString("network").split(" ")[0]

                val vehicleJourney = trainJson.getJSONArray("links").getJSONObject(1).getString("id")

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val departDate = trainJson.getJSONObject("stop_date_time").getString("departure_date_time")
                val departHeure = LocalDateTime.parse(departDate, formatter)
                val departHeures = departHeure.format(DateTimeFormatter.ofPattern("HH"))
                val departMinutes = departHeure.format(DateTimeFormatter.ofPattern("mm"))

                //On crée l'objet train et l'ajoute à l'arrayList
                lateinit var train: Train

                // On vérifie qu'il ne s'agit pas de ces types
                if (!typeTrain.equals("Autocar") && !typeTrain.equals("additional") && !typeTrain.equals("Bus")) {

                    // On crée l'objet train et fait l'appel api
                    train = Train(trainId, TypeTrain.valueOf(typeTrain), departHeures, departMinutes)
                    train.run(vehicleJourney, this)
                }
                else {

                    // J'ai rajouté le cas ou il s'agit d'un bus/autocar/additional, cela evite des bugs, pratique un jour de grêve

                    // L'appel api ne pouvant être fait si il ne s'agit pas d'un train, mais on permet quand même l'affichage
                    // On récupère les informations via cette reqûete api afin de crée le from et le to du train
                    // Dans ce cas le train n'aurait pas de stop

                    typeTrain = trainJson.getJSONObject("display_informations").getString("physical_mode").split(" ")[0]
                    train = Train(trainId, TypeTrain.valueOf(typeTrain), departHeures, departMinutes)
                    val from = Stop("", "", "", "", this)
                    train.setFrom(from)
                    val direction = trainJson.getJSONObject("route").getJSONObject("direction").getJSONObject("stop_area")
                    val stationLat = direction.getJSONObject("coord").getString("lat").toDouble()
                    val stationLon = direction.getJSONObject("coord").getString("lon").toDouble()
                    val stationUic = direction.getString("id").split(":")[2].toInt()
                    val stationLibelle = direction.getString("name").split("(")[0]
                    val to = Station(stationUic, stationLibelle, stationLon, stationLat)
                    val stop = Stop("", "", "", "", to)
                    train.setTo(stop)
                }

                // On rajoute le train à l'arraylist
                trainList.add(train)
            }
        }
        return trainList
    }

    fun getName(): String {
        return name
    }

    fun getLon(): Double {
        return lon
    }

    fun getLat(): Double {
        return lat
    }

    fun getCodeUIC(): Int {
        return codeUIC
    }

    override fun toString(): String {
        return name
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(codeUIC)
        parcel.writeString(name)
        parcel.writeDouble(lon)
        parcel.writeDouble(lat)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Station> {
        override fun createFromParcel(parcel: Parcel): Station {
            return Station(parcel)
        }

        override fun newArray(size: Int): Array<Station?> {
            return arrayOfNulls(size)
        }
    }
}
