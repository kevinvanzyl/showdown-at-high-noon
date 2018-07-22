package codes.kevinvanzyl.showdownathighnoon.controller.multiplayer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
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

import java.util.Random;

import codes.kevinvanzyl.showdownathighnoon.R;

public class GameFragment extends Fragment {

    private static final String TAG = GameFragment.class.getSimpleName();

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
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float azimut;
    private float pitch;
    private float roll;

    SensorManager sensorManager;
    Sensor accSensor;
    Sensor magSensor;

    private int playerAScore = 0;
    private int playerBScore = 0;

    private String myParticipantId;
    private String hostParticipantId;
    private String clientParticipantId;

    private boolean imHost;

    private final Handler countdownHandler = new Handler();

    public GameFragment() {
        // Required empty public constructor
    }

    public static GameFragment newInstance(String myParticipantId, String hostParticipantId, String clientParticipantId) {
        GameFragment fragment = new GameFragment();

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

        if (imHost) {
            startCountDown();
        }
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
        if (playerAScore < 10 && playerBScore < 10) {

            nextRound();
        }
        else {
            txtCountDown.setVisibility(View.VISIBLE);
            txtCountDown.setTextColor(ContextCompat.getColor(getActivity(), R.color.blue_heading));

            if (imHost && playerAScore == 10) {
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

        Toast.makeText(getActivity(), "Round Started", Toast.LENGTH_SHORT).show();

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


            }
        }, random*1000);
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

}
