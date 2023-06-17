package com.example.bibmusi

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest


class MainActivity : AppCompatActivity() {

    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: SongAdapter

    private val dbHelper: SQLiteOpenHelper = object : SQLiteOpenHelper(this, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            val createTableQuery = "CREATE TABLE $TABLE_NAME " +
                    "($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COL_ARTIST TEXT, " +
                    "$COL_SONG TEXT, " +
                    "$COL_ALBUM TEXT, " +
                    "$COL_YEAR INTEGER)"
            db.execSQL(createTableQuery)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }

    private lateinit var editTextArtist: EditText
    private lateinit var editTextSong: EditText
    private lateinit var editTextAlbum: EditText
    private lateinit var editTextYear: EditText
    private lateinit var buttonAdd: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnMostrarMapa: Button

    private var selectedSongId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextArtist = findViewById(R.id.editTextArtist)
        editTextSong = findViewById(R.id.editTextSong)
        editTextAlbum = findViewById(R.id.editTextAlbum)
        editTextYear = findViewById(R.id.editTextYear)
        buttonAdd = findViewById(R.id.buttonAdd)
        recyclerView = findViewById(R.id.recyclerView)
        btnMostrarMapa = findViewById(R.id.Ubicacionapa)
        btnMostrarMapa.setOnClickListener{
            mostrarMapa()
        }

        adapter = SongAdapter(::onSongItemClick)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        buttonAdd.setOnClickListener { addOrUpdateData() }

        db = dbHelper.writableDatabase

        displayData()
    }

    private fun mostrarMapa(){
        val gmmIntentUri = Uri.parse("geo:21.8888342, -102.2613258(DiscoOfertas)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        }
    }

    private fun addOrUpdateData() {
        val artist = editTextArtist.text.toString().trim()
        val song = editTextSong.text.toString().trim()
        val album = editTextAlbum.text.toString().trim()
        val year = editTextYear.text.toString().toIntOrNull()

        if (artist.isEmpty() || song.isEmpty() || album.isEmpty() || year == null) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val values = ContentValues().apply {
            put(COL_ARTIST, artist)
            put(COL_SONG, song)
            put(COL_ALBUM, album)
            put(COL_YEAR, year)
        }

        if (selectedSongId == -1L) {
            val newRowId = db.insert(TABLE_NAME, null, values)
            if (newRowId != -1L) {
                Toast.makeText(this, "Datos añadidos correctamente", Toast.LENGTH_SHORT).show()
                clearFields()
                displayData()
            } else {
                Toast.makeText(this, "Error al añadir los datos", Toast.LENGTH_SHORT).show()
            }
        } else {
            val selection = "$COL_ID = ?"
            val selectionArgs = arrayOf(selectedSongId.toString())
            val updatedRows = db.update(TABLE_NAME, values, selection, selectionArgs)
            if (updatedRows > 0) {
                Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                clearFields()
                displayData()
            } else {
                Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
            }
            selectedSongId = -1
            buttonAdd.text = "Añadir"
        }
    }

    private fun clearFields() {
        editTextArtist.text.clear()
        editTextSong.text.clear()
        editTextAlbum.text.clear()
        editTextYear.text.clear()
    }

    private fun displayData() {
        val cursor: Cursor = db.query(TABLE_NAME, null, null, null, null, null, null)

        val songs = mutableListOf<Song>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex(COL_ID))
            val artist = cursor.getString(cursor.getColumnIndex(COL_ARTIST))
            val song = cursor.getString(cursor.getColumnIndex(COL_SONG))
            val album = cursor.getString(cursor.getColumnIndex(COL_ALBUM))
            val year = cursor.getInt(cursor.getColumnIndex(COL_YEAR))

            val songItem = Song(id, artist, song, album, year)
            songs.add(songItem)
        }

        cursor.close()

        adapter.setSongs(songs)
    }

    private fun onSongItemClick(song: Song) {
        selectedSongId = song.id
        editTextArtist.setText(song.artist)
        editTextSong.setText(song.song)
        editTextAlbum.setText(song.album)
        editTextYear.setText(song.year.toString())
        buttonAdd.text = "Actualizar"
    }

    private fun deleteData(song: Song) {
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(song.id.toString())
        val deletedRows = db.delete(TABLE_NAME, selection, selectionArgs)
        if (deletedRows > 0) {
            Toast.makeText(this, "Datos eliminados correctamente", Toast.LENGTH_SHORT).show()
            clearFields()
            displayData()
        } else {
            Toast.makeText(this, "Error al eliminar los datos", Toast.LENGTH_SHORT).show()
        }
        selectedSongId = -1
        buttonAdd.text = "Añadir"
    }

    inner class SongAdapter(private val onItemClickListener: (Song) -> Unit) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
        private var songs: List<Song> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
            return SongViewHolder(view)
        }

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            val song = songs[position]
            holder.bind(song)
        }

        override fun getItemCount(): Int {
            return songs.size
        }

        fun setSongs(songs: List<Song>) {
            this.songs = songs
            notifyDataSetChanged()
        }

        inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(R.id.textView)
            private val buttonEdit: Button = itemView.findViewById(R.id.buttonEdit)
            private val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)

            fun bind(song: Song) {
                val songInfo = "Artista: ${song.artist}\nCanción: ${song.song}\nÁlbum: ${song.album}\nAño: ${song.year}"
                textView.text = songInfo

                buttonEdit.setOnClickListener { onItemClickListener(song) }
                buttonDelete.setOnClickListener { deleteData(song) }
            }
        }
    }

    data class Song(val id: Long, val artist: String, val song: String, val album: String, val year: Int)

    companion object {
        private const val DATABASE_NAME = "songs.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "songs"
        private const val COL_ID = "id"
        private const val COL_ARTIST = "artist"
        private const val COL_SONG = "song"
        private const val COL_ALBUM = "album"
        private const val COL_YEAR = "year"
    }
    class MainActivity : AppCompatActivity(), OnMapReadyCallback {

        private lateinit var mapView: MapView
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private var googleMap: GoogleMap? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            mapView = findViewById(R.id.mapView)
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this)

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }

        override fun onMapReady(map: GoogleMap?) {
            googleMap = map
            googleMap?.apply {

                clear()

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)
                            addMarker(MarkerOptions().position(currentLatLng).title("Mi ubicación actual"))
                            moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12.0f))
                        }
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
            }
        }

        override fun onResume() {
            super.onResume()
            mapView.onResume()
        }

        override fun onPause() {
            super.onPause()
            mapView.onPause()
        }

        override fun onDestroy() {
            super.onDestroy()
            mapView.onDestroy()
        }

        override fun onLowMemory() {
            super.onLowMemory()
            mapView.onLowMemory()
        }

        companion object {
            private const val REQUEST_LOCATION_PERMISSION = 1
        }
    }

}

