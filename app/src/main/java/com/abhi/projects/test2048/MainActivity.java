package com.abhi.projects.test2048;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    MainGame mg = new MainGame();
    private GestureDetector gestureDetector;
    private float x1, x2, y1, y2;
    static final int MIN_DISTANCE = 150;
    private final String BEST = "BEST";
    private final String GAME_OVER = "GAME OVER";
    private final String GAME_DESC = "Join the numbers and get to the 2048 title!";
    private final String regex1 = "button+[1-4]";
    private final String regex2 = "button+[5-8]";
    private final String regex3 = "button+(9|1[0-2])";
    private final String regex4 = "button+1[3-6]";
    private final String regex5 = "button+([48]|12|16)";
    private final String regex6 = "button+([059]|13)";
    int score_value = 0;
    int tilesMoved = 0;
    Animation fadeInAnimation, bounceAnimation, slideDownAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Share Preference is used to Store the high score - It will be erased only when app's data is cleared
        SharedPreferences preferences = getPreferences(0);
        Button highScore = findViewById(R.id.highScoreValue);
        highScore.setText(String.valueOf(preferences.getInt(BEST, 0))); // Default value is 0

        // Styling "2048 title!" word
        SpannableString ss = new SpannableString(GAME_DESC);
        ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(131, 119, 113));
        ss.setSpan(fcs, 32, 43, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 32, 43, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView textView = findViewById(R.id.textMessage);
        textView.setText(ss);

        setButtonListener(getButtonCollection());

        // starting new Game onCreate
        mg.startNewGame(getButtonCollection());

        // Setting Gesture for Swipes
        gestureDetector = new GestureDetector(this, new OnSwipeTouchListener() {
            @Override
            public boolean onSwipe() {
                return true;
            }
        });

        // Pass the Touchable events to the children of Constraint layout
        setTouchabilityToGridLayout();

        setAnimation();

    }

    // Setting the animation for different activities
    public void setAnimation() {
        bounceAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_in);
        fadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        slideDownAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);

        bounceAnimation.setDuration(2000);
        slideDownAnimation.setDuration(3000);
        fadeInAnimation.setDuration(6000);

        TextView text = findViewById(R.id.textMessage);
        TextView title = findViewById(R.id.title);
        LinearLayout l = findViewById(R.id.gridLinearLayout);
        Button newGame = findViewById(R.id.newGame);

        l.setAnimation(bounceAnimation);
        text.startAnimation(bounceAnimation);
        newGame.setAnimation(fadeInAnimation);
        title.setAnimation(slideDownAnimation);
    }

    // Start New Game When NEW GAME button is clicked
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newGame: {
                mg.startNewGame(getButtonCollection());
                score_value = 0;
                Button score = findViewById(R.id.scoreValue);
                score.setText(String.valueOf(score_value));
                break;
            }
        }
    }

    // Getting all buttons when required
    public ArrayList<Button> getButtonCollection() {
        ArrayList<Button> buttonArray = new ArrayList();
        for (int i = 1; i <= 16; i++) {
            int id = getResources().getIdentifier("button" + i, "id", getPackageName());
            Button b = findViewById(id);
            buttonArray.add(b);
        }
        return buttonArray;
    }

    // Setting Button Listeners and Setting default Test Size of Buttons
    private void setButtonListener(ArrayList<Button> buttonArray) {

        Button newGame = findViewById(R.id.newGame);
        newGame.setOnClickListener(this);
        for (Button b : buttonArray) {
            b.setOnClickListener(this);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // gesture event initialized earlier in onCreate gets the event and passes it to OnTouch
        gestureDetector.onTouchEvent(event);

        // Swiping logic and Movement of tiles
        getSwipeDirections(event);
        return true;
    }

    public void setTouchabilityToGridLayout(){
        // Setting the whole screen to allow swipes ( Constraint Layout and Linear Layout)
        ConstraintLayout parentView = findViewById(R.id.constraintLayout);
        parentView.setOnTouchListener(this);
        // Post is used to pass the parents touches/clicks basically events to its children
        parentView.post(new Runnable() {
            @Override
            public void run() {
                // Getting the inner layout to pass the touch events to all the buttons
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.gridLinearLayout).findViewById(R.id.linearLayoutG1);
                linearLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Button b : getButtonCollection()) {
                            b.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    // gesture event initialized earlier in onCreate gets the event and passes it to OnTouch
                                    gestureDetector.onTouchEvent(event);
                                    getSwipeDirections(event);// Swiping logic and Movement of tiles
                                    return true;
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void generateRandomNumbersAndCheckGameEnd() {
        // generate random number only if the tile is moved
        if (tilesMoved > 0) {
            mg.randomSpawnNumber(getButtonCollection());
        }
        // check if all blocks are filled - Game Over
        if (mg.isBlocksFilled(getButtonCollection())) {
            showLongToast(GAME_OVER);
        }
    }

    private void getSwipeDirections(MotionEvent event) {

        // event.getAction() = detect the numbers - 0(first touch) , 2 ( Move ), 1 (last Touch)
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX() * event.getXPrecision();
                y1 = event.getY() * event.getYPrecision();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX() * event.getXPrecision();
                y2 = event.getY() * event.getYPrecision();
                float deltaX = x2 - x1; // Left/Right points difference
                float deltaY = y2 - y1; // Up/Down points difference

                // Checking if the finger is moved with MIN DISTANCE
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // Left to Right swipe action
                    if (x2 > x1) {
                        move("Right");
                        generateRandomNumbersAndCheckGameEnd();
                    }
                    // Right to left swipe action
                    else {
                        move("Left");
                        generateRandomNumbersAndCheckGameEnd();
                    }
                } else if (Math.abs(deltaY) > MIN_DISTANCE) {
                    // Uo to Down swipe action
                    if (y2 > y1) {
                        move("Down");
                        generateRandomNumbersAndCheckGameEnd();
                    }
                    // Down to Up swipe action
                    else {
                        move("Up");
                        generateRandomNumbersAndCheckGameEnd();
                    }
                } else {
                    // a screen tap/touch - No movement
//                    showToast("Please Swipe");
                }
                break;
        }
    }

    // get the value of a particular Button
    private String getButtonStringValue(int index) {
        return getButtonCollection().get(index).getText().toString();
    }

    // Check if values of 2 Buttons are Equal
    private boolean isValueEqual(int index1, int index2) {
        return getButtonStringValue(index1).equals(getButtonStringValue(index2));
    }

    // Toast with short duration
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Toast with Long duration
    public void showLongToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // Logic - Moves based on the direction Given
    public void move(String direction) {
        SharedPreferences sharedPreferences = getPreferences(0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Button score = findViewById(R.id.scoreValue);
        Button highScore = findViewById(R.id.highScoreValue);
        // Left to Right
        if (direction.equalsIgnoreCase("Right")) {
            tilesMoved = 0;
            for (int x = 0; x < getButtonCollection().size(); x++) {
                Button b = getButtonCollection().get(x);
                if (!b.getText().equals("") && !b.getResources().getResourceEntryName(b.getId()).matches(regex5)) {
                    if (b.getResources().getResourceEntryName(b.getId()).matches(regex1)) {
                        if (!getButtonStringValue(x + 1).equals("")) {
                            if (isValueEqual(x, x + 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (((x + 1) + 1 < 4) && !getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(3).equals("")) {
                                    getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                                } else if (((x + 1) + 1 < 4) && getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(3).equals("")) {
                                    getButtonCollection().get((x + 1) + 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x + 1) + 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 1).setText("");
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(3).setText(String.valueOf(value));
                                    getButtonCollection().get(3).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x + 2) < 4) && !getButtonStringValue(x + 2).equals("")) {
                            if (isValueEqual(x, x + 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;

                                if (!getButtonStringValue(3).equals("")) {
                                    getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 2).setText("");
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(0));

                                    getButtonCollection().get(3).setText(String.valueOf(value));
                                    getButtonCollection().get(3).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));

                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(3).equals("")) {
                            if (isValueEqual(x, 3)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));

                                getButtonCollection().get(3).setText(String.valueOf(value));
                                getButtonCollection().get(3).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(3).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));

                            getButtonCollection().get(3).setText(String.valueOf(value));
                            getButtonCollection().get(3).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex2)) {
                        if (!getButtonStringValue(x + 1).equals("")) {
                            if (isValueEqual(x, x + 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (((x + 1) + 1 < 8) && !getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(7).equals("")) {
                                    getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                                } else if (((x + 1) + 1 < 8) && getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(7).equals("")) {
                                    getButtonCollection().get((x + 1) + 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x + 1) + 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 1).setText("");
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(0));
//                                                getButtonCollection().get(6).setText("");
                                    getButtonCollection().get(7).setText(String.valueOf(value));
                                    getButtonCollection().get(7).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x + 2) < 8) && !getButtonStringValue(x + 2).equals("")) {
                            if (isValueEqual(x, x + 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(7).equals("")) {
                                    getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(7).setText(String.valueOf(value));
                                    getButtonCollection().get(7).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(7).equals("")) {
                            if (isValueEqual(x, 7)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));

                                getButtonCollection().get(7).setText(String.valueOf(value));
                                getButtonCollection().get(7).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(7).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));

                            getButtonCollection().get(x + 2).setText("");
                            getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(0));

                            getButtonCollection().get(7).setText(String.valueOf(value));
                            getButtonCollection().get(7).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex3)) {
                        if (!getButtonStringValue(x + 1).equals("")) {
                            if (isValueEqual(x, x + 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (((x + 1) + 1 < 12) && !getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(11).equals("")) {
                                    b.setText("");
                                    b.setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                                } else if (((x + 1) + 1 < 12) && getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(11).equals("")) {
                                    getButtonCollection().get((x + 1) + 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x + 1) + 1).setBackgroundColor(mg.getTileColor(value));
                                } else if ((x + 1) == 11) {
                                    b.setText("");
                                    b.setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(11).setText(String.valueOf(value));
                                    getButtonCollection().get(11).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 1).setText("");
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(0));

                                    getButtonCollection().get(11).setText(String.valueOf(value));
                                    getButtonCollection().get(11).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x + 2) < 12) && !getButtonStringValue(x + 2).equals("")) {
                            if (isValueEqual(x, x + 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(11).equals("")) {
                                    getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 2).setText("");
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(0));

                                    getButtonCollection().get(11).setText(String.valueOf(value));
                                    getButtonCollection().get(11).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(11).equals("")) {
                            if (isValueEqual(x, 11)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));

                                getButtonCollection().get(11).setText(String.valueOf(value));
                                getButtonCollection().get(11).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(11).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(11).setText(String.valueOf(value));
                            getButtonCollection().get(11).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex4)) {
                        if (!getButtonStringValue(x + 1).equals("")) {
                            if (isValueEqual(x, x + 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (((x + 1) + 1 < 16) && !getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(15).equals("")) {

                                    getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                                } else if (((x + 1) + 1 < 16) && getButtonStringValue((x + 1) + 1).equals("") && !getButtonStringValue(15).equals("")) {
                                    getButtonCollection().get((x + 1) + 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x + 1) + 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 1).setText("");
                                    getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(0));

                                    getButtonCollection().get(15).setText(String.valueOf(value));
                                    getButtonCollection().get(15).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x + 2) < 16) && !getButtonStringValue(x + 2).equals("")) {
                            if (isValueEqual(x, x + 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(15).equals("")) {
                                    getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 2).setText("");
                                    getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(0));

                                    getButtonCollection().get(15).setText(String.valueOf(value));
                                    getButtonCollection().get(15).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 1).setText(String.valueOf(value));
                                getButtonCollection().get(x + 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(15).equals("")) {
                            if (isValueEqual(x, 15)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(15).setText(String.valueOf(value));
                                getButtonCollection().get(15).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 2).setText(String.valueOf(value));
                                getButtonCollection().get(x + 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(15).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(15).setText(String.valueOf(value));
                            getButtonCollection().get(15).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    }
                }
            }
            score.setText(String.valueOf(score_value));
            int best = sharedPreferences.getInt(BEST, 0);
            if (best <= score_value) {
                highScore.setText(String.valueOf(score_value));
                editor.putInt(BEST, score_value);
                editor.apply();
                editor.commit();
            }
        }
        // Right to Left
        else if (direction.equalsIgnoreCase("Left")) {
            tilesMoved = 0;
            for (int x = getButtonCollection().size() - 1; x > 0; x--) {
                Button b = getButtonCollection().get(x);
                if (!b.getText().equals("") && !b.getResources().getResourceEntryName(b.getId()).matches(regex6)) {
                    if (b.getResources().getResourceEntryName(b.getId()).matches(regex4)) {
                        if (!getButtonStringValue(x - 1).equals("")) {
                            if (isValueEqual(x, x - 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if ((((x - 1) - 1) > 11) && !getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(12).equals("")) {
                                    getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                                } else if ((((x - 1) - 1) > 11) && getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(12).equals("")) {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get((x - 1) - 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x - 1) - 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(12).setText(String.valueOf(value));
                                    getButtonCollection().get(12).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x - 2) > 11) && !getButtonStringValue(x - 2).equals("")) {
                            if (isValueEqual(x, x - 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(12).equals("")) {
                                    getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 2).setText("");
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(12).setText(String.valueOf(value));
                                    getButtonCollection().get(12).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(12).equals("")) {
                            if (isValueEqual(x, 12)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(12).setText(String.valueOf(value));
                                getButtonCollection().get(12).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(12).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(12).setText(String.valueOf(value));
                            getButtonCollection().get(12).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex3)) {
                        if (!getButtonStringValue(x - 1).equals("")) {
                            if (isValueEqual(x, x - 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if ((((x - 1) - 1) > 7) && !getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(8).equals("")) {
                                    getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                                } else if ((((x - 1) - 1) > 7) && getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(8).equals("")) {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get((x - 1) - 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x - 1) - 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(8).setText(String.valueOf(value));
                                    getButtonCollection().get(8).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x - 2) > 7) && !getButtonStringValue(x - 2).equals("")) {
                            if (isValueEqual(x, x - 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(8).equals("")) {
                                    getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 2).setText("");
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(8).setText(String.valueOf(value));
                                    getButtonCollection().get(8).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(8).equals("")) {
                            if (isValueEqual(x, 8)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(8).setText(String.valueOf(value));
                                getButtonCollection().get(8).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(8).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));

                            getButtonCollection().get(8).setText(String.valueOf(value));
                            getButtonCollection().get(8).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex2)) {
                        if (!getButtonStringValue(x - 1).equals("")) {
                            if (isValueEqual(x, x - 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;

                                if ((((x - 1) - 1) > 3) && !getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(4).equals("")) {
                                    getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                                } else if ((((x - 1) - 1) > 3) && getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(4).equals("")) {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get((x - 1) - 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x - 1) - 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(4).setText(String.valueOf(value));
                                    getButtonCollection().get(4).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x - 2) > 3) && !getButtonStringValue(x - 2).equals("")) {
                            if (isValueEqual(x, x - 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(4).equals("")) {
                                    getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 2).setText("");
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(4).setText(String.valueOf(value));
                                    getButtonCollection().get(4).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(4).equals("")) {
                            if (isValueEqual(x, 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(4).setText(String.valueOf(value));
                                getButtonCollection().get(4).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(4).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(4).setText(String.valueOf(value));
                            getButtonCollection().get(4).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex1)) {
                        if (!getButtonStringValue(x - 1).equals("")) {
                            if (isValueEqual(x, x - 1)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if ((((x - 1) - 1) > -1) && !getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(0).equals("")) {
                                    getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                                } else if ((((x - 1) - 1) > -1) && getButtonStringValue((x - 1) - 1).equals("") && !getButtonStringValue(0).equals("")) {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get((x - 1) - 1).setText(String.valueOf(value));
                                    getButtonCollection().get((x - 1) - 1).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 1).setText("");
                                    getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(0).setText(String.valueOf(value));
                                    getButtonCollection().get(0).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (((x - 2) > -1) && !getButtonStringValue(x - 2).equals("")) {
                            if (isValueEqual(x, x - 2)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(0).equals("")) {
                                    getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 2).setText("");
                                    getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(0).setText(String.valueOf(value));
                                    getButtonCollection().get(0).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 1).setText(String.valueOf(value));
                                getButtonCollection().get(x - 1).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(0).equals("")) {
                            if (isValueEqual(x, 0)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(0).setText(String.valueOf(value));
                                getButtonCollection().get(0).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 2).setText(String.valueOf(value));
                                getButtonCollection().get(x - 2).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(0).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(0).setText(String.valueOf(value));
                            getButtonCollection().get(0).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    }
                }
            }
            // Updating the score and checking for HighScore
            score.setText(String.valueOf(score_value));
            int best = sharedPreferences.getInt(BEST, 0);
            if (best <= score_value) {
                highScore.setText(String.valueOf(score_value));
                editor.putInt(BEST, score_value);
                editor.apply();
                editor.commit();
            }
        }

        // Up to Down
        else if (direction.equalsIgnoreCase("Down")) {
            tilesMoved = 0;
            for (int x = 0; x < getButtonCollection().size(); x++) {
                Button b = getButtonCollection().get(x);
                if (!b.getText().equals("")) {
                    if (b.getResources().getResourceEntryName(b.getId()).matches(regex1)) {
                        if (!getButtonStringValue(x + 4).equals("")) {
                            if (isValueEqual(x, x + 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(x + 8).equals("") && !getButtonStringValue(x + 12).equals("")) {
                                    getButtonCollection().get(x + 4).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(value));
                                } else if (getButtonStringValue(x + 8).equals("") && !getButtonStringValue(x + 12).equals("")) {
                                    getButtonCollection().get(x + 4).setText("");
                                    getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 8).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 4).setText("");
                                    getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 12).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 12).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (!getButtonStringValue(x + 8).equals("")) {
                            if (isValueEqual(x, x + 8)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(x + 12).equals("")) {
                                    getButtonCollection().get(x + 8).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x + 8).setText("");
                                    getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 12).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 12).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 4).setText(String.valueOf(value));
                                getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(x + 12).equals("")) {
                            if (isValueEqual(x, x + 12)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 12).setText(String.valueOf(value));
                                getButtonCollection().get(x + 12).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 8).setText(String.valueOf(value));
                                getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonCollection().get(x + 12).getText().equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(x + 8).setText("");
                            getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(x + 12).setText(String.valueOf(value));
                            getButtonCollection().get(x + 12).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex2)) {
                        if (!getButtonStringValue(x + 4).equals("")) {
                            if (isValueEqual(x, x + 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(x + 8).equals("")) {
                                    b.setText("");
                                    b.setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 4).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    b.setText("");
                                    b.setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 4).setText("");
                                    getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x + 8).setText(String.valueOf(value));
                                    getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(value));
                                }
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (!getButtonStringValue(x + 8).equals("")) {
                            if (isValueEqual(x, x + 8)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 8).setText(String.valueOf(value));
                                getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 4).setText(String.valueOf(value));
                                getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(x + 8).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(x + 8).setText(String.valueOf(value));
                            getButtonCollection().get(x + 8).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex3)) {
                        if (!getButtonStringValue(x + 4).equals("")) {
                            if (isValueEqual(x, x + 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x + 4).setText(String.valueOf(value));
                                getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(value));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(x + 4).setText(String.valueOf(value));
                            getButtonCollection().get(x + 4).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    }
                }
            }
            // Updating the score and checking for HighScore
            score.setText(String.valueOf(score_value));
            int best = sharedPreferences.getInt(BEST, 0);
            if (best <= score_value) {
                highScore.setText(String.valueOf(score_value));
                editor.putInt(BEST, score_value);
                editor.apply();
                editor.commit();
            }
        }

        // Down to Up
        else if (direction.equalsIgnoreCase("Up")) {
            tilesMoved = 0;
            for (int x = getButtonCollection().size() - 1; x > 0; x--) {
                Button b = getButtonCollection().get(x);
                if (!b.getText().equals("")) {
                    if (b.getResources().getResourceEntryName(b.getId()).matches(regex4)) {
                        if (!getButtonStringValue(x - 4).equals("")) {
                            if (isValueEqual(x, x - 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(x - 8).equals("") && !getButtonStringValue(x - 12).equals("")) {
                                    getButtonCollection().get(x - 4).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(value));
                                } else if (getButtonStringValue(x - 8).equals("") && !getButtonStringValue(x - 12).equals("")) {
                                    getButtonCollection().get(x - 4).setText("");
                                    getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x - 8).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 4).setText("");
                                    getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x - 12).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 12).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (!getButtonStringValue(x - 8).equals("")) {
                            if (isValueEqual(x, x - 8)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(x - 12).equals("")) {
                                    getButtonCollection().get(x - 8).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    getButtonCollection().get(x - 8).setText("");
                                    getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(0));

                                    getButtonCollection().get(x - 12).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 12).setBackgroundColor(mg.getTileColor(value));
                                }
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 4).setText(String.valueOf(value));
                                getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (!getButtonStringValue(x - 12).equals("")) {
                            if (isValueEqual(x, x - 12)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));

                                getButtonCollection().get(x - 12).setText(String.valueOf(value));
                                getButtonCollection().get(x - 12).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 8).setText(String.valueOf(value));
                                getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonCollection().get(x - 12).getText().equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(x - 8).setText("");
                            getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(0));
                            getButtonCollection().get(x - 12).setText(String.valueOf(value));
                            getButtonCollection().get(x - 12).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }

                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex3)) {
                        if (!getButtonStringValue(x - 4).equals("")) {
                            if (isValueEqual(x, x - 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                if (!getButtonStringValue(x - 8).equals("")) {
                                    b.setText("");
                                    b.setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x - 4).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(value));
                                } else {
                                    b.setText("");
                                    b.setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x - 4).setText("");
                                    getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(0));
                                    getButtonCollection().get(x - 8).setText(String.valueOf(value));
                                    getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(value));
                                }
                                tilesMoved = tilesMoved + 1;
                            }
                        } else if (!getButtonStringValue(x - 8).equals("")) {
                            if (isValueEqual(x, x - 8)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 8).setText(String.valueOf(value));
                                getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(value));
                            } else {
                                int value = Integer.parseInt(getButtonStringValue(x));
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 4).setText(String.valueOf(value));
                                getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(value));
                            }
                            tilesMoved = tilesMoved + 1;
                        } else if (getButtonStringValue(x - 8).equals("")) {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));

                            getButtonCollection().get(x - 8).setText(String.valueOf(value));
                            getButtonCollection().get(x - 8).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    } else if (b.getResources().getResourceEntryName(b.getId()).matches(regex2)) {
                        if (!getButtonStringValue(x - 4).equals("")) {
                            if (isValueEqual(x, x - 4)) {
                                int value = Integer.parseInt(getButtonStringValue(x)) * 2;
                                score_value = score_value + value;
                                b.setText("");
                                b.setBackgroundColor(mg.getTileColor(0));
                                getButtonCollection().get(x - 4).setText(String.valueOf(value));
                                getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(value));
                                tilesMoved = tilesMoved + 1;
                            }
                        } else {
                            int value = Integer.parseInt(getButtonStringValue(x));
                            b.setText("");
                            b.setBackgroundColor(mg.getTileColor(0));

                            getButtonCollection().get(x - 4).setText(String.valueOf(value));
                            getButtonCollection().get(x - 4).setBackgroundColor(mg.getTileColor(value));
                            tilesMoved = tilesMoved + 1;
                        }
                    }
                }
            }
            // Updating the score and checking for HighScore
            score.setText(String.valueOf(score_value));
            int best = sharedPreferences.getInt(BEST, 0);
            if (best <= score_value) {
                highScore.setText(String.valueOf(score_value));
                editor.putInt(BEST, score_value);
                editor.apply();
                editor.commit();
            }
        }
    }
}