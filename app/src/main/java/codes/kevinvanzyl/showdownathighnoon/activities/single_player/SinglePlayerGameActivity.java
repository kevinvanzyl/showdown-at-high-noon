package codes.kevinvanzyl.showdownathighnoon.activities.single_player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Random;

import codes.kevinvanzyl.showdownathighnoon.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SinglePlayerGameActivity extends AppCompatActivity implements SensorEventListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_DOWN = 1;
    private static final int DIRECTION_LEFT = 2;
    private static final int DIRECTION_RIGHT = 3;
    private static final int DIRECTION_NONE = -1;
    private final Handler mHideHandler = new Handler();
    private final Handler countdownHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private TextView txtCountDown;
    private TextView txtRoundNumber;
    private TextView txtScore;
    private LinearLayout layoutArrow;
    private ImageView imgArrow;

    private int roundNumber = 0;
    private int currentDirection = DIRECTION_NONE;
    private int score = 0;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float azimut;
    private float pitch;
    private float roll;

    SensorManager sensorManager;
    Sensor accSensor;
    Sensor magSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_single_player_game);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);

        txtCountDown = (TextView) findViewById(R.id.text_countdown);
        txtRoundNumber = (TextView) findViewById(R.id.text_round_number);
        txtScore = (TextView) findViewById(R.id.text_score);

        layoutArrow = (LinearLayout) findViewById(R.id.layout_image_arrow);
        imgArrow = (ImageView) findViewById(R.id.image_arrow);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        startCountDown();
    }

    private void startCountDown() {

        new CountDownTimer(4000, 1000) {

            public void onTick(long millisUntilFinished) {
                txtCountDown.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                txtCountDown.setVisibility(View.GONE);
                playGame();
            }
        }.start();
    }

    private void playGame() {
        if (roundNumber < 10) {

            nextRound();
        }
        else {
            txtCountDown.setVisibility(View.VISIBLE);
            txtCountDown.setTextColor(ContextCompat.getColor(this, R.color.blue_heading));
            txtCountDown.setText("Your final score is "+score);

            countdownHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
        }
    }

    private void nextRound() {

        Toast.makeText(this, "Round Started", Toast.LENGTH_SHORT).show();

        roundNumber++;
        txtRoundNumber.setText(""+roundNumber);

        Random r = new Random();
        int random = r.nextInt(9 - 2) + 2; //random number from 2 to 8 inclusive

        final int randomDirection = r.nextInt(4);
        final int arrowDrawable = getArrowDrawable(randomDirection);

        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                layoutArrow.setVisibility(View.VISIBLE);
                imgArrow.setImageResource(arrowDrawable);


                currentDirection = DIRECTION_NONE;

                new CountDownTimer(1000, 50) {

                    public void onTick(long millisUntilFinished) {
                        if (currentDirection == randomDirection) {
                            this.cancel();
                            roundWon();
                        }
                    }

                    public void onFinish() {

                        roundLost();
                    }
                }.start();
            }
        }, random*1000);
    }

    private void roundLost() {

        layoutArrow.setVisibility(View.GONE);
        imgArrow.setImageDrawable(null);

        txtCountDown.setVisibility(View.VISIBLE);
        txtCountDown.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        txtCountDown.setText("You Lost!");
        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                txtCountDown.setVisibility(View.GONE);
                playGame();
            }
        }, 2000);
    }

    private void roundWon() {

        score++;
        txtScore.setText(score+"");

        layoutArrow.setVisibility(View.GONE);
        imgArrow.setImageDrawable(null);

        txtCountDown.setVisibility(View.VISIBLE);
        txtCountDown.setTextColor(ContextCompat.getColor(this, R.color.blue_heading));
        txtCountDown.setText("You Won!");
        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                txtCountDown.setVisibility(View.GONE);
                playGame();
            }
        }, 2000);
    }

    public int getArrowDrawable(int direction) {

        if (direction == DIRECTION_UP) {
            return R.drawable.ic_arrow_upward_accent_24dp;
        }
        else if (direction == DIRECTION_DOWN) {
            return R.drawable.ic_arrow_downward_accent_24dp;
        }
        else if (direction == DIRECTION_LEFT) {
            return R.drawable.ic_arrow_back_accent_24dp;
        }
        else {
            return R.drawable.ic_arrow_forward_accent_24dp;
        }
    }

    public String getDirectionString(int direction) {

        if (direction == DIRECTION_UP) {
            return "Up";
        }
        else if (direction == DIRECTION_DOWN) {
            return "Down";
        }
        else if (direction == DIRECTION_LEFT) {
            return "Left";
        }
        else if (direction == DIRECTION_RIGHT) {
            return "Right";
        }
        else {
            return "None";
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                pitch = orientation[1];
                roll = orientation[2];
            }
        }

        float x = event.values[0];
        float y = event.values[1];
        if (Math.abs(x) > Math.abs(y)) {
            if (x < 0) {

                if (Math.toDegrees(roll) >= 50) {
                    currentDirection = DIRECTION_UP;
                }
            }
            if (x > 0) {

                if (Math.toDegrees(roll) <= -50) {
                    currentDirection = DIRECTION_DOWN;
                }
            }
        } else {
            if (y < 0) {

                if (Math.toDegrees(pitch) >= 50) {
                    currentDirection = DIRECTION_LEFT;
                }
            }
            if (y > 0) {

                if (Math.toDegrees(pitch) <= -50) {
                    currentDirection = DIRECTION_RIGHT;
                }
            }
        }
        if (x > (-2) && x < (2) && y > (-2) && y < (2)) {

            currentDirection = DIRECTION_NONE;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(SinglePlayerGameActivity.this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(SinglePlayerGameActivity.this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
