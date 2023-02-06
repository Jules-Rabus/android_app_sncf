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

    fun getStops(): ArrayList<Stop>? {
        return stops
    }

    private fun addStop(stop: Stop, departureStation: Boolean, arrivalStation: Boolean){
        if(departureStation){
            from = stop
            return
        }
        if(arrivalStation){
            to = stop
            return
        }
        if(!arrivalStation || !departureStation){
            stops!!.add(stop)
        }

    }

    fun run(vehicle_journey: String) {
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
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    // On récupère et transforme le retour de la requete en Json
                    body = JSONObject(response.body!!.string())

                }
                countDownLatch.countDown()
            }
        })

        countDownLatch.await()
        traitementApi()

    }

    private fun apiFrom(){

    }

    private fun dateHeures(date: String): String{
        return date.substring(0,2)
    }

    private fun dateMinutes(date: String): String{
        return date.substring(2,4)
    }

    private fun traitementApi(){
        val stopApi = body!!.getJSONArray("vehicle_journeys").getJSONObject(0).getJSONArray("stop_times")

        for (i in 0 until stopApi.length()) {
            val dateDepart = stopApi.getJSONObject(i).getString("departure_time")
            val dateArrivee = stopApi.getJSONObject(i).getString("arrival_time")
            val stationLat = stopApi.getJSONObject(i).getJSONObject("stop_point").getJSONObject("coord").getString("lat").toDouble()
            val stationLon = stopApi.getJSONObject(i).getJSONObject("stop_point").getJSONObject("coord").getString("lon").toDouble()
            val stationUic = stopApi.getJSONObject(i).getJSONObject("stop_point").getString("id").split(":")[2].toInt()
            val stationLibelle = stopApi.getJSONObject(i).getJSONObject("stop_point").getString("name").split("(")[0]

            val station = Station(stationUic,stationLibelle,stationLon,stationLat)
            val stop = Stop(
                dateHeures(dateDepart),
                dateMinutes(dateDepart),
                dateHeures(dateArrivee),
                dateMinutes(dateArrivee),
                station
            )

            if(i == 0){
                addStop(stop,true,false)
            }
            else if(i == stopApi.length()-1){
                addStop(stop,false,true)
            }
            else{
                addStop(stop,false,false)
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

