package com.example.tp1

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tp1.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var train: Train

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // On récupère le train dans le intent
        train = intent.getParcelableExtra<Train>("train")!!
        val textViewTrain = findViewById<TextView>(R.id.textViewTrain)

        // On affiche les informations du train dans le textView
        val stringTextView = "${train.getType()} n°${train.getNum()} \n  ${train.getFrom().getStation().getName()} - ${train.getTo().getStation().getName()}"
        textViewTrain.text = stringTextView

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
        val trajetPolyline = PolylineOptions()

        // On rajoute le marker du départ et ses coordonnées
        addStop(train.getFrom(),"Départ")
        var station = train.getFrom().getStation()
        trajetPolyline.add(LatLng(station.getLat(),station.getLon()))

        // On rajoute les marker des stops / "arrets" et leur cordonnées
        val stops = train.getStops()
        val count = stops.size
        if( count > 0){
            for (i in 0 until count) {
                station = stops.get(i).getStation()
                trajetPolyline.add(LatLng(station.getLat(),station.getLon()))
                addStop(stops.get(i),"Arrivée")
            }
        }

        // On rajoute le marker de l'arrivée et ses coordonnées
        addStop(train.getTo(),"Arrivée")
        station = train.getTo().getStation()
        trajetPolyline.add(LatLng(station.getLat(),station.getLon()))

        // On rajoute le tracé du trajte
        googleMap.addPolyline(trajetPolyline)

        // On zoom et déplace la "caméra" sur la gare de départ
        val stationFrom = train.getFrom().getStation()
        val from = LatLng(stationFrom.getLat(), stationFrom.getLon())
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10F))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(from))
    }

    private fun addStop(stop: Stop, type: String){

        // On récupère la gare
        val station = stop.getStation()
        val coord = LatLng(station.getLat(),station.getLon())
        lateinit var marker: MarkerOptions

        // On vérifie que le format de l'heure est correcte sinon on ne le rajoute pas
        if(!stop.getHeure().equals("h")){
            marker = MarkerOptions()
                .position(coord)
                .title(station.getName())
                .snippet("$type : ${stop.getHeure()}")
        }
        else{
            // Ce cas permet de rien afficher quand on a pas les horaires pour les bus/autocar
            marker = MarkerOptions()
                .position(coord)
                .title(station.getName())
        }

        // On rajoute le marker
        mMap.addMarker(marker)
    }


}