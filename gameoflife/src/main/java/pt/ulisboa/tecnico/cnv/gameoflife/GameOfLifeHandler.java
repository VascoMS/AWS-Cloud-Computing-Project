package pt.ulisboa.tecnico.cnv.gameoflife;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class GameOfLifeHandler implements HttpHandler, RequestHandler<Map<String, String>, String> {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Input (request) model.
     */
    private static class GameOfLifeRequest {
        public int iterations;
        public int[][] map;

        public GameOfLifeRequest(int iterations, int[][] map) {
            this.iterations = iterations;
            this.map = map;
        }

        public GameOfLifeRequest() {}
    }

    /**
     * Output (response) model.
     */
    private static class GameOfLifeResponse {
        public int[][] map;

        public GameOfLifeResponse(int[][] map) {
            this.map = map;
        }

        public GameOfLifeResponse() {}
    }

    /**
     * Game entrypoint.
     */
    private String handleWorkload(byte[] map, int width, int height, int iterations) {
        GameOfLife gol = new GameOfLife(width, height, map);
        gol.play(iterations);
        byte[] resultData = gol.getData();

        int[][] resultMap = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                resultMap[i][j] = Byte.toUnsignedInt(resultData[i * width + j]);
            }
        }
        GameOfLifeResponse response = new GameOfLifeResponse(resultMap);

        try {
            return MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{ \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Entrypoint or HTTP requests.
     */
    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS.
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if ("OPTIONS".equalsIgnoreCase(he.getRequestMethod())) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        InputStream requestBody = he.getRequestBody();
        String jsonBody = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        GameOfLifeRequest request = null;
        try {
            request = MAPPER.readValue(jsonBody, GameOfLifeRequest.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            String errorResponse = "{ \"error\":\"" + e.getMessage() + "\"}";
            he.sendResponseHeaders(400, errorResponse.length());
            OutputStream os = he.getResponseBody();
            os.write(errorResponse.getBytes());
            os.close();
            return;
        }

        int rows = request.map.length;
        int cols = (rows > 0) ? request.map[0].length : 0;
        byte[] map = convertMapToByteArray(request.map, rows, cols);

        String response = handleWorkload(map, cols, rows, request.iterations);

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * Entrypoint for AWS Lambda.
     */
    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        int iterations = Integer.parseInt(event.get("iterations"));
        String jsonMap = event.get("map");
        int[][] intMap = MAPPER.convertValue(jsonMap, int[][].class);

        int rows = intMap.length;
        int cols = (rows > 0) ? intMap[0].length : 0;
        byte[] map = convertMapToByteArray(intMap, rows, cols);

        return handleWorkload(map, cols, rows, iterations);
    }

    /**
     * For debugging use - to run from CLI.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler <map_json_filename> [<iterations>]");
            return;
        }
        String jsonFilename = args[0];

        GameOfLifeRequest input;
        try {
            input = MAPPER.readValue(new File(jsonFilename), GameOfLifeRequest.class);
        } catch (IOException e) {
            System.err.println("Provide a valid JSON file as input. It should contain a 2D map of integers (field \"map\") and a number of iterations (field \"iterations\").");
            System.exit(1);
            return; // redundant but needed to avoid null-check warning.
        }

        int iterations = 0;
        try {
            // Command-line-provided iterations argument overrides the value from JSON.
            iterations = (args.length > 1) ? Integer.parseInt(args[1]) : input.iterations;
        } catch (NumberFormatException e) {
            System.err.println("The optional \"iterations\" argument should be a valid integer value.");
            System.exit(1);
        }

        int rows = input.map.length;
        int cols = (rows > 0) ? input.map[0].length : 0;
        byte[] map = convertMapToByteArray(input.map, rows, cols);

        GameOfLife gol = new GameOfLife(cols, rows, map);
        gol.playCLI(iterations);
    }

    /**
     * Util method to convert a 2d int array to a byte array.
     */
    private static byte[] convertMapToByteArray(int[][] map, int rows, int cols) {
        byte[] byteArray = new byte[rows * cols];

        int index = 0;
        for (int[] row : map) {
            for (int cell : row) {
                byteArray[index++] = (byte) cell;
            }
        }

        return byteArray;
    }
}
