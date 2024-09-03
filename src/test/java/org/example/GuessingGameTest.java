package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuessingGameTest {
    private GuessingGame guessingGame;

    @BeforeEach
    void setup() {
        guessingGame = new GuessingGame();
    }

    @Test
    void setAnswerLetsYouSetAnswerTo5() {
        guessingGame.setAnswer(5);
        assertEquals(5, guessingGame.getAnswer());
    }

    @Test
    void setAnswerLetsYouSetAnswerTo10() {
        var guessingGame = new GuessingGame();
        guessingGame.setAnswer(10);
        assertEquals(10, guessingGame.getAnswer());
    }

    @Test
    void guessesLeftStartsAt7() {
        assertEquals(7, guessingGame.getGuessesLeft());
    }

    @Test
    void handleGuessDecreasesTheGuessesLeftBy1() {
        guessingGame.handleGuess(30);
        assertEquals(6, guessingGame.getGuessesLeft());
    }

    @Test
    void handleGuessReturns20isTooHigh() {
        guessingGame.setAnswer(10);
        assertEquals("20 is too high. Try again.", guessingGame.handleGuess(20));
    }

    @Test
    void handleGuessReturns30isTooHigh() {
        guessingGame.setAnswer(10);
        assertEquals("30 is too high. Try again.", guessingGame.handleGuess(30));
    }

    @Test
    void handleGuessReturns5TooLow() {
        guessingGame.setAnswer(10);
        assertEquals("5 is too low. Try again.", guessingGame.handleGuess(5));
    }

    @Test
    void handleGuessReturns8TooLow() {
        guessingGame.setAnswer(10);
        assertEquals("8 is too low. Try again.", guessingGame.handleGuess(8));
    }

    @Test
    void handleGuessReturnsCongratulationsIf10isTheAnswer() {
        guessingGame.setAnswer(10);
        assertEquals("You guessed it! 10 is the number!", guessingGame.handleGuess(10));
    }

    @Test
    void handleGuessReturnsCongratulationsIf15isTheAnswer() {
        guessingGame.setAnswer(15);
        assertEquals("You guessed it! 15 is the number!", guessingGame.handleGuess(15));
    }

    @Test
    void handleGuessReturnsLoseIf7WrongAnswers(){
        guessingGame.setAnswer(10);
        for (int i = 0; i < 6; i++) {
            guessingGame.handleGuess(i);
        }
        assertEquals("10 was the correct answer. You lose.", guessingGame.handleGuess(6));
    }

    @Test
    void handleGuessReturnsLoseWithProperNumberIf7WrongAnswers(){
        guessingGame.setAnswer(15);
        for (int i = 0; i < 6; i++) {
            guessingGame.handleGuess(i);
        }
        assertEquals("15 was the correct answer. You lose.", guessingGame.handleGuess(6));
    }

    @Test
    void handleGuessReturnsWinIf7thAnswerCorrect(){
        guessingGame.setAnswer(10);
        for (int i = 0; i < 6; i++) {
            guessingGame.handleGuess(i);
        }
        assertEquals("You guessed it! 10 is the number!", guessingGame.handleGuess(10));
    }

}
