package pt.ulisboa.tecnico.cnv.javassist.model;


import java.util.Map;

public class Statistics {
    /**
     * Number of executed basic blocks.
     */
    private long nblocks;

    /**
     * Number of executed methods.
     */
    private long nmethod;

    /**
     * Number of executed instructions.
     */
    private long ninsts;

    /**
     * Number of data accesses.
     */
    private long ndataWrites = 0;
    private long ndataReads = 0;

    private long complexity = 0;

    public Statistics(){
        nblocks = 0;
        nmethod = 0;
        ninsts = 0;
    }

    public long getNblocks(){
        return nblocks;
    }

    public long getNmethod(){
        return nmethod;
    }

    public long getNinsts(){
        return ninsts;
    }

    public long getNdataWrites(){return ndataWrites;}

    public long getNdataReads(){return ndataReads;}

    public long getComplexity(){return complexity;}

    public void incrementNblocks(){
        nblocks++;
    }
    public void incrementNmethod(){
        nmethod++;
    }
    public void incrementNinsts(int length){
        ninsts += length;
    }
    public void incrementNdataWrites(){ndataWrites++;}
    public void incrementNdataReads(){ndataReads++;}
    public long computeComplexity(String game) {
        return computeComplexity(game, this.nmethod, this.ndataReads);
    }

    public static long computeComplexity(String game, long nmethod, long ndataReads) {
        if (game.equals("fifteenpuzzle")){
            return Math.round((ndataReads / 44.056) * 1.56 + nmethod);
        } else if (game.equals("capturetheflag")){
            return Math.round((ndataReads / 2.6133) * 3.8 + nmethod);
        } else {
            return Math.round((ndataReads / 39.8408) * 8.83 + nmethod);
        }
    }
}
