package com.development.security.ciphernote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;

public class StartupActivity extends AppCompatActivity {
    SharedPreferences prefs = null;
    Context applicationContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        try{
            prefs = getSharedPreferences("com.security.test", MODE_PRIVATE);
            applicationContext = getApplicationContext();

            SecurityManager securityManager = new SecurityManager();
            DataStructures dataStructures = new DataStructures();
            FileManager fileManager = new FileManager();



            String userPassword = "password";
            String salt = securityManager.generateSalt();

            Log.d("help", "StartupActivity salt: " + salt);

            fileManager.saveHashInfo(applicationContext, "", Base64.encodeToString(salt.getBytes(), Base64.DEFAULT), 5000);

            String saltFromFile = fileManager.getSalt(applicationContext);

            byte[] newHash = securityManager.hashPassword(userPassword, saltFromFile.getBytes());

//            for(int i=0; i<10; i++){
//                byte[] testHash = securityManager.hashPassword(userPassword, salt.getBytes());
//                Log.d("help", "Test: " + Base64.encodeToString(testHash, Base64.DEFAULT));
//            }

            Log.d("help", "StartupActivity hash: " + Base64.encodeToString(newHash, Base64.DEFAULT));

            Log.d("help", "Startup ran");

            fileManager.saveHashInfo(applicationContext, Base64.encodeToString(newHash, Base64.DEFAULT), Base64.encodeToString(salt.getBytes(), Base64.DEFAULT), 5000);

            prefs.edit().putBoolean("firstRun", false).commit();

            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}
