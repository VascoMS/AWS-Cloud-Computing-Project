package pt.ulisboa.tecnico.cnv;

import pt.ulisboa.tecnico.cnv.storage.StorageUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComplexityEstimator {
    private static final int MAX_ENTRIES = 5000;
    private static final double FIFTEEN_PUZZLE_SCALE_WEIGHT = 3.54;
    private static final double GAME_OF_LIFE_SCALE_WEIGHT = 1.84;

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

                double ninsts = -10098930362.82 + 119260738.56 * num_shuffles + 0 * num_size;
                double nmethod = -14828529.82 + 175108.42 * num_shuffles + 0 * num_size;
                double complexity = (ninsts / 682.3358) * 3.3 + nmethod;
                return Math.round(complexity);
            }

            case "capturetheflag": {
                double gridSize = Double.parseDouble(params.get("gridSize"));
                double numBlueAgents = Double.parseDouble(params.get("numBlueAgents"));
                double numRedAgents = Double.parseDouble(params.get("numRedAgents"));
                String flagPlacement = params.get("flagPlacementType");

                int cat_flagPlacementType_B = flagPlacement.equals("B") ? 1 : 0;
                int cat_flagPlacementType_C = flagPlacement.equals("C") ? 1 : 0;

                double ninsts = -188844810.08
                        + 8917467.24 * gridSize
                        + 8917467.24 * numBlueAgents
                        + 8917467.24 * numRedAgents
                        - 269574378.20 * cat_flagPlacementType_B
                        - 142751327.33 * cat_flagPlacementType_C;

                double nmethod = -3322991.36
                        + 156958.27 * gridSize
                        + 156958.27 * numBlueAgents
                        + 156958.27 * numRedAgents
                        - 4748919.53 * cat_flagPlacementType_B
                        - 2518286.13 * cat_flagPlacementType_C;

                double complexity = (ninsts / 56.9413) * 2.72 + nmethod;
                return Math.round(complexity);
            }

            case "gameoflife": {
                double iterations = Double.parseDouble(params.get("iterations"));

                double ninsts = 3870 + 54698 * iterations;
                double nmethod = 11.00 + 101.00 * iterations;
                double complexity = (ninsts / 541.5605) * 7.85 + nmethod;
                return Math.round(complexity);
            }

            default:
                throw new IllegalArgumentException("Unsupported game: " + game);
        }
    }

}
