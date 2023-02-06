package com.example.tp1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tp1.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var train: Train

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        train = intent.getParcelableExtra<Train>("train")!!
        val textViewTrain = findViewById<TextView>(R.id.textViewTrain)
        textViewTrain.text = "${train.getType()} n°${train.getNum()}  ${train.getFrom().getStation().getName()} - ${train.getTo().getStation().getName()}"

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        addStop(mMap,train.getFrom(),"Départ");

        val stops = train.getStops()
        val count = stops!!.size
        if( count > 0){
            for (i in 0 until count) {
                addStop(mMap, stops.get(i),"Arrivée")
            }
        }


        addStop(mMap,train.getTo(),"Arrivée");
        // Add a marker in Sydney and move the camera
        val stationFrom = train.getFrom().getStation()
        val from = LatLng(stationFrom.getLat(), stationFrom.getLon())
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10F))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(from))
    }

    private fun addStop(map: GoogleMap, stop: Stop, type: String){
        val station = stop.getStation()
        val coord = LatLng(station.getLat(),station.getLon())
        lateinit var marker: MarkerOptions

        if(!stop.getHeure().equals("h")){
            marker = MarkerOptions()
                .position(coord)
                .title(station.getName())
                .snippet("$type : ${stop.getHeure()}")
        }
        else{
            marker = MarkerOptions()
                .position(coord)
                .title(station.getName())
        }

        mMap.addMarker(marker)
    }


}