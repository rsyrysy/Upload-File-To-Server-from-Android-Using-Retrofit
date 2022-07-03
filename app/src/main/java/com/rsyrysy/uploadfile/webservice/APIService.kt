package com.rsyrysy.uploadfile.webservice

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface APIService {
    // Form Data
    @Multipart
    @POST("Login")
    suspend fun validateLogin(@PartMap map: HashMap<String?, RequestBody?>): Response<ResponseBody>

    // Form Data
    @Multipart
    @POST("Upload")
    suspend fun uploadEmployeeData(@PartMap map: HashMap<String?, RequestBody?>): Response<ResponseBody>

    @Multipart
    @POST("Upload")
    suspend fun uploadEmployeeDataMultiple(@PartMap map: MutableList<MultipartBody.Part>?): Response<ResponseBody>

    // Form Data
    @Multipart
    @POST("download")
    suspend fun downloadData(@PartMap map: HashMap<String?, RequestBody?>): Response<ResponseBody>

    // Form Data
    @Multipart
    @POST("DeleteRange")
    suspend fun deleteRangeData(@PartMap map: HashMap<String?, RequestBody?>): Response<ResponseBody>

    /*****************************************************************************************************************************************************/
}