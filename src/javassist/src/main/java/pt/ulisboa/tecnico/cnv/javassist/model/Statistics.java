package pt.ulisboa.tecnico.cnv.javassist.model;

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
    public long computeComplexity(){return complexity = ninsts + 2*ndataReads +3*ndataWrites + 5*nmethod;}
}
