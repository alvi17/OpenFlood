package alvi17.openflood;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Activity allowing the user to play the actual game.
 */
public class GameActivity extends AppCompatActivity
        implements EndGameDialogFragment.EndGameDialogFragmentListener,
        SeedDialogFragment.SeedDialogFragmentListener {

    private final int UPDATE_SETTINGS = 1;

    private Game game;
    private SharedPreferences sp;
    private SharedPreferences.Editor spEditor;

    private FloodView floodView;
    private TextView stepsTextView;

    private int lastColor;

    private boolean gameFinished;

    // Paints to be used for the board
    private Paint paints[];


    LinearLayout linearLayout;
    AdView adView;
    AdRequest adRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(alvi17.openflood.R.layout.activity_game);

        // Initialize the SharedPreferences and SharedPreferences editor
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = sp.edit();

        // Get the FloodView
        floodView = (FloodView) findViewById(alvi17.openflood.R.id.floodView);

        // Initialize the paints array and pass it to the FloodView
        initPaints();
        floodView.setPaints(paints);

        ImageView settingsButton = (ImageView) findViewById(alvi17.openflood.R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
              Intent launchSettingsIntent = new Intent(GameActivity.this, SettingsActivity.class);
              startActivityForResult(launchSettingsIntent, UPDATE_SETTINGS);
          }
      }
        );

        ImageView infoButton = (ImageView) findViewById(alvi17.openflood.R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchSettingsIntent = new Intent(GameActivity.this, InfoActivity.class);
                startActivity(launchSettingsIntent);
            }
        });

        ImageView newGameButton = (ImageView) findViewById(alvi17.openflood.R.id.newGameButton);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
            }
        });
        newGameButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SeedDialogFragment seedDialogFragment = new SeedDialogFragment();
                seedDialogFragment.show(getSupportFragmentManager(), "SeedDialog");
                return true;
            }
        });

        // Get the steps text view
        stepsTextView = (TextView) findViewById(alvi17.openflood.R.id.stepsTextView);

        // Set up a new game
        newGame();

        linearLayout=(LinearLayout)findViewById(R.id.adsLayout);
        adView=new AdView(this);
        adRequest=new AdRequest.Builder().build();
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-6508526601344465/4115437630");
        adView.loadAd(adRequest);
        linearLayout.addView(adView);
    }

    private int getBoardSize(){
        int defaultBoardSize = getResources().getInteger(alvi17.openflood.R.integer.default_board_size);
        if (!sp.contains("board_size")) {
            spEditor.putInt("board_size", defaultBoardSize);
            spEditor.apply();
        }
        return sp.getInt("board_size", defaultBoardSize);
    }

    private int getNumColors(){
        int defaultNumColors = getResources().getInteger(alvi17.openflood.R.integer.default_num_colors);
        if (!sp.contains("num_colors")) {
            spEditor.putInt("num_colors", defaultNumColors);
            spEditor.apply();
        }
        return sp.getInt("num_colors", defaultNumColors);
    }

    private void initPaints() {
        int[] colors;
        if (sp.getBoolean("use_old_colors", false)){
            colors = getResources().getIntArray(alvi17.openflood.R.array.oldBoardColorScheme);
        } else {
            colors = getResources().getIntArray(alvi17.openflood.R.array.boardColorScheme);
        }

        paints = new Paint[colors.length];
        for (int i = 0; i < colors.length; i++) {
            paints[i] = new Paint();
            paints[i].setColor(colors[i]);
        }
        return;
    }

    private void newGame() {
        game = new Game(getBoardSize(), getNumColors());
        gameFinished = false;
        lastColor = game.getColor(0, 0);

        layoutColorButtons();

        stepsTextView.setText(game.getSteps() + " / " + game.getMaxSteps());
        floodView.setBoardSize(getBoardSize());
        floodView.drawGame(game);
    }

    private void newGame(String seed) {
        game = new Game(getBoardSize(), getNumColors(), seed);
        gameFinished = false;
        lastColor = game.getColor(0, 0);

        layoutColorButtons();

        stepsTextView.setText(game.getSteps() + " / " + game.getMaxSteps());
        floodView.setBoardSize(getBoardSize());
        floodView.drawGame(game);
    }

    private void layoutColorButtons() {
        // Add color buttons
        LinearLayout buttonLayout = (LinearLayout) findViewById(alvi17.openflood.R.id.buttonLayout);
        buttonLayout.removeAllViews();
        int buttonPadding = (int) getResources().getDimension(alvi17.openflood.R.dimen.color_button_padding);
        for (int i = 0; i < getNumColors(); i++) {
            final int localI = i;
            ColorButton newButton = new ColorButton(this);
            newButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(AnimationUtils.loadAnimation(GameActivity.this, alvi17.openflood.R.anim.button_anim));
                    if (localI != lastColor) {
                        doColor(localI);
                    }
                }
            });
            newButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
            newButton.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);

            newButton.setColorBlindText(Integer.toString(i + 1));
            newButton.setColor(paints[i].getColor());
            buttonLayout.addView(newButton);
        }
        return;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                // Only start a new game if the settings have been changed
                if (extras.getBoolean("gameSettingsChanged")) {
                    newGame();
                }
                if (extras.getBoolean("colorSettingsChanged")) {
                    initPaints();
                    floodView.setPaints(paints);
                    layoutColorButtons();
                }
            }
        }
    }

    private void doColor(int color) {
        if (gameFinished || game.getSteps() >= game.getMaxSteps()) {
            return;
        }

        game.flood(color);
        floodView.drawGame(game);
        lastColor = color;
        stepsTextView.setText(game.getSteps() + " / " + game.getMaxSteps());

        if (game.checkWin() || game.getSteps() == game.getMaxSteps()) {
            gameFinished = true;
            showAds();
            showEndGameDialog();
        }

        return;
    }

    public void showAds()
    {
        final InterstitialAd interstitialAd=new  InterstitialAd(GameActivity.this);
        interstitialAd.setAdUnitId("ca-app-pub-6508526601344465/5592170834");
        AdRequest aRequest = new AdRequest.Builder().addTestDevice("858A1315E0C450B387A59834A836CF5D").build();

        // Begin loading your interstitial.  0754C239B1E2E19421FDE46BCEFB8855
        interstitialAd.loadAd(aRequest);
        interstitialAd.setAdListener(
                new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        interstitialAd.show();
                    }
                }
        );
    }

    public void onNewGameClick() {
        newGame();
        showAds();
        return;
    }

    public void onReplayClick() {
        newGame(game.getSeed());
    }

    public void onLaunchSeedDialogClick() {
        SeedDialogFragment seedDialogFragment = new SeedDialogFragment();
        seedDialogFragment.show(getSupportFragmentManager(), "SeedDialog");
        return;
    }

    public void onGetSeedClick() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("seed", game.getSeed());
        clipboard.setPrimaryClip(clip);
        Toast toast = Toast.makeText(this, getString(alvi17.openflood.R.string.game_seed_copied), Toast.LENGTH_SHORT);
        toast.show();
        return;
    }

    public void onNewGameFromSeedClick(String seed) {
        newGame(seed);
    }

    private void showEndGameDialog() {
        DialogFragment endGameDialog = new EndGameDialogFragment();
        Bundle args = new Bundle();
        args.putInt("steps", game.getSteps());
        args.putBoolean("game_won", game.checkWin());
        args.putString("seed", game.getSeed());
        endGameDialog.setArguments(args);
        endGameDialog.show(getSupportFragmentManager(), "EndGameDialog");
        return;
    }
}
