package pt.ulisboa.tecnico.cnv.storage;

import pt.ulisboa.tecnico.cnv.javassist.model.Statistics;

public class Response {

    private String gameResult;
    private Statistics requestStatistics;


    public Response(String gameResult, Statistics requestStatistics) {
        this.gameResult = gameResult;
        this.requestStatistics = requestStatistics;
    }

    public String getGameResult() {
        return gameResult;
    }

    public Statistics getRequestStatistics() {
        return requestStatistics;
    }
}
