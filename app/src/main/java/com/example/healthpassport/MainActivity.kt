package com.example.healthpassport

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import com.vansuita.pickimage.bean.PickResult
import com.vansuita.pickimage.bundle.PickSetup
import com.vansuita.pickimage.dialog.PickImageDialog
import com.vansuita.pickimage.listeners.IPickResult
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File


class MainActivity : AppCompatActivity(), IPickResult  {

    private val TAG = "MyActivity"
    private val fileName = "covid-tickets.txt"
    private val imageName = "covid-image.png"
    private lateinit var userImage: ImageButton
    private lateinit var deleteButton: Button
    private lateinit var confirmationText: TextView
    private lateinit var missingImageText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        val file = File(filesDir, this.fileName)
        val imageFile = File(filesDir, this.imageName)
        if (!file.exists()) {
            file.createNewFile()
        }


        this.fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            scanQRCode()
        }

        this.userImage = findViewById(R.id.user_image)
        this.confirmationText = findViewById(R.id.confirmation)
        this.missingImageText = findViewById(R.id.missing_image)
        this.welcomeText = findViewById(R.id.welcome_text)

        if (!imageFile.exists() || imageFile.length() == 0.toLong()) {
            imageFile.createNewFile()
            this.userImage.setOnClickListener {
                PickImageDialog.build(PickSetup()).show(this)
            }
        } else {
            val bitmap: Bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            this.userImage.setImageBitmap(bitmap)
            this.missingImageText.visibility = View.INVISIBLE
        }

        val confirmationData = file.readText()

        this.deleteButton = findViewById(R.id.delete)
        this.deleteButton.setOnClickListener {
            deleteData()
        }

        if (confirmationData.isNotEmpty()) {
            runOnUiThread {
                val text: TextView = findViewById(R.id.text_home)
                text.text = confirmationData
                this.welcomeText.visibility = View.INVISIBLE
                this.fab.visibility = View.INVISIBLE
            }
        } else {
            this.confirmationText.visibility = View.INVISIBLE
            this.userImage.visibility = View.INVISIBLE
            this.deleteButton.visibility = View.INVISIBLE
            this.missingImageText.visibility = View.INVISIBLE
        }
    }

    private fun scanQRCode(){
        val integrator = IntentIntegrator(this).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(true)
            setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            setPrompt("Scanning Code")
        }
        integrator.initiateScan()
    }

    private fun deleteData() {
        this.openFileOutput(this.fileName, Context.MODE_PRIVATE).use {
            it.write("".toByteArray())
        }

        val imageFile = File(filesDir, this.imageName)
        imageFile.delete();

        finish();
        startActivity(intent);
    }

    override fun onPickResult(r: PickResult) {
        if (r.getError() == null) {
            this.userImage.setImageBitmap(r.getBitmap())
            this.userImage.setOnClickListener(null)
            this.missingImageText.visibility = View.INVISIBLE
            this.openFileOutput(this.imageName, Context.MODE_PRIVATE).use {
                r.getBitmap().compress(Bitmap.CompressFormat.PNG, 85, it)
                it.flush()
                it.close()
            }
        }
    }

    // Get the results:
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val context = this
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) Toast.makeText(this, "Nevalidan kod", Toast.LENGTH_LONG)
                .show()
            else {
                // Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
                try {
                    doAsync {
                        val doc = Jsoup.connect(result.contents).get()
                        val type = if (doc.title() == "COVID-19 Vaccination Validation") "Vakcina" else "PCR test";
                        val successBox: Element? = doc.selectFirst(".login-box-body .alert-success")

                        if (successBox !== null) {
                            val covidDocs = successBox.select("strong")
                            val name = covidDocs[0]
                            val code = covidDocs[1]
                            val confirmationData =
                                "Ime i prezime: " + name.text() + System.lineSeparator() + "Tip potvrde: " + type + System.lineSeparator() + "Šifra potvrde: " + code.text()

                            context.openFileOutput(context.fileName, Context.MODE_PRIVATE).use {
                                it.write(confirmationData.toByteArray())
                            }

                            runOnUiThread {
                                val text: TextView = findViewById(R.id.text_home)
                                text.text = confirmationData
                                context.userImage.visibility = View.VISIBLE
                                context.deleteButton.visibility = View.VISIBLE
                                context.confirmationText.visibility = View.VISIBLE
                                context.welcomeText.visibility = View.INVISIBLE
                                context.fab.visibility = View.INVISIBLE

                                val imageFile = File(filesDir, context.imageName)
                                if (!imageFile.exists() || imageFile.length() == 0.toLong()) {
                                    context.missingImageText.visibility = View.VISIBLE
                                }
                            }

                            context.userImage.setOnClickListener {
                                PickImageDialog.build(PickSetup()).show(context)
                            }

                        } else {
                            Toast.makeText(context, "Nevalidan kod", Toast.LENGTH_LONG).show()
                        }
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Nevalidan kod", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

}