package com.example.ojulija

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ojulija.data.User
import com.example.ojulija.databinding.ActivityRegisterBinding
import com.example.ojulija.viewmodels.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val REQUEST_IMAGE_FROM_GALLERY = 23
    private val REQUEST_IMAGE_CAMERA = 34

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        requestCameraPermission()
        binding.imageView.setOnClickListener {
            loadImageFromStorage()
        }
        binding.btnRegister.setOnClickListener {

            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Morate da uneste sva polja!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity()) { task ->
                    if (task.isSuccessful) {

                        //dodavanje korisnika u realTimeDatabase
                        uploadUserToRealtimeDatabase(email, password)


                        Toast.makeText(this, "Uspesno ste dodali korisnika", Toast.LENGTH_SHORT)
                            .show()


                    } else {
                        Toast.makeText(this, "Niste dodali korisnika", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

        }

    }

    private fun requestCameraPermission() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        )
        if(permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                1
            )
        }
    }

    private fun loadImageFromStorage() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Select image from ?")
            .setMessage("Choose your option")
            .setPositiveButton("Gellery") { dialog, which ->
                dialog.dismiss()
                val intent = Intent()
                intent.action = Intent.ACTION_PICK
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_IMAGE_FROM_GALLERY)
            }.setNegativeButton("Camera") { dialog,which->
                dialog.dismiss()

                Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE
                ).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        val permission = ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.CAMERA
                        )
                        if (permission != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(android.Manifest.permission.CAMERA),
                                1
                            )

                        } else {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAMERA)
                        }
                    }
                }

            }
        val dialog:AlertDialog = builder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && requestCode == REQUEST_IMAGE_FROM_GALLERY) {
            if (data.data != null){
                binding.imageView.setImageURI(data.data)
            }
        }else if(data != null && requestCode == REQUEST_IMAGE_CAMERA){
            if(data.data!=null){
                val imageBitmap = data.extras?.get("data") as Bitmap
                binding.imageView.setImageURI(data.data)
            }
        }
        else{
            Toast.makeText(this, "Nema brale lose nesto", Toast.LENGTH_SHORT).show()
        }

    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "Title",
            null
        )
        return Uri.parse(path)
    }

    private fun uploadUserToRealtimeDatabase(email: String, password: String) {

        val uid = auth.currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/${uid}")

        val user = uid?.let {
            User(uid, name = "Julija", surname = "Ristic", "06428738", email)
        }
        ref.setValue(user)
            .addOnSuccessListener {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

    }
}