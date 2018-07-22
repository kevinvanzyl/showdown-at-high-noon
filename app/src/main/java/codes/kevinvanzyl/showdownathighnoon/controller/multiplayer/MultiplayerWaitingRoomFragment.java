package codes.kevinvanzyl.showdownathighnoon.controller.multiplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import codes.kevinvanzyl.showdownathighnoon.R;


public class MultiplayerWaitingRoomFragment extends Fragment {

    private static final long ROLE_ANY = 0x0;

    private ImageView imgProfile1;
    private TextView txtProfile1;
    private ImageView imgProfile2;
    private TextView txtProfile2;
    private TextView txtMeText;
    private TextView txtOpponentText;

    public MultiplayerWaitingRoomFragment() {
        // Required empty public constructor
    }

    public static MultiplayerWaitingRoomFragment newInstance() {
        MultiplayerWaitingRoomFragment fragment = new MultiplayerWaitingRoomFragment();
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
        View v = inflater.inflate(R.layout.fragment_multiplayer_waiting_room, container, false);

        imgProfile1 = (ImageView) v.findViewById(R.id.image_profile_1);
        txtProfile1 = (TextView) v.findViewById(R.id.text_profile_1);
        imgProfile2 = (ImageView) v.findViewById(R.id.image_profile_2);
        txtProfile2 = (TextView) v.findViewById(R.id.text_profile_2);
        txtMeText = (TextView) v.findViewById(R.id.text_me_text);
        txtOpponentText = (TextView) v.findViewById(R.id.text_opponent_text);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MultiplayerActivity) getActivity()).silentSignIn();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void initSignIn(GoogleSignInAccount account) {

        imgProfile1.setVisibility(View.VISIBLE);
        txtProfile1.setVisibility(View.VISIBLE);
        txtMeText.setVisibility(View.VISIBLE);
        txtOpponentText.setVisibility(View.VISIBLE);

        PlayersClient playersClient = Games.getPlayersClient(getActivity(), account);

        playersClient.getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>() {
            @Override
            public void onComplete(@NonNull Task<Player> task) {

                ((MultiplayerActivity) getActivity()).setMyPlayerId(task.getResult().getPlayerId());

                ImageManager manager = ImageManager.create(getActivity());
                manager.loadImage(imgProfile1, task.getResult().getIconImageUri());

                txtProfile1.setText(task.getResult().getDisplayName());
                ((MultiplayerActivity) getActivity()).startQuickGame(ROLE_ANY);
            }
        });
    }

    public void updateStatus(String message) {
        txtOpponentText.setText(message);
    }

    public void showOpponent(Participant p) {
        imgProfile2.setVisibility(View.VISIBLE);
        txtProfile2.setVisibility(View.VISIBLE);

        ImageManager manager = ImageManager.create(getActivity());
        manager.loadImage(imgProfile2, p.getIconImageUri());

        txtProfile2.setText(p.getDisplayName());
        txtOpponentText.setText("Opponent");
    }
}
