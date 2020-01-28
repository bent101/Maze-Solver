
import javafx.application.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.*;
import javafx.scene.paint.Color;
import java.io.*;
import java.util.*;
import javafx.scene.shape.Rectangle;
import javafx.animation.*;
import java.math.*;

public class Main extends Application {
	// globals
	private final short[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
	private final int WINDOW_SIZE = 820;
	
	private int n, m;
	private double w, h;
	
	private int startR, startC, endR, endC;
	
	private boolean[][] grid;
	private int distFromEnd[][];
	private BigInteger[][] numPaths;
	
	private Group root;
	
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		// set up stage root
		root = new Group();
		root.setLayoutX(10);
		root.setLayoutY(10);
		Rectangle border = new Rectangle(-5, -5, WINDOW_SIZE+10, WINDOW_SIZE+10);
		border.setStroke(Color.BLACK);
		border.setStrokeWidth(10);
		border.setFill(Color.grayRgb(200));
		root.getChildren().add(border);
		
		// read in maze
		Scanner in = new Scanner(new File("maze1.txt"));
		n = in.nextInt();
		m = in.nextInt();
		w = (double) WINDOW_SIZE / m;
		h = (double) WINDOW_SIZE / n;
		
		startR = 0;
		startC = 0;
		endR = n-1;
		endC = m-1;
		
		grid = new boolean[n][m];
		distFromEnd = new int[n][m];
		numPaths = new BigInteger[n][m];
		
		for(int r=0;r<n;r++) for(int c=0;c<m;c++) {
			distFromEnd[r][c] = Integer.MAX_VALUE;
			numPaths[r][c] = BigInteger.ZERO;
		}
		
		while(in.hasNextInt()) {
			grid[in.nextInt()][in.nextInt()] = true;
		}
		
		in.close();
		
		// get distance from each cell to the end 
		getDistFromEnd();
		int pathLength = distFromEnd[startR][startC];
		
		// get number of paths from start to each cell
		getNumPaths(new boolean[n][m]);
		
		// draw maze
		for(int r = 0; r < n; r++)
			for(int c = 0; c < m; c++)
				if(grid[r][c]) {
					Rectangle rect = new Rectangle(c*w, r*h, w+0.5, h+0.5);
					rect.setFill(Color.BLACK);
					root.getChildren().add(rect);
				}
		
		// wait 3 seconds
		PauseTransition pt = new PauseTransition();
		pt.setDuration(new Duration(3000));
		pt.setOnFinished(e -> {
			// print "the maze is not passable" if the maze is not passable
			if(pathLength == Integer.MAX_VALUE) {
				System.out.println("The maze is not passable.");
				return;
			}
			// print the number of paths from start to bottom-right
			String msg = numPaths[endR][endC].equals(BigInteger.ONE) ?
					"There is one path" : "There are " + numPaths[endR][endC] + " paths";
			System.out.println(msg + " of length " +
					pathLength + " going from (" +
					startR + ", " + startC + ") to (" + endR + ", " + endC + ")!");
			// display solution
			int timePerStep;
			if(pathLength == 0) timePerStep = 3000;
			else if(pathLength < 100) timePerStep = 3000 / pathLength;
			else timePerStep = 1;
			displaySolution(
					startR, startC, pathLength, pathLength, timePerStep, new boolean[n][m]);
		});
		
		// display scene
		Scene scene = new Scene(root, WINDOW_SIZE + 20, WINDOW_SIZE + 20);
		stage.setScene(scene);
		stage.show();
		// start animation
		pt.play();
	}
	
	
	// gets number of paths from start to each cell
	private void getNumPaths(boolean[][] seen) {
		numPaths[startR][startC] = BigInteger.ONE;
		Queue<Integer>
			rq = new LinkedList<>(),
			cq = new LinkedList<>();
		rq.add(startR); cq.add(startC);
		while(!rq.isEmpty()) {
			int r = rq.poll(), c = cq.poll();
			if(seen[r][c]) continue;
			seen[r][c] = true;
			for(short[] dir : dirs) {
				int adjr = r + dir[0], adjc = c + dir[1];
				if(!inBounds(adjr, adjc) || grid[adjr][adjc]) continue;
				if(seen[adjr][adjc]) {
					numPaths[r][c] = numPaths[r][c].add(numPaths[adjr][adjc]);
				} else {
					rq.add(adjr);
					cq.add(adjc);
				}
			}
			if(r == endR && c == endC) return;
		}
	}
	
	private void getDistFromEnd() {
		Queue<Integer>
			rq = new LinkedList<>(), // rows
			cq = new LinkedList<>(), // columns
			dq = new LinkedList<>();  // dist from start
		rq.add(endR); cq.add(endC); dq.add(0);
		while(!rq.isEmpty()) {
			int r = rq.poll(), c = cq.poll(), d = dq.poll();
			if(
					!inBounds(r, c) ||
					distFromEnd[r][c] != Integer.MAX_VALUE || // seen
					grid[r][c]) // in wall
				continue;
			distFromEnd[r][c] = d;
			for(short[] dir : dirs) {
				rq.add(r+dir[0]);
				cq.add(c+dir[1]);
				dq.add(d+1);
			}
		}
	}
	
	// displays solution recursively
	private void displaySolution(
			int r, int c, int d, int pathLength, int timePerStep, boolean[][] seen) {
		if(seen[r][c]) return;
		seen[r][c] = true;
		Rectangle rect = new Rectangle(c*w, r*h, w+0.5, h+0.5);
		rect.setFill(getColor(d, pathLength));
		root.getChildren().add(rect);
		
		for(short[] dir : dirs) {
			int newr = r + dir[0];
			int newc = c + dir[1];
			if(inBounds(newr, newc) && !seen[newr][newc] &&
			   distFromEnd[newr][newc] == d-1) {
				PauseTransition pt = new PauseTransition();
				pt.setDuration(new Duration(timePerStep));
				pt.setOnFinished(e -> {
					displaySolution(newr, newc, d-1, pathLength, timePerStep, seen);
				});
				pt.play();
				
				// returning here displays only one solution
				// continuing displays all possible solutions
				// return;
			}
		}
	}
	
	private Color getColor(int d, int pathLength) {
		return Color.hsb(270 - 150 * d/pathLength, 1.0, 1.0);
	}
	
	private boolean inBounds(int r, int c) {
		return r >= 0 && r < n && c >= 0 && c < m;
	}
}
