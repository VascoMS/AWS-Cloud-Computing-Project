package pt.ulisboa.tecnico.cnv.fifteenpuzzle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class FifteenPuzzle {
    private final int size;
    private final int[][] tiles;
    private TilePos blank;
    private final int displayWidth;

    private static class TilePos {
        final int x, y;
        TilePos(int x, int y) { this.x = x; this.y = y; }
    }

    public FifteenPuzzle(int size) {
        this.size = size;
        this.tiles = new int[size][size];
        int cnt = 1;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                tiles[i][j] = cnt++;
            }
        }
        tiles[size - 1][size - 1] = 0; // Blank space
        blank = new TilePos(size - 1, size - 1);
        displayWidth = Integer.toString(size * size).length();
    }

    private FifteenPuzzle(FifteenPuzzle other) {
        this.size = other.size;
        this.tiles = new int[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(other.tiles[i], 0, this.tiles[i], 0, size);
        }
        this.blank = new TilePos(other.blank.x, other.blank.y);
        this.displayWidth = other.displayWidth;
    }

    public void shuffle(int moves, Random random) {
        for (int i = 0; i < moves; i++) {
            List<TilePos> movesList = validMoves();
            TilePos move = movesList.get(random.nextInt(movesList.size()));
            makeMove(move);
        }
    }

    private List<TilePos> validMoves() {
        List<TilePos> moves = new ArrayList<>();
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] dir : directions) {
            int nx = blank.x + dir[0];
            int ny = blank.y + dir[1];
            if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                moves.add(new TilePos(nx, ny));
            }
        }
        return moves;
    }

    private void makeMove(TilePos p) {
        tiles[blank.x][blank.y] = tiles[p.x][p.y];
        tiles[p.x][p.y] = 0;
        blank = p;
    }

    private FifteenPuzzle moveClone(TilePos p) {
        FifteenPuzzle copy = new FifteenPuzzle(this);
        copy.makeMove(p);
        return copy;
    }

    public String getData() {
        StringBuilder sb = new StringBuilder();
        sb.append("-".repeat(size * (displayWidth + 3) + 1)).append("\n");
        for (int i = 0; i < size; i++) {
            sb.append("| ");
            for (int j = 0; j < size; j++) {
                String s = (tiles[i][j] == 0) ? "" : Integer.toString(tiles[i][j]);
                while (s.length() < displayWidth) s = " " + s;
                sb.append(s).append(" | ");
            }
            sb.append("\n");
        }
        sb.append("-".repeat(size * (displayWidth + 3) + 1)).append("\n");
        return sb.toString();
    }

    private boolean isSolved() {
        int cnt = 1;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == size - 1 && j == size - 1) {
                    if (tiles[i][j] != 0) return false;
                } else {
                    if (tiles[i][j] != cnt++) return false;
                }
            }
        }
        return true;
    }

    private int manhattanDistance() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int val = tiles[i][j];
                if (val != 0) {
                    int targetX = (val - 1) / size;
                    int targetY = (val - 1) % size;
                    sum += Math.abs(i - targetX) + Math.abs(j - targetY);
                }
            }
        }
        return sum;
    }

    public List<FifteenPuzzle> aStarSolve() {
        Map<FifteenPuzzle, FifteenPuzzle> predecessor = new HashMap<>();
        Map<FifteenPuzzle, Integer> gScore = new HashMap<>();
        PriorityQueue<FifteenPuzzle> openSet = new PriorityQueue<>(Comparator.comparingInt(p -> gScore.get(p) + p.manhattanDistance()));

        predecessor.put(this, null);
        gScore.put(this, 0);
        openSet.add(this);

        while (!openSet.isEmpty()) {
            FifteenPuzzle current = openSet.poll();
            if (current.isSolved()) {
                List<FifteenPuzzle> path = new ArrayList<>();
                while (current != null) {
                    path.add(0, current);
                    current = predecessor.get(current);
                }
                return path;
            }

            for (TilePos move : current.validMoves()) {
                FifteenPuzzle neighbor = current.moveClone(move);
                if (!gScore.containsKey(neighbor) || gScore.get(current) + 1 < gScore.get(neighbor)) {
                    predecessor.put(neighbor, current);
                    gScore.put(neighbor, gScore.get(current) + 1);
                    openSet.add(neighbor);
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FifteenPuzzle)) return false;
        FifteenPuzzle that = (FifteenPuzzle) o;
        return Arrays.deepEquals(this.tiles, that.tiles);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(tiles);
    }

    static String getSolutionData(List<FifteenPuzzle> solution) {

        if (solution == null) {
            return "No solution found.";
        }
        return "\nSolution found in " + (solution.size() - 1) + " moves.";
        // Optional: Uncomment to show each step of the solution
        //for (FifteenPuzzle step : solution) {
        //		sb.append(step.getData());
        //}
    }
}
