package codes.kevinvanzyl.showdownathighnoon.controller;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import codes.kevinvanzyl.showdownathighnoon.R;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_AppCompat);
        setContentView(R.layout.page_splash_screen);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);

                Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getApplicationContext(),
                        android.R.anim.fade_in, android.R.anim.fade_out).toBundle();

                try {
                    synchronized (this) {
                        startActivity(intent, bundle);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                SplashScreenActivity.this.finish();
                }
            }, 1100);
            }
        });
    }
}
