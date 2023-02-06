package com.example.tp1

import android.os.Parcel
import android.os.Parcelable

class Stop(
    private val hourArrival: String,
    private val minuteArrival: String,
    private val hourDeparture: String,
    private val minuteDeparture: String,
    private val station: Station
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readParcelable(Station::class.java.classLoader)!!
    ) {
    }

    fun getStation(): Station{
        return station
    }

    fun getHeure(): String{
        return hourArrival + "h" + minuteArrival
    }

    override fun toString(): String {
        return "Stop(hourArrival='$hourArrival', minuteArrival='$minuteArrival', hourDeparture='$hourDeparture', minuteDeparture='$minuteDeparture', station=$station)"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(hourArrival)
        parcel.writeString(minuteArrival)
        parcel.writeString(hourDeparture)
        parcel.writeString(minuteDeparture)
        parcel.writeParcelable(station, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Stop> {
        override fun createFromParcel(parcel: Parcel): Stop {
            return Stop(parcel)
        }

        override fun newArray(size: Int): Array<Stop?> {
            return arrayOfNulls(size)
        }
    }
}