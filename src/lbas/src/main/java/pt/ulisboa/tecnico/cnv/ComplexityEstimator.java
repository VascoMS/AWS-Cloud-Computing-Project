package pt.ulisboa.tecnico.cnv;

import pt.ulisboa.tecnico.cnv.storage.StorageUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComplexityEstimator {
    private static final int MAX_ENTRIES = 5000;
    private static final double FIFTEEN_PUZZLE_SCALE_WEIGHT = 6.2;
    private static final double GAME_OF_LIFE_SCALE_WEIGHT = 15.1;

    public record ComplexityEstimate(long value, boolean storeMetrics) {}

    // Thread-safe LRU Cache
    private final Map<String, Long> localCache = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_ENTRIES;
                }
            }
    );

    public ComplexityEstimate estimateComplexity(String game, Map<String, String> params) {
        String cacheKey = StorageUtil.serializeParameters(params);

        // Try local cache first
        Long complexity = localCache.get(cacheKey);
        if (complexity != null) {
            return new ComplexityEstimate(normalizeComplexity(game, complexity), false);
        }

        // Try fetching from storage
        complexity = fetchFromStorage(game, params);
        if (complexity != null) {
            localCache.put(cacheKey, complexity);
            return new ComplexityEstimate(normalizeComplexity(game, complexity), false);
        }
        // Fallback to approximation
        complexity = approximateComplexityFromParams(game, params);
        return new ComplexityEstimate(normalizeComplexity(game, complexity), true);
    }

    private Long normalizeComplexity(String game, Long complexity) {
        if(game.equals("FifteenPuzzle"))
            return Math.round(complexity * FIFTEEN_PUZZLE_SCALE_WEIGHT);
        else if (game.equals("GameOfLife"))
            return Math.round(complexity * GAME_OF_LIFE_SCALE_WEIGHT);
        return complexity;
    }

    private Long fetchFromStorage(String game, Map<String, String> params) {
        try {
            return StorageUtil.getMetrics(game, StorageUtil.serializeParameters(params));
        } catch (Exception e) {
            System.err.println("Error fetching complexity from storage: " + e.getMessage());
            return null;
        }
    }

    private Long approximateComplexityFromParams(String game, Map<String, String> params) {
        switch (game.toLowerCase()) {
            case "fifteenpuzzle": {
                double num_shuffles = Double.parseDouble(params.get("shuffles"));
                double num_size = Double.parseDouble(params.get("size"));

                double nDataReads = -14449130649.65 + 34439917.01 * num_shuffles + 1180278544.33 * num_size;
                double nmethod = -276136068.04 + 1031843.96 * num_shuffles + 19322036.43 * num_size;
                double complexity = (nDataReads / 44.0560) * 1.56 + nmethod;
                return Math.round(complexity);
            }

            case "capturetheflag": {
                double gridSize = Double.parseDouble(params.get("gridSize"));
                double numBlueAgents = Double.parseDouble(params.get("numBlueAgents"));
                double numRedAgents = Double.parseDouble(params.get("numRedAgents"));
                String flagPlacement = params.get("flagPlacementType");

                int cat_flagPlacementType_B = flagPlacement.equals("B") ? 1 : 0;
                int cat_flagPlacementType_C = flagPlacement.equals("C") ? 1 : 0;

                double nDataReads = -8741285.47
                        + 413503.27 * gridSize
                        + 413503.27 * numBlueAgents
                        + 413503.27 * numRedAgents
                        - 12534137.37 * cat_flagPlacementType_B
                        - 6687443.83 * cat_flagPlacementType_C;

                double nmethod = -3322991.17
                        + 156958.27 * gridSize
                        + 156958.27 * numBlueAgents
                        + 156958.27 * numRedAgents
                        - 4748919.43 * cat_flagPlacementType_B
                        - 2518286.07 * cat_flagPlacementType_C;

                double complexity = (nDataReads / 2.6133) * 3.8 + nmethod;
                return Math.round(complexity);
            }

            case "gameoflife": {
                double iterations = Double.parseDouble(params.get("iterations"));

                double nDataReads = 4.00 + 4024.00 * iterations;
                double nmethod = 11.00 + 101.00 * iterations;
                double complexity = (nDataReads / 39.8408) * 8.83 + nmethod;
                return Math.round(complexity);
            }

            default:
                throw new IllegalArgumentException("Unsupported game: " + game);
        }
    }

}
