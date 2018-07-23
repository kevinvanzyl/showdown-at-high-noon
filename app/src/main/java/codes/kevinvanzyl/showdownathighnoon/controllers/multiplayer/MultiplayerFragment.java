package codes.kevinvanzyl.showdownathighnoon.controllers.multiplayer;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Dimension;
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

import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;

import org.w3c.dom.Text;

import java.util.Random;

import codes.kevinvanzyl.showdownathighnoon.R;
import codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor;

import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_DOWN;
import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_LEFT;
import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_NONE;
import static codes.kevinvanzyl.showdownathighnoon.sensors.TiltSensor.DIRECTION_UP;

public class MultiplayerFragment extends Fragment {

    private static final String TAG = MultiplayerFragment.class.getSimpleName();


    private static final String MY_PARTICIPANT_ID = "MY_PARTICIPANT_ID";
    private static final String HOST_PARTICIPANT_ID = "HOST_PARTICIPANT_ID";
    private static final String CLIENT_PARTICIPANT_ID = "CLIENT_PARTICIPANT_ID";
    private static final long TIME_FORFEIT = 10000;

    private TextView txtCountDown;

    private TextView txtMyScore;
    private TextView txtOpponentScore;
    private TextView txtMyName;
    private TextView txtOpponentName;
    private ImageView imgMyProfile;
    private ImageView imgOpponentProfile;

    private LinearLayout layoutArrow;
    private ImageView imgArrow;

    private TiltSensor tiltSensor;

    private int roundNumber = 0;
    private int myScore = 0;
    private int opponentScore = 0;

    private String myParticipantId;
    private String hostParticipantId;
    private String clientParticipantId;

    private boolean imHost;

    private final Handler countdownHandler = new Handler();
    private Room room;

    private CountDownTimer singleCountDownTimer;
    private Runnable showArrowRunnable;

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

        tiltSensor = new TiltSensor(getActivity());

        txtCountDown = (TextView) v.findViewById(R.id.text_countdown);

        txtMyScore = (TextView) v.findViewById(R.id.text_my_score);
        txtOpponentScore = (TextView) v.findViewById(R.id.text_opponent_score);
        txtMyName = (TextView) v.findViewById(R.id.text_my_name);
        txtOpponentName = (TextView) v.findViewById(R.id.text_opponent_name);
        imgMyProfile = (ImageView) v.findViewById(R.id.img_my_profile);
        imgOpponentProfile = (ImageView) v.findViewById(R.id.img_opponent_profile);

        layoutArrow = (LinearLayout) v.findViewById(R.id.layout_image_arrow);
        imgArrow = (ImageView) v.findViewById(R.id.image_arrow);

        Bundle b = getArguments();

        myParticipantId = b.getString(MY_PARTICIPANT_ID);
        hostParticipantId = b.getString(HOST_PARTICIPANT_ID);
        clientParticipantId = b.getString(CLIENT_PARTICIPANT_ID);
        Log.e(TAG, "myParticipantId = "+myParticipantId);
        Log.e(TAG, "hostParticipantId = "+hostParticipantId);
        Log.e(TAG, "clientParticipantId = "+clientParticipantId);

        Participant me = room.getParticipant(myParticipantId);
        Participant opponent = room.getParticipant( (myParticipantId.equals(hostParticipantId)) ? clientParticipantId : hostParticipantId);

        txtMyName.setText(me.getDisplayName());
        txtOpponentName.setText(opponent.getDisplayName());

        ImageManager manager = ImageManager.create(getActivity());
        manager.loadImage(imgMyProfile, me.getIconImageUri());
        manager.loadImage(imgOpponentProfile, opponent.getIconImageUri());

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
                Log.d("HELLOHELLO", millisUntilFinished+"");
                if (millisUntilFinished > 1000) {
                    txtCountDown.setText("" + millisUntilFinished / 1000);
                }
            }

            public void onFinish() {
                txtCountDown.setVisibility(View.GONE);
                txtCountDown.setTextSize(Dimension.SP, 38f);
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

            Participant participant = room.getParticipant( (imHost && myScore == 10) ? hostParticipantId : clientParticipantId);
            txtCountDown.setText(participant.getDisplayName()+" wins!");

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
        tiltSensor.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        tiltSensor.pause();
    }

    public void handleRoundData(final Integer randomDirection, final Integer randomDelay) {

        final int arrowDrawable = getArrowDrawable(randomDirection);
        tiltSensor.setCurrentDirection(DIRECTION_NONE);

        showArrowRunnable = new Runnable() {
            @Override
            public void run() {
                layoutArrow.setVisibility(View.VISIBLE);
                imgArrow.setImageResource(arrowDrawable);
            }
        };

        countdownHandler.postDelayed(showArrowRunnable, randomDelay*1000);

        final long totalMillis = TIME_FORFEIT+(randomDelay*1000);
        singleCountDownTimer = new CountDownTimer(totalMillis, 200) {

            public void onTick(long millisUntilFinished) {

                long passedTime = totalMillis - millisUntilFinished;
                //Check for premature tilt action
                if (passedTime < (randomDelay*1000)) {
                    if (tiltSensor.getCurrentDirection() != DIRECTION_NONE) {
                        this.cancel();
                        claimLoss();
                    }
                }
                else {
                    if (tiltSensor.getCurrentDirection() == randomDirection) {
                        this.cancel();
                        claimWin();
                    }
                }
            }

            public void onFinish() {
                bothLose();
            }
        }.start();
    }

    private void bothLose() {

        singleCountDownTimer.cancel();
        layoutArrow.setVisibility(View.GONE);

        txtCountDown.setVisibility(View.VISIBLE);
        txtCountDown.setTextColor(ContextCompat.getColor(getActivity(), R.color.blue_heading));

        txtCountDown.setText("Both lose!");
        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                txtCountDown.setVisibility(View.GONE);
                playGame();
            }
        }, 2000);
    }

    private void claimWin() {

        ((MultiplayerActivity) getActivity()).claimWin();
    }

    private void claimLoss() {

        ((MultiplayerActivity) getActivity()).claimLoss();
    }

    public void handleWin() {

        singleCountDownTimer.cancel();
        countdownHandler.removeCallbacks(showArrowRunnable);
        myScore++;
        txtMyScore.setText(myScore+"");
        displayWinner((imHost) ? hostParticipantId : clientParticipantId);
    }

    public void handleLoss() {

        singleCountDownTimer.cancel();
        countdownHandler.removeCallbacks(showArrowRunnable);
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
