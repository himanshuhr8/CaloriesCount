package com.example.caloriecount

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.caloriecount.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val API_URL = "https://calorie.himanshurajhr8.workers.dev/analyze" // Replace with your backend URL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigate to Profile Activity
        binding.profile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Open Camera to Capture Image
        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } else {
                Toast.makeText(this, "No Camera Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val base64Image = convertToBase64(imageBitmap)

            // Log base64 string to verify conversion
            Log.d("Base64Image", base64Image)

            // Send image to backend
            sendImageToBackend(base64Image)
        }
    }

    // Convert Bitmap to Base64 String
    private fun convertToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Send Base64 Image to Backend API
    private fun sendImageToBackend(base64Image: String) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("image", base64Image)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonObject.toString()
        )

        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "API call failed", Toast.LENGTH_SHORT).show()
                    Log.e("API_CALL", "Failed to send image", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let {
                            parseNutrientData(it)
                        }
                    } else {
                        Log.e("API_ERROR", "Response Code: ${response.code}")
                    }
                }
            }
        })
    }
    private fun parseNutrientData(response: String) {
        try {
            val jsonObject = JSONObject(response)

            val protein = jsonObject.getJSONObject("Protein").getDouble("value")
            val proteinUnit = jsonObject.getJSONObject("Protein").getString("unit")

            val carbohydrate = jsonObject.getJSONObject("Carbohydrate, by difference").getDouble("value")
            val carbohydrateUnit = jsonObject.getJSONObject("Carbohydrate, by difference").getString("unit")

            val fat = jsonObject.getJSONObject("Total lipid (fat)").getDouble("value")
            val fatUnit = jsonObject.getJSONObject("Total lipid (fat)").getString("unit")

            // Log the values to check if they are parsed correctly
            Log.d("Nutrient Data", "Protein: $protein $proteinUnit")
            Log.d("Nutrient Data", "Carbohydrate: $carbohydrate $carbohydrateUnit")
            Log.d("Nutrient Data", "Fat: $fat $fatUnit")

            // Show extracted values in a Toast (optional)
            runOnUiThread {
                binding.proteinQuantity.text = "$protein $proteinUnit"
                binding.carbsQuantity.text = "$carbohydrate $carbohydrateUnit"
                binding.fatQuantity.text = "$fat $fatUnit"
            }

        } catch (e: Exception) {
            Log.e("JSON_PARSE_ERROR", "Error parsing response", e)
        }
    }

}
