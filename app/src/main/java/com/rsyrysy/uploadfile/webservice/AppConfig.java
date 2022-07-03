package com.rsyrysy.uploadfile.webservice;


import androidx.viewbinding.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class AppConfig {
    public static Retrofit getRetrofit() {

        ///webservice mail url
        final String mainurl = "Yourwebserviceurl";
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);

        }
        return new Retrofit.Builder().baseUrl(mainurl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
    }
}
