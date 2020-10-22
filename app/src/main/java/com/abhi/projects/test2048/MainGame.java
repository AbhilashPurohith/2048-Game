package com.abhi.projects.test2048;

import android.graphics.Color;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Random;

public class MainGame {

    // Start New Game
    public void startNewGame(ArrayList<Button> buttonCollection) {
        resetGame(buttonCollection);  // Resetting Game before Starting
        int count = 0;
        for (int x = 0; x < buttonCollection.size(); x++) {
            Button randomButton = buttonCollection.get(new Random().nextInt(buttonCollection.size() - x));
            if (randomButton.getText().equals("")) {
                String value = Math.random() < 0.9 ? "2" : "4";
                int color = value.equals("2") ? Color.parseColor("#eee4da") : Color.parseColor("#ede0c8");
                randomButton.setText(value);
                randomButton.setBackgroundColor(color);
                count = count + 1;
            }
            if (count > 1) {
                break;
            }
        }
    }

    // Randomly Spawn a number on empty tiles
    public void randomSpawnNumber(ArrayList<Button> buttonCollection) {
        boolean isBlocksFilled = isBlocksFilled(buttonCollection);
        if (!isBlocksFilled) {
            int count = 0;
            for (int x = 0; x < buttonCollection.size(); x++) {
                // size - 1 - x is used to avoid index out of bound exception
                Button randomButton = buttonCollection.get(new Random().nextInt(buttonCollection.size() - 1));
                if (randomButton.getText().equals("")) {
                    String value = Math.random() < 0.9 ? "2" : "4";
                    int color = value.equals("2") ? getTileColor(2) : getTileColor(4);
                    randomButton.setText(value);
                    randomButton.setBackgroundColor(color);
                    count = count + 1;
                }
                if (count > 0) {
                    break;
                }
            }
        }
    }

    // Check if all tiles all filled
    public boolean isBlocksFilled(ArrayList<Button> buttonCollection) {
        int count = 0;
        for (int x = 0; x < buttonCollection.size(); x++) {
            if (buttonCollection.get(x).getText().equals("")) {
                count = count + 1;
                return false;
            }
        }
        if (count == 0) {
            return true;
        }
        return false;
    }

    // Resets the Game
    public void resetGame(ArrayList<Button> buttonCollection) {
        for (Button b : buttonCollection) {
            b.setText("");
            b.setBackgroundColor(getTileColor(0));
        }
    }

    // Gets color of 2^x tiles
    public int getTileColor(int value) {

        switch (value) {
            case 2:
                return Color.parseColor("#eee4da");
            case 4:
                return Color.parseColor("#ede0c8");
            case 8:
                return Color.parseColor("#f2b179");
            case 16:
                return Color.parseColor("#f59563");
            case 32:
                return Color.parseColor("#f67c5f");
            case 64:
                return Color.parseColor("#f65e3b");
            case 128:
                return Color.parseColor("#edcf72");
            case 256:
                return Color.parseColor("#edcc61");
            case 512:
                return Color.parseColor("#edc850");
            case 1024:
                return Color.parseColor("#edc53f");
            case 2048:
                return Color.parseColor("#edc22e");
            case 4096:
                return Color.parseColor("#3c3a32");
        }

        return Color.parseColor("#cdc1b5");
    }

}
