/*
 * My Digital Safe, the secure notepad Android app.
 * Copyright (C) 2018 Security First Designs
 *
 * My Digital Safe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <a href="www.gnu.org/licenses/">here</a>.
 */

package com.securityfirstdesigns.mydigitalsafe.app.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.securityfirstdesigns.mydigitalsafe.app.R;
import com.securityfirstdesigns.mydigitalsafe.app.model.DatabaseManager;
import com.securityfirstdesigns.mydigitalsafe.app.model.QuickNoteFile;
import com.securityfirstdesigns.mydigitalsafe.app.model.UserConfiguration;
import com.securityfirstdesigns.mydigitalsafe.app.security.LoginActivity;
import com.securityfirstdesigns.mydigitalsafe.app.security.SecurityService;

public class StartupActivity extends AppCompatActivity {
    Context applicationContext;
    WebView browser;
    final SecurityService securityService = SecurityService.getInstance();

    private boolean creatingPasswordFlag = false;

    String firstPassword = null;
    String secondPassword = null;

    int lastScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        applicationContext = getApplicationContext();
        browser=(WebView)findViewById(R.id.webkit);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.addJavascriptInterface(new StartupActivity.WebAppInterface(this), "Android");
        browser.loadUrl("file:///android_asset/StartupPage.html");

        SharedPreferences prefs = getSharedPreferences("com.mycompany.myAppName", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            // Do first run stuff here then set 'firstrun' as false
            // using the following line to edit/commit prefs
            prefs.edit().putBoolean("firstrun", false).commit();
            try{
                QuickNoteFile quickNoteFile = new QuickNoteFile();
                quickNoteFile.setQuickNoteFileName("Welcome to My Digital Safe!");
                quickNoteFile.setQuickNoteData("Welcome to the most customizable, easy to use secure notepad app on the market!\n" +
                        "\n" +
                        "My Digital Safe commits to striking the perfect balance between securing your data and allowing you to customize your own experience to suit your needs.\n" +
                        "\n" +
                        "We recommend you visit the Setting's page to configure the app to your likings. By default, the app does not have a Forget Password method in place. By visiting the Settings page, you can enable the password reset feature and configure as few as one and as many as five security questions. My Digital Safe will require the correct response of all security questions to allow you to reset your password.\n" +
                        "\n" +
                        "You can also configure your Digital Safe to lock itself after too many failed login attempts. If you are concerned about friends, family or other people trying to guess your My Digital Safe password, this feature can help protect you.\n" +
                        "\n" +
                        "Lastly, don't forget to check out the \"Quick Note\" feature on the login page. My Digital Safe understands it can be painful to have to login everytime you want to write down a quick thought. To solve this problem, we allow you to create a note from your login page that will be kept hidden once saved and encrypted the next time you login.\n" +
                        "\n" +
                        "Enjoy the app!\n" +
                        "- My Digital Safe team.");
                DatabaseManager databaseManager = new DatabaseManager(applicationContext);
                databaseManager.addQuickNoteFile(quickNoteFile);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        if (savedInstanceState != null){
            creatingPasswordFlag = savedInstanceState.getBoolean("inAsyncFlag", false);
        }

    }


    private class AsyncAccountCreation extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                if (firstPassword.equals(secondPassword) && lastScore > 3) {
                    long start_time = System.nanoTime();
                    int iterations = 120000;

                    String salt = securityService.generateSalt();
                    UserConfiguration userConfiguration = new UserConfiguration();
                    userConfiguration.setPassword_hash("");
                    userConfiguration.setSalt(salt);
                    userConfiguration.setIterations(iterations);
                    DatabaseManager databaseManager = new DatabaseManager(applicationContext);
                    databaseManager.addUserConfiguration(userConfiguration);
                    String saltFromFile = databaseManager.getUserConfiguration().getSalt();

                    byte[] newHash = securityService.hashPassword(firstPassword, saltFromFile.getBytes(), iterations);
                    databaseManager.checkConfigDirectory(applicationContext);
                    databaseManager.writeToDataFile(applicationContext, "started".getBytes(), "startup", true);

                    UserConfiguration currentUserConfig = databaseManager.getUserConfiguration();
                    String hash = Base64.encodeToString(newHash, Base64.DEFAULT);
                    currentUserConfig.setPassword_hash(hash);
                    databaseManager.addUserConfiguration(currentUserConfig);
                    long end_time = System.nanoTime();
                    double difference = (end_time - start_time) / 1e6;
                    int loginTime = (int) difference;
                    writeLoginTime(loginTime);

                    return true;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return false;

        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Boolean status) {
            if(status){
                boolean authenticate = false;
                try{
                    authenticate = securityService.authenticateUser(firstPassword, applicationContext);
                    securityService.generateKey(applicationContext);
                }catch (Exception e){
                    e.printStackTrace();
                }
                if(authenticate){
                    Intent landingIntent = new Intent(applicationContext, HomeActivity.class);
                    startActivity(landingIntent);
                    finish();
                }else{
                    Intent loginIntent = new Intent(applicationContext, LoginActivity.class);
                    startActivity(loginIntent);
                    finish();
                }
            }else{
                if(firstPassword.equals(secondPassword)){
                    CharSequence failedAuthenticationString = getString(R.string.passwordTooShort);

                    Toast toast = Toast.makeText(applicationContext, failedAuthenticationString, Toast.LENGTH_LONG);
                    toast.show();
                }else{
                    CharSequence failedAuthenticationString = getString(R.string.passwordsDoNotMatch);

                    Toast toast = Toast.makeText(applicationContext, failedAuthenticationString, Toast.LENGTH_LONG);
                    toast.show();
                }
            }
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearFields()");
                }
            });

            creatingPasswordFlag = false;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle bundle){
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("inAsyncFlag", creatingPasswordFlag);
    }

    protected void androidCreatePassword(String passwordOne, String passwordTwo){
       firstPassword = passwordOne;
       secondPassword = passwordTwo;

        new StartupActivity.AsyncAccountCreation().execute("");
    }

    private int androidCheckPasswordStrength(String password){
        password = password.trim();

        //total score of password
        int iPasswordScore = 0;

        if (password.length() < 6) {
            return -1;
        }else{
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearLength()");
                }
            });
        }
        if (password.length() >= 10) {
            iPasswordScore += 2;
        }

        //if it contains one digit, add 2 to total score
        if (password.matches("(?=.*[0-9]).*")){
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearBadNumber()");
                }
            });
            iPasswordScore += 2;
        }

        //if it contains one lower case letter, add 2 to total score
        if (password.matches("(?=.*[a-z]).*")) {
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearLowerCase()");
                }
            });
            iPasswordScore += 2;
        }
        //if it contains one upper case letter, add 2 to total score
        if (password.matches("(?=.*[A-Z]).*")) {
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearUpperCase()");
                }
            });
            iPasswordScore += 2;
        }

        //if it contains one special character, add 2 to total score
        if (password.matches("(?=.*[~!@#$%^&*()_-]).*")) {
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearBadSymbol()");
                }
            });
            iPasswordScore += 2;
        }

        if (iPasswordScore < 3) {
            iPasswordScore = -2;
        }

        lastScore = iPasswordScore;
        return iPasswordScore;
    }

       private void writeLoginTime(int time){
        SharedPreferences sp = getSharedPreferences("digital_safe", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("login_time", time);
        editor.commit();
    }


    public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) {
            mContext = c;
        }
        @JavascriptInterface
        public void createPassword(String passwordOne, String passwordTwo) {
            creatingPasswordFlag = true;
            androidCreatePassword(passwordOne, passwordTwo);
        }

        @JavascriptInterface
        public int checkPasswordStrength(String password){
            return androidCheckPasswordStrength(password);
        }

        @JavascriptInterface
        public void goToPrivacyPolicy() {
            Intent privacyPolicyIntent = new Intent(applicationContext, PrivacyPolicyActivity.class);
            startActivity(privacyPolicyIntent);
        }

        @JavascriptInterface
        public boolean checkForAsync(){
            return creatingPasswordFlag;
        }

    }
}
