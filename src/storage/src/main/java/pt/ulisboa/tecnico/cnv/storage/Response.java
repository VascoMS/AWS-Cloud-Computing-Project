package pt.ulisboa.tecnico.cnv.storage;

public class Response {

    private String gameResult;
    private long complexityScore;

    public Response(String gameResult, long complexityScore) {
        this.gameResult = gameResult;
        this.complexityScore = complexityScore;
    }

    public String getGameResult() {
        return gameResult;
    }

    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
    }

    public long getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(long complexityScore) {
        this.complexityScore = complexityScore;
    }
}
