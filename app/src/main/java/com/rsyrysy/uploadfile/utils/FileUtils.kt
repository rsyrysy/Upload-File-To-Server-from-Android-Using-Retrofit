package com.rsyrysy.uploadfile.utils

import android.app.ActivityManager
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract.*
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.viewbinding.BuildConfig.DEBUG

import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

object FileUtils {
    const val DOCUMENTS_DIR = "documents"

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author
     */
    @JvmStatic
    fun getPath(context: Context, uri: Uri): String? {

//        final boolean isKitKat = ;

        // DocumentProvider
        if (isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            return if (isExternalStorageDocument(uri)) {
                val docId: String = getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var filePath = ""
                if ("primary".equals(type, ignoreCase = true)) {
                    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                } else {
                    if (Build.VERSION.SDK_INT > 20) {
                        //getExternalMediaDirs() added in API 21
                        val extenal = context.externalMediaDirs
                        if (extenal.size > 1) {
                            filePath = extenal[1].absolutePath
                            filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1]
                        }
                    } else {
                        filePath = "/storage/" + type + "/" + split[1]
                    }
                    filePath
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id: String = getDocumentId(uri)
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4)
                }
                val contentUriPrefixesToTry =
                    arrayOf( //                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                    )
                for (contentUriPrefix in contentUriPrefixesToTry) {
                    val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse(contentUriPrefix),
                        java.lang.Long.valueOf(id)
                    )
                    try {
                        val path = getDataColumn(context, contentUri, null, null)
                        if (path != null) {
                            return path
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                }
                destinationPath
            } else if (isGoogleDocument(uri)) {
                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                }
                destinationPath
                //                String fileName = getFileName(context, uri);
            } else if (isMediaDocument(uri)) {
                val docId: String = getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                getDataColumn(context, contentUri, selection, selectionArgs)
            } else {
                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                }
                destinationPath
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            Log.e("fileutils", "+++ No DOCUMENT URI :: CONTENT ")
            Log.e("fileutils", "+++ No DOCUMENT URI :: CONTENT " + uri.authority)

            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                Log.e("fileutils", "+++ No DOCUMENT URI :: CONTENadT " + uri.lastPathSegment)
                return uri.lastPathSegment
            } else if (isNewGooglePhotosUri(uri)) {
                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                    return destinationPath
                }
            } else if (uri.authority != null && uri.authority == "com.whatsapp.provider.media") {
                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                    return destinationPath
                }
            }
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }
    //
    //catch (Exception e) {
    //        return getFilePathFromURI(context, uri);
    //    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    fun isGoogleDocument(uri: Uri): Boolean {
        return "com.google.android.apps.docs.storage" == uri.authority
    }

    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun isNewGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.contentprovider" == uri.authority
    }

    fun getFileName(context: Context?, uri: Uri): String? {
        val mimeType = context!!.contentResolver.getType(uri)
        var filename: String? = null
        if (mimeType == null && context != null) {
            val path = getPath(context, uri)
            filename = if (path == null) {
                getName(uri.toString())
            } else {
                val file = File(path)
                file.name
            }
        } else {
            val returnCursor = context.contentResolver.query(
                uri, null, null, null,
                null
            )
            if (returnCursor != null) {
                val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                returnCursor.moveToFirst()
                filename = returnCursor.getString(nameIndex)
                returnCursor.close()
            }
        }
        return filename
    }

    fun getName(filename: String?): String? {
        if (filename == null) {
            return null
        }
        val index = filename.lastIndexOf('/')
        return filename.substring(index + 1)
    }

    fun getDocumentCacheDir(context: Context): File {
        val dir = File(context.cacheDir, DOCUMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        logDir(context.cacheDir)
        logDir(dir)
        return dir
    }

    private fun logDir(dir: File) {
        if (!DEBUG) return
        Log.d("Dashboard", "Dir=$dir")
        val files = dir.listFiles()
        for (file in files) {
            Log.d("Dashboard", "File=" + file.path)
        }
    }

    fun generateFileName(name: String?, directory: File): File? {
        var name = name ?: return null
        var file = File(directory, name)
        if (file.exists()) {
            var fileName = name
            var extension = ""
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex)
                extension = name.substring(dotIndex)
            }
            var index = 0
            while (file.exists()) {
                index++
                name = "$fileName($index)$extension"
                file = File(directory, name)
            }
        }
        try {
            if (!file.createNewFile()) {
                return null
            }
        } catch (e: IOException) {
            Log.w("Dashboard", e)
            return null
        }
        logDir(directory)
        return file
    }

    private fun saveFileFromUri(context: Context, uri: Uri, destinationPath: String?) {
        var `is`: InputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            `is` = context.contentResolver.openInputStream(uri)
            bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
            val buf = ByteArray(1024)
            `is`!!.read(buf)
            do {
                bos.write(buf)
            } while (`is`.read(buf) != -1)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
                bos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getRealPathFromURIPath(contentURI: Uri, activity: Context): String? {
        return try {
            val cursor = activity.contentResolver.query(contentURI, null, null, null, null)
            if (cursor == null) {
                contentURI.path
            } else {
                cursor.moveToFirst()
                val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                cursor.getString(idx)
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getImageUri(inContext: Context, inImage: Bitmap?): Uri {
        val OutImage: Bitmap? = inImage?.let { Bitmap.createScaledBitmap(it, 1000, 1000, true) }
        val path: String =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, OutImage, "Title", null)
        return Uri.parse(path)
    }

    fun getFilePathFromURI(context: Context, contentUri: Uri): String? {
        val fileName = getFileName(context, contentUri)
        val cacheDir = getDocumentCacheDir(context)
        val file = generateFileName(fileName, cacheDir)
        var destinationPath: String? = null
        if (file != null) {
            destinationPath = file.absolutePath
            saveFileFromUri(context, contentUri, destinationPath)
        }
        return destinationPath
    }

    fun isConnected(context: Context): Boolean {
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.getActiveNetworkInfo()
        return activeNetwork != null && activeNetwork.isConnected()
    }

    // ping the google server to check if internet is really working or not
    val isInternetWorking: Boolean
        get() {
            var success = false
            try {
                val url = URL("https://google.com")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.connect()
                success = connection.responseCode == 200
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return success
        }

    fun isMyServiceRunning(con: Context, serviceClass: Class<*>): Boolean {
        val manager: ActivityManager =
            con.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.getClassName()) {
                return true
            }
        }
        return false
    }
}