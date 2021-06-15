package br.edu.infnet.luis_barbosa_dr4_at

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders
import br.edu.infnet.luis_barbosa_dr4_at.Util.EXTRA_DATA
import br.edu.infnet.luis_barbosa_dr4_at.Util.EXTRA_ID
import br.edu.infnet.luis_barbosa_dr4_at.Util.EXTRA_IMAGEM
import br.edu.infnet.luis_barbosa_dr4_at.Util.EXTRA_LOCAL
import br.edu.infnet.luis_barbosa_dr4_at.Util.EXTRA_TEXTO
import br.edu.infnet.luis_barbosa_dr4_at.Util.EXTRA_TITULO
import br.edu.infnet.luis_barbosa_dr4_at.cryptography.Encrypto
import br.edu.infnet.luis_barbosa_dr4_at.model.Note
import br.edu.infnet.luis_barbosa_dr4_at.viewModel.NoteViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_add_note.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.*


class AddNoteActivity : AppCompatActivity() {

    private lateinit var noteViewModel: NoteViewModel
    private val REQUEST_PERMISSION_CODE = 1009
    private val REQUEST_IMAGE_CAPTURE = 1001
    private val GRANTED = PackageManager.PERMISSION_GRANTED
    private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private val EXTERNAL = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val CAMERA  = Manifest.permission.CAMERA
    private var LATITUDE = ""
    private var LONGITUDE = ""
    private var LOCATION = ""
    private var imageBitmap: Bitmap? = null
    private var encodedImageString = ""
    private val encrypto = Encrypto()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val id = intent.getIntExtra(EXTRA_ID, 0)

        this.let {
                act -> noteViewModel = ViewModelProviders.of(act)
            .get(NoteViewModel::class.java)
        }

        fillFieldsData()
        setupListeners(id)
        getLocation()
        changeImage()
    }

    private fun setupListeners(id: Int) {
        btn_save_note.setOnClickListener {
            val retornoIntent = Intent()
            val titulo = add_titulo_input.text.toString()
            val data = add_data_input.text.toString()
            val foto = encodedImageString
            val localizacao = LOCATION
            val texto = add_text_input.text.toString()

            if (encodedImageString.isNotEmpty() && titulo.isNotEmpty() &&
                    data.isNotEmpty() && texto.isNotEmpty()) {
                retornoIntent.putExtra(EXTRA_ID, id)
                retornoIntent.putExtra(EXTRA_TITULO, titulo)
                retornoIntent.putExtra(EXTRA_DATA, data)
                retornoIntent.putExtra(EXTRA_IMAGEM, foto)
                retornoIntent.putExtra(EXTRA_LOCAL, localizacao)
                retornoIntent.putExtra(EXTRA_TEXTO, texto)

                actionWriteExternal()

                setResult(Activity.RESULT_OK, retornoIntent)
                finish()
            } else {
                showSnackbar("Preencha todos campos!")
            }
        }
    }

    private fun fillFieldsData(){
        val titulo = intent.getStringExtra(EXTRA_TITULO)

        if (titulo != null) {
            val data = intent.getStringExtra(EXTRA_DATA)
            encodedImageString = intent.getStringExtra(EXTRA_IMAGEM)!!
            LOCATION = intent.getStringExtra(EXTRA_LOCAL)!!
            val texto = intent.getStringExtra(EXTRA_TEXTO)
            btn_save_note.text = getString(R.string.update)
            add_titulo_input.setText(titulo.toString())
            add_data_input.setText(data.toString())
            add_text_input.setText(texto.toString())
            tv_add_location.text = LOCATION

            val bytarray: ByteArray = Base64.decode(encodedImageString, Base64.DEFAULT)
            val bmimage = BitmapFactory.decodeByteArray(
                bytarray, 0,
                bytarray.size
            )

            img_add.setImageBitmap(bmimage)
        }
    }

    private fun changeImage() {
        btn_alterar_imagem.setOnClickListener {
            when {
                checkSelfPermission(CAMERA) == GRANTED -> captureImage()
                shouldShowRequestPermissionRationale(CAMERA) -> showDialogPermission(
                    "É preciso liberar o acesso à câmera!",
                    arrayOf(CAMERA)
                )
                else -> requestPermissions(
                    arrayOf(CAMERA),
                    REQUEST_IMAGE_CAPTURE
                )
            }
        }
    }

    private fun captureImage(){
        val capturaImagemIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(capturaImagemIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            showSnackbar("${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    imageBitmap = data!!.extras!!["data"] as Bitmap?
                    img_add.setImageBitmap(imageBitmap)

                    val baos = ByteArrayOutputStream()
                    imageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val b: ByteArray = baos.toByteArray()
                    encodedImageString = Base64.encodeToString(b, Base64.DEFAULT)

                } catch (e: Exception) {
                    showSnackbar("${e.message}")
                }
            }
        }
    }

    fun actionWriteExternal(){
        when {
            checkSelfPermission(EXTERNAL) == GRANTED -> writeInAExternalFile()
            shouldShowRequestPermissionRationale(EXTERNAL) -> showDialogPermission(
                "É preciso liberar o acesso ao armazenamento externo!",
                arrayOf(EXTERNAL)
            )
            else -> requestPermissions(
                arrayOf(EXTERNAL),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    private fun writeInAExternalFile(){
        val titulo = intent.getStringExtra(EXTRA_TITULO)
        val texto = intent.getStringExtra(EXTRA_TEXTO)
        val data = getDate()

        if (titulo != null && texto != null) {
            val nomeArquivoTxt = "$titulo($data).txt"
            val nomeArquivoFig = "$titulo($data).fig"
            val fileTxt = File(getExternalFilesDir(null), nomeArquivoTxt)
            val fileFig = File(getExternalFilesDir(null), nomeArquivoFig)

            if (fileTxt.exists()) {
                fileTxt.delete()
            } else {
                try {
                    FileOutputStream(fileTxt).let {
                        it.write("${encrypto.cipher(texto)}".toByteArray())
                        it.close()
                    }
                } catch (e: Exception) {
                    showSnackbar("${e.message}")
                }
            }

            if (fileFig.exists()) {
                fileFig.delete()
            } else {
                try {
                    FileOutputStream(fileFig).let {
                        it.write("${encrypto.cipher(encodedImageString)}".toByteArray())
                        it.close()
                    }
                } catch (e: Exception) {
                    showSnackbar("${e.message}")
                }
            }
        }
    }

    private fun getCurrentLocation() {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(
            LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.d("Permissao", "Ative os serviços necessários")
        } else {
            when {
                isGPSEnabled -> {
                    try {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            2000L, 0f, locationListener)
                    } catch(ex: SecurityException) {
                        Log.d("Permissao", "Security Exception")
                    }
                }
                isNetworkEnabled -> {
                    try {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            2000L, 0f, locationListener)
                    } catch(ex: SecurityException) {
                        Log.d("Permissao", "Security Exception")
                    }
                }
            }
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            LATITUDE = location.latitude.toString()
            LONGITUDE = location.longitude.toString()
            LOCATION = "LAT: $LATITUDE LOG: $LONGITUDE"
            tv_add_location.text = LOCATION
        }

        override fun onProviderDisabled(provider: String) {
            showSnackbar("$provider off")
        }

        override fun onProviderEnabled(provider: String) {
            showSnackbar("$provider on")
        }
    }

    fun getLocation() {
        val permissionACL = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val permissionAFL = checkSelfPermission(FINE_LOCATION)

        if (permissionACL == GRANTED || permissionAFL == GRANTED)
            getCurrentLocation()
        else {
            if (shouldShowRequestPermissionRationale(FINE_LOCATION))
                showDialogPermission(
                    "É necessário conceder as permissões " +
                            "para todos os recurso disponíveis.",
                    arrayOf(FINE_LOCATION)
                )
            else
                requestPermissions(arrayOf(FINE_LOCATION),
                    REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] == GRANTED)
                        when (permission) {
                            FINE_LOCATION -> getCurrentLocation()
                            EXTERNAL -> writeInAExternalFile()
                        }
                    }
                }
            }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showDialogPermission(
        message: String, permissions: Array<String>
    ) {
        val alertDialog = AlertDialog
            .Builder(this)
            .setTitle("Permissões")
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ ->
                requestPermissions(
                    permissions,
                    REQUEST_PERMISSION_CODE)
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        alertDialog.show()
    }

    private fun getDate(): String {
        val date = Calendar.getInstance().time
        return DateFormat.getDateInstance(DateFormat.LONG).format(date)
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar
            .make(
                root_layout,
                message,
                duration
            ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow_add_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_delete -> {
                val titulo = intent.getStringExtra(EXTRA_TITULO)
                val retornoIntent = Intent()
                if (titulo != null) {
                    actionDeleteNote()
                    setResult(RESULT_CANCELED, retornoIntent)
                    finish()
                }
            }
        }
        return true
    }

    private fun actionDeleteNote() {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val titulo = intent.getStringExtra(EXTRA_TITULO)
        val data = intent.getStringExtra(EXTRA_DATA)
        val imagem = intent.getStringExtra(EXTRA_IMAGEM)
        val localizacao = intent.getStringExtra(EXTRA_LOCAL)
        val texto = intent.getStringExtra(EXTRA_TEXTO)

        val note = Note(
            id,
            titulo!!,
            data!!,
            imagem!!,
            localizacao!!,
            texto!!
        )

        noteViewModel.deleteNote(note)
    }
}