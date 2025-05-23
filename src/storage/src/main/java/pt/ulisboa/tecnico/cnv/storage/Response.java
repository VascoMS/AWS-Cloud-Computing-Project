package pt.ulisboa.tecnico.cnv.storage;

import pt.ulisboa.tecnico.cnv.javassist.model.Statistics;

public class Response {

    private String gameResult;
    private Statistics complexityScore;

    public Response(String gameResult, Statistics complexityScore) {
        this.gameResult = gameResult;
        this.complexityScore = complexityScore;
    }

    public String getGameResult() {
        return gameResult;
    }

    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
    }

    public Statistics getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(Statistics complexityScore) {
        this.complexityScore = complexityScore;
    }
}
