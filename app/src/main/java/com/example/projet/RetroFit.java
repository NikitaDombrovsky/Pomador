package com.example.projet;

import retrofit2.Retrofit;

public interface RetroFit {

    String BASEURL = "https://api.github.com";
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASEURL)
            .build();
}
