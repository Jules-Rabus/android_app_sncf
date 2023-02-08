package com.example.tp1

import android.os.Parcel
import android.os.Parcelable
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CountDownLatch

class Train(
    private val num: Int,
    private val type: TypeTrain,
    private val localHour: String,
    private val localMinutes: String,
) : Parcelable {

    private val httpclient = OkHttpClient()         // Client http
    private var from: Stop? = null
    private var to: Stop? = null
    private var stops: ArrayList<Stop> = ArrayList()
    private var body: JSONObject? = null

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        TypeTrain.valueOf(parcel.readString()!!),
        parcel.readString()!!,
        parcel.readString()!!
    ) {
        from = parcel.readParcelable(Stop::class.java.classLoader)
        to = parcel.readParcelable(Stop::class.java.classLoader)
        parcel.readList(stops, Stop::class.java.classLoader)
    }

    fun getNum(): Int {
        return num
    }

    fun getType(): TypeTrain{
        return type
    }

    fun getFrom(): Stop {
        return from!!
    }

    fun setFrom(stop : Stop){
        from = stop
    }

    fun getTo(): Stop{
        return to!!
    }

    fun setTo(stop : Stop){
        to = stop
    }

    fun getLocalHour(): String {
        return localHour
    }

    fun getLocalMinutes(): String {
        return localMinutes
    }

    fun getStops(): ArrayList<Stop> {
        return stops
    }

    // Fonction qui permet l'ajout à l'arraylist les stops du train, ou au from / to
    private fun addStop(stop: Stop, departureStation: Boolean, arrivalStation: Boolean){

        // Si c'est la station de départ on le rajoute au from
        if(departureStation){
            from = stop
            return
        }
        // Si c'est la station d'arrivée on le rajoute au to
        if(arrivalStation){
            to = stop
            return
        }

        // Si ce n'est pas une station d'arrivée / de départ c'est un arret
        if(!arrivalStation || !departureStation){
            stops.add(stop)
        }

    }

    // Fonction faisait l'appel api permettant de récupérer le trajet du train
    fun run(vehicle_journey: String, station: Station) {
        val credential = Credentials.basic("xxxxx", "") // Clé api sncf à mettre dans l'username

        // Requête pour api
        val request = Request.Builder()
            .url("https://api.sncf.com/v1/coverage/sncf/vehicle_journeys/$vehicle_journey/vehicle_journeys")
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
                    if (!response.isSuccessful){
                        println("Erreur 404 : ${request.url}")
                    }
                    else{
                        // On récupère le vehicle journey et transforme le retour de la requete en Json
                        body = JSONObject(response.body!!.string())
                    }

                }
                countDownLatch.countDown()      // On débloque l'éxécution une fois la requête reçu
            }
        })

        countDownLatch.await()
        traitementApi(station, false)       // On appel la fonction qui traite l'api et ne rajoute que les arrets à partir de la gare sélectionné

    }

    private fun dateHeures(date: String): String{
        return date.substring(0,2)
    }

    private fun dateMinutes(date: String): String{
        return date.substring(2,4)
    }

    // Fonction qui traite le retour de l'api
    private fun traitementApi(stationDepart : Station, trajetAvant: Boolean) {

        // On effectue le traitement que si le body a bien reçu une requête
        if (body != null) {

            //  On récupère les arrets
            val stopApi = body!!.getJSONArray("vehicle_journeys").getJSONObject(0).getJSONArray("stop_times")

            // Boolean qui permet de récupérer les trajets qu'à partir de la gare selectionné si false
            // Si true la gare de départ est la gare d'origine du train
            // Si false la gare de départ est la gare selectionné dans l'autocomplete
            var trajetAvantStatut: Boolean = trajetAvant

            for (i in 0 until stopApi.length()) {

                // On récupère le code de la gare
                val stationUic = stopApi.getJSONObject(i).getJSONObject("stop_point").getString("id").split(":")[2].toInt()

                // On vérifie si il s'agit de la gare sélectionné pour ajouter les stops à partir de ce moment, si trajetAvant était = false
                if (stationUic == stationDepart.getCodeUIC()) {
                    trajetAvantStatut = true
                }

                if (trajetAvantStatut) {

                    // On récupère les informations venant de l'api pour faire un stop et une station
                    val dateDepart = stopApi.getJSONObject(i).getString("departure_time")
                    val dateArrivee = stopApi.getJSONObject(i).getString("arrival_time")
                    val stationLat = stopApi.getJSONObject(i).getJSONObject("stop_point").getJSONObject("coord").getString("lat").toDouble()
                    val stationLon = stopApi.getJSONObject(i).getJSONObject("stop_point").getJSONObject("coord").getString("lon").toDouble()
                    val stationLibelle = stopApi.getJSONObject(i).getJSONObject("stop_point").getString("name").split("(")[0]

                    // On crée les objets stop et station
                    val station = Station(stationUic, stationLibelle, stationLon, stationLat)
                    val stop = Stop(
                        dateHeures(dateDepart),
                        dateMinutes(dateDepart),
                        dateHeures(dateArrivee),
                        dateMinutes(dateArrivee),
                        station
                    )

                    if (stationUic == stationDepart.getCodeUIC() && from == null || trajetAvant && from == null ) {
                        // On ajoute un départ que si il s'agit de la gare sélectionné et le from n'a pas encore été initialisé
                        // Ou on ajoute un départ si le from n'a pas encore été initialisé et qu'on veut afficher le trajet depuis la réel gare de départ du train
                        addStop(stop, true, false)
                    } else if (i == stopApi.length() - 1) {
                        // On ajoute l'arrivée
                        addStop(stop, false, true)
                    } else {
                        // On ajoute un stop / un "arret"
                        addStop(stop, false, false)
                    }
                }
            }
        }
    }

    override fun toString(): String {
        if(to == null){
            return localHour + "h" + localMinutes + " $type $num"
        }
        else{
            return localHour + "h" + localMinutes + " - " + to!!.getStation().getName() + " $type $num"
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(num)
        parcel.writeString(type.name)
        parcel.writeString(localHour)
        parcel.writeString(localMinutes)
        parcel.writeParcelable(from, flags)
        parcel.writeParcelable(to, flags)
        parcel.writeList(stops)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Train> {
        override fun createFromParcel(parcel: Parcel): Train {
            return Train(parcel)
        }

        override fun newArray(size: Int): Array<Train?> {
            return arrayOfNulls(size)
        }
    }


}

