package com.rsyrysy.uploadfile.ui.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.Nullable
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.rsyrysy.uploadfile.BuildConfig
import com.rsyrysy.uploadfile.R
import com.rsyrysy.uploadfile.databinding.ActivityMainBinding
import com.rsyrysy.uploadfile.utils.CommonUtils
import com.rsyrysy.uploadfile.utils.FileUtils.getFilePathFromURI
import com.rsyrysy.uploadfile.utils.ProgressDialogrsyrysy
import com.rsyrysy.uploadfile.webservice.APIService
import com.rsyrysy.uploadfile.webservice.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val PICK_FILE = 1001

        const val REQUESTcode = 102222
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (!CommonUtils.checkPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            CommonUtils.requestPermission(
                this@MainActivity, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_PHONE_STATE
                ), REQUESTcode
            )
        } else {

        }

        binding.fab.setOnClickListener { view ->
            //upload  file   to server  call
            val intent = Intent()
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select file to upload"),
                PICK_FILE
            )

        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    /////
    // Get the result from this Overriden method
    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 ->
                    // Checking whether data is null or not
                    if (data != null) {

                        // Checking for selection multiple files or single.
                        if (data.clipData != null) {
                            // Getting the length of data and logging up the logs using index
                            var index = 0
                            var pathList: MutableList<String> = mutableListOf()
                            while (index < data.clipData!!.itemCount) {
                                // Getting the URIs of the selected files and logging them into logcat at debug level
                                val uri = data.clipData!!.getItemAt(index).uri
                                Log.d("filesUri [$uri] : ", uri.toString())
                                index++
                                Log.d("fileUri: ", uri?.path.toString())
                                val path: String? = getFilePathFromURI(this@MainActivity, uri!!)
                                pathList.add(path.toString())
                            }
                            Log.d("SIZE: ", "" + pathList.size)
                            formDatauploadmultiple(pathList)

                        } else {

                            // Getting the URI of the selected file and logging into logcat at debug level
                            val uri = data.data
                            Log.d("fileUri: ", uri?.path.toString())
                            Log.d("fileUri: ", uri.toString())
                            val path: String? = getFilePathFromURI(this@MainActivity, uri!!)
                            formDataupload(path)
                        }
                    }
            }
        }
    }


    ///single upload
    private fun formDataupload(path: String?) {
        var pDialog: ProgressDialogrsyrysy? = null
        pDialog = ProgressDialogrsyrysy(this@MainActivity, R.style.progressDialogTheme)
        pDialog!!.setCancelable(false)
        pDialog!!.setCanceledOnTouchOutside(false)
        pDialog!!.show()
        // Create Retrofit
        val retrofit = AppConfig.getRetrofit()
        // Create Service
        val service = retrofit.create(APIService::class.java)
        // Get file from path
        val file = File(path)
        val fields: HashMap<String?, RequestBody?> = HashMap()
        fields["filemod\"; filename=\"${file.name}\" "] =
            (file).asRequestBody("text/plain".toMediaTypeOrNull())
        fields["Userid"] = ("userid").toRequestBody("text/plain".toMediaTypeOrNull())
        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            val response = service.uploadEmployeeData(fields)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {

                    // Convert raw JSON to pretty JSON using GSON library
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val prettyJson = gson.toJson(
                        JsonParser.parseString(
                            response.body()
                                ?.string()
                        )
                    )
                    Log.d("Pretty Printed JSON :", prettyJson)

                    CommonUtils.showToastMessage(this@MainActivity, prettyJson + "")

                    pDialog!!.dismiss()

                } else {
                    Log.e("RETROFIT_ERROR", response.code().toString())
                    CommonUtils.showToastMessage(this@MainActivity, response.code().toString() + "")


                    pDialog!!.dismiss()

                }
            }
        }

    }

    ///  multiple  file  upload
    private fun formDatauploadmultiple(path: MutableList<String>?) {
        // Get file list
        var path1 = path
        Log.d("SIZE: ", "filelist " + path1!!.size)
        var filesUploadlist: MutableList<MultipartBody.Part> = mutableListOf()

        for (i in 0..path!!.size - 1) {
            var pDialog: ProgressDialogrsyrysy? = null
            pDialog = ProgressDialogrsyrysy(this@MainActivity, R.style.progressDialogTheme)
            pDialog!!.setCancelable(false)
            pDialog!!.setCanceledOnTouchOutside(false)
            pDialog!!.show()
            // Create Retrofit
            val retrofit = AppConfig.getRetrofit()
            // Create Service
            val service = retrofit.create(APIService::class.java)
            // Get file from path
            val file = File(path.get(i).toString())
            val fields: HashMap<String?, RequestBody?> = HashMap()
            fields["filemod\"; filename=\"${file.name}\" "] =
                (file).asRequestBody("text/plain".toMediaTypeOrNull())
            fields["Userid"] = ("userid").toRequestBody("text/plain".toMediaTypeOrNull())
            CoroutineScope(Dispatchers.IO).launch {
                // Do the POST request and get response
                val response = service.uploadEmployeeData(fields)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {

                        // Convert raw JSON to pretty JSON using GSON library
                        val gson = GsonBuilder().setPrettyPrinting().create()
                        val prettyJson = gson.toJson(
                            JsonParser.parseString(
                                response.body()
                                    ?.string() // About this thread blocking annotation : https://github.com/square/retrofit/issues/3255
                            )
                        )
                        Log.d("Pretty Printed JSON :", prettyJson)
                        CommonUtils.showToastMessage(this@MainActivity, prettyJson + "")
                        pDialog!!.dismiss()
                    } else {
                        Log.e("RETROFIT_ERROR", response.code().toString())
                        CommonUtils.showToastMessage(
                            this@MainActivity,
                            response.code().toString() + ""
                        )
                        pDialog!!.dismiss()
                    }
                }
            }
        }


    }

}