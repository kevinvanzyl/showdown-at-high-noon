package codes.kevinvanzyl.showdownathighnoon.controller.multiplayer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;

import java.util.ArrayList;
import java.util.Random;

import codes.kevinvanzyl.showdownathighnoon.R;

public class MultiplayerFragment extends Fragment implements SensorEventListener {

    private static final String TAG = MultiplayerFragment.class.getSimpleName();

    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_DOWN = 1;
    private static final int DIRECTION_LEFT = 2;
    private static final int DIRECTION_RIGHT = 3;
    private static final int DIRECTION_NONE = -1;
    private static final String MY_PARTICIPANT_ID = "MY_PARTICIPANT_ID";
    private static final String HOST_PARTICIPANT_ID = "HOST_PARTICIPANT_ID";
    private static final String CLIENT_PARTICIPANT_ID = "CLIENT_PARTICIPANT_ID";

    private TextView txtCountDown;
    private TextView txtMyScore;
    private TextView txtOpponentScore;
    private LinearLayout layoutArrow;
    private ImageView imgArrow;

    private int currentDirection = DIRECTION_NONE;

    private final float[] mMagnet = new float[3];               // magnetic field vector
    private final float[] mAcceleration = new float[3];         // accelerometer vector
    private final float[] mAccMagOrientation = new float[3];    // orientation angles from mAcceleration and mMagnet
    private float[] mRotationMatrix = new float[9];

    SensorManager sensorManager;
    Sensor accSensor;
    Sensor magSensor;

    private int roundNumber = 0;
    private int myScore = 0;
    private int opponentScore = 0;

    private String myParticipantId;
    private String hostParticipantId;
    private String clientParticipantId;

    private boolean imHost;

    private final Handler countdownHandler = new Handler();
    private Room room;

    public MultiplayerFragment() {
        // Required empty public constructor
    }

    public static MultiplayerFragment newInstance(String myParticipantId, String hostParticipantId, String clientParticipantId) {
        MultiplayerFragment fragment = new MultiplayerFragment();

        Bundle b = new Bundle();
        b.putString(MY_PARTICIPANT_ID, myParticipantId);
        b.putString(HOST_PARTICIPANT_ID, hostParticipantId);
        b.putString(CLIENT_PARTICIPANT_ID, clientParticipantId);
        fragment.setArguments(b);
        
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_game, container, false);

        txtCountDown = (TextView) v.findViewById(R.id.text_countdown);
        txtMyScore = (TextView) v.findViewById(R.id.text_my_score);
        txtOpponentScore = (TextView) v.findViewById(R.id.text_opponent_score);

        layoutArrow = (LinearLayout) v.findViewById(R.id.layout_image_arrow);
        imgArrow = (ImageView) v.findViewById(R.id.image_arrow);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        Bundle b = getArguments();

        myParticipantId = b.getString(MY_PARTICIPANT_ID);
        hostParticipantId = b.getString(HOST_PARTICIPANT_ID);
        clientParticipantId = b.getString(CLIENT_PARTICIPANT_ID);
        Log.e(TAG, "myParticipantId = "+myParticipantId);
        Log.e(TAG, "hostParticipantId = "+hostParticipantId);
        Log.e(TAG, "clientParticipantId = "+clientParticipantId);

        if (myParticipantId.equals(hostParticipantId)) {
            imHost = true;
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        if (myScore < 10 && opponentScore < 10) {

            nextRound();
        }
        else {
            txtCountDown.setVisibility(View.VISIBLE);
            txtCountDown.setTextColor(ContextCompat.getColor(getActivity(), R.color.blue_heading));

            if (imHost && myScore == 10) {
                txtCountDown.setText("Player A wins!");
            }
            else {
                txtCountDown.setText("Player B wins!");
            }

            countdownHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((MultiplayerActivity) getActivity()).finish();
                }
            }, 3000);
        }
    }

    private void nextRound() {

        roundNumber++;
        Toast.makeText(getActivity(), "Round Started", Toast.LENGTH_SHORT).show();

        if (imHost) {
            Random r = new Random();
            int randomDelay = r.nextInt(9 - 2) + 2; //random number from 2 to 8 inclusive

            final int randomDirection = r.nextInt(4);

            ((MultiplayerActivity) getActivity()).sendRoundData(roundNumber, randomDirection, randomDelay);
        }
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
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAcceleration, 0, 3);   // save datas
                calculateAccMagOrientation(event.values);                       // then calculate new orientation
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagnet, 0, 3);         // save datas
                break;
            default: break;
        }
    }

    public void calculateAccMagOrientation(float[] values) {

        if (SensorManager.getRotationMatrix(mRotationMatrix, null, mAcceleration, mMagnet))
            SensorManager.getOrientation(mRotationMatrix, mAccMagOrientation);
        else { 
            double gx, gy, gz;
            gx = mAcceleration[0] / 9.81f;
            gy = mAcceleration[1] / 9.81f;
            gz = mAcceleration[2] / 9.81f;
            // http://theccontinuum.com/2012/09/24/arduino-imu-pitch-roll-from-accelerometer/
            float pitch = (float) -Math.atan(gy / Math.sqrt(gx * gx + gz * gz));
            float roll = (float) -Math.atan(gx / Math.sqrt(gy * gy + gz * gz));
            float azimuth = 0; // Impossible to guess

            mAccMagOrientation[0] = azimuth;
            mAccMagOrientation[1] = pitch;
            mAccMagOrientation[2] = roll;
            mRotationMatrix = getRotationMatrixFromOrientation(mAccMagOrientation);
        }

        float x = values[0];
        float y = values[1];
        if (Math.abs(x) > Math.abs(y)) {
            if (x < 0) {

                if (Math.toDegrees(mAccMagOrientation[2]) >= 50) {
                    currentDirection = DIRECTION_UP;
                }
            }
            if (x > 0) {

                if (Math.toDegrees(mAccMagOrientation[2]) <= -50) {
                    currentDirection = DIRECTION_DOWN;
                }
            }
        } else {
            if (y < 0) {

                if (Math.toDegrees(mAccMagOrientation[1]) >= 50) {
                    currentDirection = DIRECTION_LEFT;
                }
            }
            if (y > 0) {

                if (Math.toDegrees(mAccMagOrientation[1]) <= -50) {
                    currentDirection = DIRECTION_RIGHT;
                }
            }
        }
        if (x > (-2) && x < (2) && y > (-2) && y < (2)) {

            currentDirection = DIRECTION_NONE;
        }
    }

    public static float[] getRotationMatrixFromOrientation(float[] o) {

        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;xM[1] = 0.0f;xM[2] = 0.0f;
        xM[3] = 0.0f;xM[4] = cosX;xM[5] = sinX;
        xM[6] = 0.0f;xM[7] =-sinX;xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;yM[1] = 0.0f;yM[2] = sinY;
        yM[3] = 0.0f;yM[4] = 1.0f;yM[5] = 0.0f;
        yM[6] =-sinY;yM[7] = 0.0f;yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;zM[1] = sinZ;zM[2] = 0.0f;
        zM[3] =-sinZ;zM[4] = cosZ;zM[5] = 0.0f;
        zM[6] = 0.0f;zM[7] = 0.0f;zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    public static float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    public void handleRoundData(final Integer randomDirection, Integer randomDelay) {

        final int arrowDrawable = getArrowDrawable(randomDirection);

        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                layoutArrow.setVisibility(View.VISIBLE);
                imgArrow.setImageResource(arrowDrawable);

                currentDirection = DIRECTION_NONE;

                new CountDownTimer(10000, 50) {

                    public void onTick(long millisUntilFinished) {
                        if (currentDirection == randomDirection) {
                            this.cancel();
                            claimWin();
                        }
                    }

                    public void onFinish() {
                    }
                }.start();
            }
        }, randomDelay*1000);
    }

    private void claimWin() {

        ((MultiplayerActivity) getActivity()).claimWin();
    }

    public void handleWin() {

        myScore++;
        txtMyScore.setText(myScore+"");
        displayWinner(myParticipantId);
    }

    public void handleLoss() {

        opponentScore++;
        txtOpponentScore.setText(opponentScore+"");
        displayWinner((imHost) ? clientParticipantId : hostParticipantId);
    }

    public void displayWinner(String participantId) {

        layoutArrow.setVisibility(View.GONE);

        txtCountDown.setVisibility(View.VISIBLE);
        txtCountDown.setTextColor(ContextCompat.getColor(getActivity(), R.color.blue_heading));

        Participant winner = room.getParticipant(participantId);

        txtCountDown.setText(winner.getDisplayName()+" Won!");
        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                txtCountDown.setVisibility(View.GONE);
                playGame();
            }
        }, 2000);
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
