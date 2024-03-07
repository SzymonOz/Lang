package com.example.lang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate


class createProfile : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_profile)

        val btn_click_me = findViewById<Button>(R.id.button)

            btn_click_me.setOnClickListener {

            val name = findViewById<EditText>(R.id.editTextText)
            if(name.getText().toString().length < 5) {
                Toast.makeText(this, "Nazwa musi mieć minimum 5 znaków.", Toast.LENGTH_SHORT).show()
            }else{
                btn_click_me.isEnabled = false;
                val tx = name.getText().toString();
                val current = LocalDate.now()
                var namehash = md5(tx+current)
                val data:String = namehash+"::"+tx
                val sharedPreference =  getSharedPreferences("my",Context.MODE_PRIVATE)
                var editor = sharedPreference.edit()
                editor.putString("Name",data)
                editor.commit()
                val i: Intent = Intent(this, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                finish()

            }

        }
    }

    fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }
}