package org.example;

public class GuessingGame {
    public int answer;
    public int guessesLeft = 7;

    public GuessingGame() {
        setAnswer((int)(Math.random() * 100) + 1);
    }

    public String handleGuess(int guess) {
        guessesLeft--;

        if (guess == answer)
            return "You guessed it! " + guess + " is the number!";

        if (guessesLeft <= 0)
            return answer + " was the correct answer. You lose.";

        if (guess > answer)
            return guess + " is too high. Try again.";


        return guess + " is too low. Try again.";
    }

    public void setAnswer(int i) {
        answer = i;
    }

    public int getAnswer() {
        return answer;
    }

    public int getGuessesLeft() {
        return guessesLeft;
    }

}
