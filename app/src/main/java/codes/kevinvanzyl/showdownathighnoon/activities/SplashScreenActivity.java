package codes.kevinvanzyl.showdownathighnoon.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;

import codes.kevinvanzyl.showdownathighnoon.R;

public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_AppCompat_Light_NoActionBar_FullScreen);
        setContentView(R.layout.page_splash_screen);

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
