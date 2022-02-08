import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.*;

public class Main {
	public static void main(String[] args) throws Exception {
		new Game();
	}
}

class Game {
	static int WORD_SIZE = 5;
	static int NUM_GUESSES = 6;
	static int WINDOW_WIDTH = WORD_SIZE * 60 + 20;
	static int WINDOW_HEIGHT = NUM_GUESSES * 60 + 20 + 100;

	JFrame window;
	GameLogic gameLogic;
	Panel[][] slots;
	JLabel[][] slotLabels;
	int guess = 0;
	int charIndex = 0;
	Panel resultPanel;
	JLabel resultLabel;

	List<String> wordleWords = new ArrayList<String>();
	List<String> allowedWords = new ArrayList<String>();
	
    Random randomNumberGenerator = new Random();

	public Game() throws Exception {
		loadWords();
		initWindow();
		initGrid();
		initStatus();
		resetGame();
		registerEvents();
	}

	void loadWords() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("wordlewords.txt"));
        
        while (true) {
            String line = br.readLine();

			if (line == null) {
				break;
			}

			wordleWords.add(line.trim());
		}

		br.close();

		br = new BufferedReader(new FileReader("allowable.txt"));
        
        while (true) {
            String line = br.readLine();

			if (line == null) {
				break;
			}

			allowedWords.add(line.trim());
		}

		br.close();
	}

	void initWindow() {
		window = new JFrame();
		window.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		window.setTitle("Wordle Clone");
		window.setResizable(false);
		window.setLayout(null);
	}

	void initGrid() {
		slots = new Panel[NUM_GUESSES][WORD_SIZE];
		slotLabels = new JLabel[NUM_GUESSES][WORD_SIZE];

		for (int i = 0; i < NUM_GUESSES; i++) {
			for (int j = 0; j < WORD_SIZE; j++) {
				Panel p = new Panel();
				JLabel l = new JLabel("");
				slots[i][j] = p;
				slotLabels[i][j] = l;
				l.setFont(new Font("SansSerif", Font.BOLD, 18));
				l.setHorizontalAlignment(SwingConstants.CENTER);
				l.setVerticalAlignment(SwingConstants.CENTER);
				p.setBackground(Color.LIGHT_GRAY);
				p.setBounds(10 + 60 * j, 10 + 60 * i, 50, 50);
				p.add(l);
				window.add(p);
			}
		}
	}

	void initStatus() {
		resultPanel = new Panel();
		resultLabel = new JLabel("Take a turn!");
		resultLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
		resultLabel.setForeground(Color.MAGENTA);
		resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
		resultLabel.setVerticalAlignment(SwingConstants.CENTER);
		resultPanel.setBounds(10, 380, 290, 50);
		resultPanel.setLayout(new BorderLayout());
		resultPanel.add(resultLabel);
		window.add(resultPanel);
		window.setVisible(true);
	}

	void resetGame() {
		gameLogic = new GameLogic(getRandomElement(wordleWords), NUM_GUESSES);
		guess = 0;
		charIndex = 0;
	}

	void registerEvents() {
		window.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				inputChar(c);
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 8) { // backspace
					deleteChar();
				} else if (e.getKeyCode() == 10) { // enter
					submitGuess();
				}

				switch (gameLogic.getState()) {
					case WIN:
						resultLabel.setText("You Win!");
						resultLabel.setForeground(Color.GREEN);
						resultPanel.setBackground(Color.BLACK);
					break;
					case LOSE:
						resultLabel.setText("You Lose! [" + gameLogic.getSolution() + "]");
						resultLabel.setForeground(Color.RED);
						resultPanel.setBackground(Color.WHITE);
					break;
					default:
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
	}

	void inputChar(char c) {
		if (gameLogic.getState() != GameState.IN_PROGRESS) {
			return;
		}

		if (c >= 'a' && c <= 'z') {
			if (charIndex < WORD_SIZE) {
				slotLabels[guess][charIndex].setText(("" + c).toUpperCase());
				charIndex++;
			}
		}
	}

	void deleteChar() {
		if (gameLogic.getState() != GameState.IN_PROGRESS) {
			return;
		}

		if (charIndex > 0) {
			charIndex--;
			slotLabels[guess][charIndex].setText("");
		}
	}

	void submitGuess() {
		if (gameLogic.getState() != GameState.IN_PROGRESS) {
			return;
		}

		String guessValue = "";

		for (int i = 0; i < WORD_SIZE; i++) {
			guessValue += slotLabels[guess][i].getText();
		}

		guessValue = guessValue.toLowerCase();

		if (!allowedWords.contains(guessValue)) {
			resultLabel.setText("Word not allowed! [" + guessValue + "]");
			resultLabel.setForeground(Color.WHITE);
			resultPanel.setBackground(Color.DARK_GRAY);
			return;
		}

		LetterStatus[] hints = gameLogic.inputGuess(guessValue);

		for (int i = 0; i < hints.length; i++) {
			LetterStatus hint = hints[i];

			switch (hint) {
				case IN_SOLUTION_EXACT_LOCATION:
					slots[guess][i].setBackground(Color.GREEN);
				break;
				case IN_SOLUTION_SOMEWHERE:
					slots[guess][i].setBackground(Color.ORANGE);
				break;
				case NOT_IN_SOLUTION:
					slots[guess][i].setBackground(Color.DARK_GRAY);
					slotLabels[guess][i].setForeground(Color.LIGHT_GRAY);
				break;
				default:
			}
		}

		guess++;
		charIndex = 0;
	}

	void update() {
	}

	String getRandomElement(List<String> elements) {
        int index = randomNumberGenerator.nextInt(elements.size());
        return elements.get(index);
    }
}

enum GameState {
	IN_PROGRESS,
	WIN,
	LOSE,
	__
}

enum LetterStatus {
	NOT_IN_SOLUTION,
	IN_SOLUTION_SOMEWHERE,
	IN_SOLUTION_EXACT_LOCATION,
	__
}

class GameLogic {
	private String solution;
	private int guessesLeft;
	private GameState state;

	public GameLogic(String solution, int guesses) {
		this.solution = solution;
		guessesLeft = guesses;
		state = GameState.IN_PROGRESS;
	}

	public LetterStatus[] inputGuess(String guess) {
		if (guessesLeft-- < 0) {
			return null;
		}

		int length = Math.min(solution.length(), guess.length());
		LetterStatus[] hints = new LetterStatus[length];

		for (int i = 0; i < length; i++) {
			char c = guess.charAt(i);

			if (solution.charAt(i) == c) {
				hints[i] = LetterStatus.IN_SOLUTION_EXACT_LOCATION;
			} else {
				hints[i] = LetterStatus.NOT_IN_SOLUTION;
			}
		}

		Map<Character, Integer> characterOccurencesLeft = new HashMap<Character, Integer>(); 

		for (int i = 0; i < solution.length(); i++) {
			char c = solution.charAt(i);

			if (characterOccurencesLeft.containsKey(c)) {
				continue;
			}

			int correctOccurencesInGuess = 0;

			for (int j = 0; j < guess.length(); j++) {
				if (guess.charAt(j) == c && solution.charAt(j) == c) {
					correctOccurencesInGuess++;
				}
			}

			int occurencesInSolution = solution.length() - solution.replace("" + c, "").length();
			int occurencesLeft = occurencesInSolution - correctOccurencesInGuess;
			characterOccurencesLeft.put(c, occurencesLeft);
		}

		for (char c : characterOccurencesLeft.keySet()) {
			int occurencesLeft = characterOccurencesLeft.get(c);
			int i = 0;

			while (occurencesLeft > 0 && i < guess.length()) {
				if (guess.charAt(i) == c && hints[i] != LetterStatus.IN_SOLUTION_EXACT_LOCATION) {
					hints[i] = LetterStatus.IN_SOLUTION_SOMEWHERE;
					occurencesLeft--;
				}

				i++;
			}
		}

		if (guess.equals(solution)) {
			state = GameState.WIN;
		} else if (guessesLeft == 0) {
			state = GameState.LOSE;
		}

		return hints;
	}

	public int getGuessesLeft() {
		return guessesLeft;
	}

	public GameState getState() {
		return state;
	}

	public int getSolutionSize() {
		return solution.length();
	}

	public String getSolution() {
		return solution;
	}
}
