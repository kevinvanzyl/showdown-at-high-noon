package codes.kevinvanzyl.showdownathighnoon.controllers.single_player;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import codes.kevinvanzyl.showdownathighnoon.R;
import codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor;

import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_DOWN;
import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_LEFT;
import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_UP;

public class SinglePlayerGameActivity extends AppCompatActivity {

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
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

    private TextView txtCountDown;
    private TextView txtRoundNumber;
    private TextView txtScore;
    private LinearLayout layoutArrow;
    private ImageView imgArrow;

    private int roundNumber = 0;
    private int score = 0;
    private boolean gameInProgress;

    private TiltSensor tiltSensor;

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

        tiltSensor = new TiltSensor(this);

        gameInProgress = true;
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
        if (roundNumber < 10 && gameInProgress) {

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


                tiltSensor.setCurrentDirection(TiltSensor.DIRECTION_NONE);

                new CountDownTimer(1000, 50) {

                    public void onTick(long millisUntilFinished) {
                        if (tiltSensor.getCurrentDirection() == randomDirection) {
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        hide();
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

    @Override
    protected void onResume() {
        super.onResume();
        tiltSensor.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameInProgress = false;
        tiltSensor.pause();
    }
}
