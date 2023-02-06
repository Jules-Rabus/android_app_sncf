package com.example.tp1

import android.os.Parcel
import android.os.Parcelable
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
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
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    // On récupère et transforme le retour de la requete en Json
                    body = JSONObject(response.body!!.string())
                }
                countDownLatch.countDown()
            }
        })

        countDownLatch.await()

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

    fun getBody(): JSONObject?{
        return body
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
