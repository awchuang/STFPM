package functions;

import static util.Utils.getMedian;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Trace;

public class writeResult {
	public List<Long> rangeRunTime;
	public List<Long> durationRunTime;
	public List<Long> candsRunTime;
	public List<Long> labelRunTime;
	public List<Long> rCheckRunTime;
	public List<Long> fstpmRunTime;
    private Trace logger;
	public String resultfile;
	
    public writeResult(){
    	logger = Trace.getLogger(this.getClass().getSimpleName());
    	this.rangeRunTime = new ArrayList<Long>();
		this.durationRunTime = new ArrayList<Long>();
		this.candsRunTime = new ArrayList<Long>();
		this.rCheckRunTime = new ArrayList<Long>();
		this.labelRunTime = new ArrayList<Long>();
		this.fstpmRunTime = new ArrayList<Long>();
		resultfile = "";
    }
    
	public void writeOutput(String fname, HashMap<List<String>, Integer> pattern) throws IOException{
		FileWriter output = new FileWriter(fname);			
        // sort hashmap by value (frequency)
		List<Map.Entry<List<String>, Integer>> list_Data = new ArrayList<Map.Entry<List<String>, Integer>>(pattern.entrySet());
        Collections.sort(list_Data, new Comparator<Map.Entry<List<String>, Integer>>(){
            public int compare(Map.Entry<List<String>, Integer> entry1,
                               Map.Entry<List<String>, Integer> entry2){
                return (entry2.getValue() - entry1.getValue());
            }
        });
        for (Map.Entry<List<String>, Integer> entry:list_Data) {
        	output.write(entry.getKey() + " : " + pattern.get(entry.getKey()) + "\n");
        }
		output.close();
		printResults();
	}
    
	public void printResults() {
		logger.trace("\nPerforming Run Time calculations..");

		fstpmRunTime.addAll(rangeRunTime);
		fstpmRunTime.addAll(durationRunTime);
		fstpmRunTime.addAll(candsRunTime);
		fstpmRunTime.addAll(labelRunTime);
		fstpmRunTime.addAll(fstpmRunTime);

		String result = "\n"+this.getClass().getSimpleName()+" --RESULTS--";

		String temp = "\n\nRange Search operations:(in milliseconds) "+ generateRuntimeReport(rangeRunTime);
        //logger.trace(temp);
        result += temp;
		temp = "\n\nDuration Check operations: (in milliseconds) " + generateRuntimeReport(durationRunTime);
        //logger.trace(temp);
        result += temp;
		temp = "\n\nCandidates Generation operations: (in milliseconds) " + generateRuntimeReport(candsRunTime);
        //logger.trace(temp);
        result += temp;
        temp = "\n\nRange Check Generation operations: (in milliseconds) " + generateRuntimeReport(rCheckRunTime);
        //logger.trace(temp);
        result += temp;
		temp = "\n\nCords to Label operations: (in milliseconds) " + generateRuntimeReport(labelRunTime);
        //logger.trace(temp);
        result += temp;
        temp = "\n\nFSTPM operations:(in milliseconds) "+ generateRuntimeReport(fstpmRunTime);
        //logger.trace( temp);
        result += temp;

		writeResultToFile(result, resultfile);
	}
	
	protected String generateRuntimeReport(List<Long> runtime) {
		StringBuilder result = new StringBuilder();
        int size = runtime.size();

        if (size > 0) {
            Collections.sort(runtime);
            try {
                Long percent5th = runtime.get((int) (0.05 * size));
                Long percent95th = runtime.get((int) (0.95 * size));
                float median = getMedian(runtime);
                long sum = 0;
                for (Long aRuntime : runtime) {
                    sum += aRuntime;
                }
                double avg = sum / (double) size;

                result.append("\nTotal ops = ").append(size);
                result.append("\nTotal time(in minutes) = ").append(sum/(1000*60));
                result.append("\nAvg time: ").append(avg);
                result.append("\n5th percentile: ").append(percent5th);
                result.append("\n95th percentile: ").append(percent95th);
                result.append("\nmedian: ").append(median);

            } catch (Exception e) {
                logger.traceError("Exception while generating runtime results");
                e.printStackTrace();
            }
        }

		return result.toString();
	}

	protected void writeResultToFile(String result, String fname) {
		try {
			File outFile = new File(fname);
			if(outFile.exists()){
				outFile.delete();
			}
			BufferedWriter outBW =  new BufferedWriter(new FileWriter(outFile));
			try{
				logger.trace("\nWriting results to file .. ");
				outBW.write(result);
			}
			finally{
				outBW.close();
				logger.trace("done");
			}
		} 
		catch (IOException e) {
			logger.traceError("IOException while writing results to " + fname);
		}
	}

}
