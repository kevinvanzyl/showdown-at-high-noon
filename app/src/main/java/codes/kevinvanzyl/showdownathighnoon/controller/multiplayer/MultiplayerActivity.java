package codes.kevinvanzyl.showdownathighnoon.controller.multiplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import codes.kevinvanzyl.showdownathighnoon.R;

import static codes.kevinvanzyl.showdownathighnoon.controller.MainActivity.KEY_HOST_DETERMINATION;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MultiplayerActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String MESSAGE_AGREE_ON_HOST = "AGREE_ON_HOST";
    private static final String MESSAGE_ROUND_DATA = "ROUND_DATA";
    private MultiplayerWaitingRoomFragment waitingRoomFragment;
    private GameFragment gameFragment;

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

    private static final int RC_SIGN_IN = 9001;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final String TAG = MultiplayerActivity.class.getSimpleName();
    private final Handler mHideHandler = new Handler();
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

    private final Handler countdownHandler = new Handler();

    private ProgressBar progressIndicator;
    private RoomConfig joinedRoomConfig;
    private Room mRoom;
    boolean mPlaying = false;
    final static int MIN_PLAYERS = 2;

    private String mMyPlayerId;
    private String mMyParticipantId;
    private String hostParticipantId;
    private String clientParticipantId;

    private long myButtonClickedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        myButtonClickedTime = getIntent().getExtras().getLong(KEY_HOST_DETERMINATION);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);

        waitingRoomFragment = MultiplayerWaitingRoomFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.fullscreen_content, waitingRoomFragment).commit();

        progressIndicator = (ProgressBar)findViewById(R.id.progressBar);
        hideProgressIndicator();

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
    }

    public void silentSignIn() {
        final GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder()
                        .requestScopes(Games.SCOPE_GAMES)
                        .requestProfile()
                        .build()
        );

        Task<GoogleSignInAccount> task = googleSignInClient.silentSignIn();
        if (task.isSuccessful()) {
            // There's immediate result available.
            GoogleSignInAccount signInAccount = task.getResult();
            waitingRoomFragment.initSignIn(signInAccount);
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            showProgressIndicator();
            task.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(Task task) {
                    try {
                        hideProgressIndicator();
                        GoogleSignInAccount signInAccount = (GoogleSignInAccount) task.getResult(ApiException.class);
                        waitingRoomFragment.initSignIn(signInAccount);

                    } catch (ApiException apiException) {
                        String msg = "";
                        switch (apiException.getStatusCode()) {
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                msg = "Sign in required";
                                break;
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                msg = "Sign in cancelled";
                                break;
                            case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                                msg = "Sign in currently in progress";
                                break;
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                msg = "Sign in failed";
                                break;
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                msg = "Network error";
                                break;
                            default:
                                msg = "Unnown error";
                        }

                        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                waitingRoomFragment.initSignIn(task.getResult());
            }
            catch (RuntimeException runtimeException) {
                runtimeException.printStackTrace();
            }
        }
    }

    private void hideProgressIndicator() {
        progressIndicator.setVisibility(View.GONE);
    }

    private void showProgressIndicator() {
        progressIndicator.setVisibility(View.VISIBLE);
    }

    private void playGame() {

        gameFragment = GameFragment.newInstance(mMyParticipantId, hostParticipantId, clientParticipantId);
        getSupportFragmentManager().beginTransaction().replace(R.id.fullscreen_content, gameFragment).commit();
    }

    public void startQuickGame(long role) {
        // auto-match criteria to invite one random automatch opponent.
        // You can also specify more opponents (up to 3).
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, role);

        // build the room config:
        RoomConfig roomConfig =
                RoomConfig.builder(roomUpdateCallback)
                        .setOnMessageReceivedListener(realTimeMessageReceivedListener)
                        .setRoomStatusUpdateCallback(roomStatusUpdateCallback)
                        .setAutoMatchCriteria(autoMatchCriteria)
                        .build();

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Save the roomConfig so we can use it if we call leave().
        joinedRoomConfig = roomConfig;

        // create room:
        Games.getRealTimeMultiplayerClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .create(roomConfig);
    }

    private RoomUpdateCallback roomUpdateCallback = new RoomUpdateCallback() {
        @Override
        public void onRoomCreated(int code, @Nullable Room room) {
            // Update UI and internal state based on room updates.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.d(TAG, "Room " + room.getRoomId() + " created.");
            } else {
                Log.w(TAG, "Error creating room: " + code);
                // let screen go to sleep
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        public void onJoinedRoom(int code, @Nullable Room room) {
            // Update UI and internal state based on room updates.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.d(TAG, "Room " + room.getRoomId() + " joined.");
            } else {
                Log.w(TAG, "Error joining room: " + code);
                // let screen go to sleep
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            }
        }

        @Override
        public void onLeftRoom(int code, @NonNull String roomId) {
            Log.d(TAG, "Left room" + roomId);
        }

        @Override
        public void onRoomConnected(int code, @Nullable Room room) {

            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.d(TAG, "Room " + room.getRoomId() + " connected.");
            } else {
                Log.w(TAG, "Error connecting to room: " + code);
                // let screen go to sleep
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            }
        }
    };

    private OnRealTimeMessageReceivedListener realTimeMessageReceivedListener = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {

            String message = null;
            try {
                message = new String(realTimeMessage.getMessageData(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (message != null) {

                Log.e(TAG, "onRealTimeMessageReceived: "+message);

                if (message.contains(MESSAGE_AGREE_ON_HOST) && hostParticipantId == null) {

                    String[] strArray = message.split(";");

                    Long buttonClickedTime = Long.parseLong(strArray[1]);
                    if (myButtonClickedTime < buttonClickedTime) {
                        hostParticipantId = mMyParticipantId;
                        clientParticipantId = realTimeMessage.getSenderParticipantId();
                    }
                    else {
                        hostParticipantId = realTimeMessage.getSenderParticipantId();
                        clientParticipantId = mMyParticipantId;
                    }

                    Log.e(TAG, "Host has been chosen to be: " + hostParticipantId);
                    Log.e(TAG, "Client has been chosen to be: " + clientParticipantId);
                }
                else if (message.contains(MESSAGE_ROUND_DATA)) {

                    String[] strArray = message.split(";");
                    int randomDirection = Integer.valueOf(strArray[1]);
                    int randomDelay = Integer.valueOf(strArray[2]);

                    gameFragment.handleRoundData(randomDirection, randomDelay);
                }
            }
        }
    };

    RoomStatusUpdateCallback roomStatusUpdateCallback = new RoomStatusUpdateCallback() {
        @Override
        public void onRoomConnecting(@Nullable Room room) {
            waitingRoomFragment.updateStatus("Creating game room...");
        }

        @Override
        public void onRoomAutoMatching(@Nullable Room room) {
            waitingRoomFragment.updateStatus("Searching for an opponent...");
        }

        @Override
        public void onPeerInvitedToRoom(@Nullable Room room, @NonNull List<String> list) {

        }

        @Override
        public void onPeerDeclined(@Nullable Room room, @NonNull List<String> list) {

        }

        @Override
        public void onPeerJoined(@Nullable Room room, @NonNull List<String> list) {
            waitingRoomFragment.updateStatus("Player has joined...");
        }

        @Override
        public void onPeerLeft(@Nullable Room room, @NonNull List<String> list) {

            // Peer left, see if game should be canceled.
            if (!mPlaying) {
                Games.getRealTimeMultiplayerClient(MultiplayerActivity.this,
                        GoogleSignIn.getLastSignedInAccount(MultiplayerActivity.this))
                        .leave(joinedRoomConfig, room.getRoomId());
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        public void onConnectedToRoom(@Nullable Room room) {

            // Connected to room, record the room Id.
            mRoom = room;
            mMyParticipantId = room.getParticipantId(mMyPlayerId);
            Games.getPlayersClient(MultiplayerActivity.this, GoogleSignIn.getLastSignedInAccount(MultiplayerActivity.this))
                    .getCurrentPlayerId().addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String playerId) {

                    if (hostParticipantId == null) {

                        String message = MESSAGE_AGREE_ON_HOST +";"+myButtonClickedTime;
                        ArrayList<String> participantIds = mRoom.getParticipantIds();
                        for (String pId: participantIds) {

                            if (!pId.equals(mMyParticipantId)) {

                                Games.getRealTimeMultiplayerClient(MultiplayerActivity.this, GoogleSignIn.getLastSignedInAccount(MultiplayerActivity.this))
                                        .sendReliableMessage(message.getBytes(), mRoom.getRoomId(), pId, new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                                            @Override
                                            public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {

                                                Log.d(TAG, "RealTime message sent");
                                                Log.d(TAG, "  statusCode: " + statusCode);
                                                Log.d(TAG, "  tokenId: " + tokenId);
                                                Log.d(TAG, "  recipientParticipantId: " + recipientParticipantId);
                                            }
                                        })
                                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                            @Override
                                            public void onSuccess(Integer tokenId) {
                                                Log.d(TAG, "Created a reliable message with tokenId: " + tokenId);
                                            }
                                        }).addOnCompleteListener(new OnCompleteListener<Integer>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Integer> task) {
                                                countdownHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        playGame();
                                                    }
                                                }, 3000);
                                            }
                                        });
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onDisconnectedFromRoom(@Nullable Room room) {

            // This usually happens due to a network error, leave the game.
            Games.getRealTimeMultiplayerClient(MultiplayerActivity.this, GoogleSignIn.getLastSignedInAccount(MultiplayerActivity.this))
                    .leave(joinedRoomConfig, room.getRoomId());
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // show error message and return to main screen
            mRoom = null;
            joinedRoomConfig = null;

        }

        @Override
        public void onPeersConnected(@Nullable Room room, @NonNull List<String> list) {

            if (mPlaying) {
                // add new player to an ongoing game
            } else if (shouldStartGame(room)) {
                // ready to start game!

                for (String player: list) {
                    Participant p = room.getParticipant(player);
                    if (p != null) {

                        waitingRoomFragment.showOpponent(p);
                    }
                }
            }
        }

        @Override
        public void onPeersDisconnected(@Nullable Room room, @NonNull List<String> list) {

            if (mPlaying) {
                // do game-specific handling of this -- remove player's avatar
                // from the screen, etc. If not enough players are left for
                // the game to go on, end the game and leave the room.
            } else if (shouldCancelGame(room)) {
                // cancel the game
                Games.getRealTimeMultiplayerClient(MultiplayerActivity.this,
                        GoogleSignIn.getLastSignedInAccount(MultiplayerActivity.this))
                        .leave(joinedRoomConfig, room.getRoomId());
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        public void onP2PConnected(@NonNull String s) {

        }

        @Override
        public void onP2PDisconnected(@NonNull String s) {

        }
    };

    // Returns whether the room is in a state where the game should be canceled.
    boolean shouldCancelGame(Room room) {
        // TODO: Your game-specific cancellation logic here. For example, you might decide to
        // cancel the game if enough people have declined the invitation or left the room.
        // You can check a participant's status with Participant.getStatus().
        // (Also, your UI should have a Cancel button that cancels the game too)
        return false;
    }

    boolean shouldStartGame(Room room) {
        int connectedPlayers = 0;
        for (Participant p : room.getParticipants()) {
            if (p.isConnectedToRoom()) {
                ++connectedPlayers;
            }
        }
        return connectedPlayers >= MIN_PLAYERS;
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("KEVIN", "connection failed: "+connectionResult.getErrorMessage());
    }

    public void setMyPlayerId(String myPlayerId) {
        this.mMyPlayerId = myPlayerId;
    }

    public void sendRoundData(int roundNumber, final int randomDirection, final int randomDelay) {

        String message = MESSAGE_ROUND_DATA+";"+randomDirection+";"+randomDelay;
        ArrayList<String> participantIds = mRoom.getParticipantIds();

        for (String pId: participantIds) {
            Games.getRealTimeMultiplayerClient(MultiplayerActivity.this, GoogleSignIn.getLastSignedInAccount(MultiplayerActivity.this))
                .sendUnreliableMessage(message.getBytes(), mRoom.getRoomId(), pId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Created a unreliable message");
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        gameFragment.handleRoundData(randomDirection, randomDelay);
                    }
                });
        }

    }
}
