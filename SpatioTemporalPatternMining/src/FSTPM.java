import static util.Utils.getMedian;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import functions.Version1;
import functions.operations;
import arbor.mining.rtree.rtree.SpatialPoint;
import util.Trace;

public class FSTPM {
	private RTreeBuilder rbuilder;
    private double range;
    private int duration;
	private String inputFile;
	private String outputFile;
	private String resultFile;
	private List<Long> insertRunTime;
	private List<Long> rangeRunTime;
	private List<Long> durationRunTime;
	private List<Long> candsRunTime;
	private List<Long> labelRunTime;
	private List<Long> rCheckRunTime;
	private List<Long> fstpmRunTime;
    private Trace logger;
    private Version1 alg1;
    

    public static void main(String[] args) {
    	FSTPM controller = new FSTPM(args);
    	
		System.out.println("Reading input file ...");
		// construct r-tree
		controller.processInput();
		System.out.println("Finished Processing file ...");
		
		controller.patternExtraction();

		/*
        controller.writeRuntimeToFile(controller.insertRunTime, "Insertion_runtime.txt");
        controller.writeRuntimeToFile(controller.rangeRunTime, "RangeSearch_runtime.txt");
        controller.writeRuntimeToFile(controller.durationRunTime, "DurationCheck_runtime.txt");
        controller.writeRuntimeToFile(controller.candsRunTime, "Candidates_runtime.txt");
        controller.writeRuntimeToFile(controller.candsRunTime, "rCheck_runtime.txt");
        controller.writeRuntimeToFile(controller.labelRunTime, "LabelPattern_runtime.txt");
        controller.writeRuntimeToFile(controller.fstpmRunTime, "FSTPM_runtime.txt");
        */

		controller.printResults();
	}

	public FSTPM(String[] args) {
		if(args.length == 3){
			this.inputFile = args[0];
            this.range = Double.parseDouble(args[1]);
            this.duration = Integer.parseInt(args[2]);
            
            String[] fName = args[0].split("\\.");
            this.resultFile = this.getClass().getSimpleName() + "_" + fName[0] + "r" + this.range + "d" + this.duration + "_Results.txt";
            outputFile = this.getClass().getSimpleName() + "_" + fName[0] + "_r" + this.range + "t" + this.duration + "_output.txt";

		} else {
			this.printUsage();
			System.exit(1);
		}
		
		rbuilder = new RTreeBuilder();
		alg1 = new Version1();
		
		this.insertRunTime = new ArrayList<Long>();
		this.rangeRunTime = new ArrayList<Long>();
		this.durationRunTime = new ArrayList<Long>();
		this.candsRunTime = new ArrayList<Long>();
		this.rCheckRunTime = new ArrayList<Long>();
		this.labelRunTime = new ArrayList<Long>();
		this.fstpmRunTime = new ArrayList<Long>();
        logger = Trace.getLogger(this.getClass().getSimpleName());
	}

	protected void processInput() {
        int oid;
        float longitude, latitude;
        double x, y;
        String label;
        int time;
        long start, end;
        int lineNum = 0;

        try {
            BufferedReader input =  new BufferedReader(new FileReader(this.inputFile));
            String line;
            String[] lineSplit;
			
        	while ((line = input.readLine()) != null) {
				lineNum++;
                lineSplit = line.split(",");

                //insertion
				try {
                    oid = Integer.parseInt(lineSplit[0]);
                    latitude = Float.parseFloat(lineSplit[1]);
                    longitude = Float.parseFloat(lineSplit[2]);
                    label = lineSplit[3];
                    time = Integer.parseInt(lineSplit[4]);

                    x = alg1.Distance(0.0, 0.0, latitude, 0.0);
                    y = alg1.Distance(0.0, 0, 0, longitude);
                    
                    start = System.currentTimeMillis();
                    rbuilder.insert(x, y, oid, label, time);
                    end = System.currentTimeMillis();

                    insertRunTime.add(end - start);

                } catch (Exception e) {
                    logger.traceError("Exception while processing line " + lineNum +
                            ". Skipped Insertion. message: "+e.getMessage());
                    break;
                }
                catch (AssertionError error){
                    logger.traceError("Error while processing line " + lineNum +
                            ".Skipped Insertion. message: "+ error.getMessage());
                    break;
                }
			}
			input.close();
		}
		catch (Exception e) {
			logger.traceError("Error while reading input file. Line " + lineNum + " Skipped\nError Details:");
		}
	}

	protected void printResults() {
		logger.trace("\nPerforming Run Time calculations..");

		fstpmRunTime.addAll(insertRunTime);
		fstpmRunTime.addAll(rangeRunTime);
		fstpmRunTime.addAll(durationRunTime);
		fstpmRunTime.addAll(candsRunTime);
		fstpmRunTime.addAll(labelRunTime);
		fstpmRunTime.addAll(fstpmRunTime);

		String result = "\n"+this.getClass().getSimpleName()+" --RESULTS--";

		String temp = "\n\nInsertion operations:(in milliseconds) "+ generateRuntimeReport(insertRunTime);
        //logger.trace(temp);
        result += temp;
		temp = "\n\nRange Search operations:(in milliseconds) "+ generateRuntimeReport(rangeRunTime);
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

		writeResultToFile(result);
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

	protected void writeResultToFile(String result) {
		try {
			File outFile = new File(this.resultFile);
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
			logger.traceError("IOException while writing results to " + resultFile);
		}
	}

	protected void writeRuntimeToFile(List<Long> runtime, String file) {
		try {
			File f = new File(file);
			if(f.exists())
				f.delete();

			BufferedWriter bf =  new BufferedWriter(new FileWriter(f));
			for(long i : runtime){
				bf.write("" + i + "\n");
			}
			bf.close();
		} catch (IOException e) {
            logger.traceError("IOException while writing runtimes to file.");
//            e.printStackTrace();
        }
	}

	protected void printUsage() {
		System.err.println("Usage: "+ this.getClass().getSimpleName() +
                " <path to input file> <range(km)> <duration(mins)>.\n");
	}
	
	//////////////// r-tree///////////////////
	
	
	//////////////// FSTPM ///////////////////
	protected void patternExtraction(){
		int oid;
        long start, end;
        long startrc, startlb, endrc, endlb;
        int lineNum = 0;
		HashMap<List<String>, Integer> result = new HashMap<List<String>, Integer>();
        
		
		try{
			BufferedReader input =  new BufferedReader(new FileReader(this.inputFile));
			String line;
			String[] lineSplit;
			List<SpatialPoint> EpNeighbor = new ArrayList<SpatialPoint>();
			List<List<SpatialPoint>> stPatterns = new ArrayList<List<SpatialPoint>>();
			List<List<SpatialPoint>> FstPatterns = new ArrayList<List<SpatialPoint>>();
			
			line = input.readLine();
			// For all nodes
			while ((line = input.readLine()) != null) {
				lineNum++;
                lineSplit = line.split(",");
                
                try{
                	// Pick one event to be pivot : Ep
                    oid = Integer.parseInt(lineSplit[0]);
                    SpatialPoint Ep = rbuilder.idMap.get(oid);
                    
                    System.out.println("\nprocessing node " + oid);

                    // Find all neighbors within 2R : EpNeighbor
                    System.out.println("Range search begin...");
                    start = System.currentTimeMillis();
                    double[] coord = Ep.getCords();
                    EpNeighbor = rbuilder.rangeQuery(coord[0],coord[1], this.range*2, this.range*2);
                    end = System.currentTimeMillis();  
                    rangeRunTime.add(( end - start ));  
                                       

                    // First filtering : remove nodes those time duration > T 
                    System.out.println("Duration check begin...");
                    start = System.currentTimeMillis();
                    EpNeighbor = alg1.durationCheck(EpNeighbor, Ep.getTime(), oid, duration);
                    end = System.currentTimeMillis();                    
                    System.out.println("There are "+EpNeighbor.size()+" points found from "+rbuilder.getRtreeSize());

                    durationRunTime.add(( end - start ));  


                    // Generate all possible pattern : patterns
                    System.out.println("Cands generation begin...");
                    if(EpNeighbor.size() > 1){
                        start = System.currentTimeMillis();
                    	stPatterns = alg1.patternExtraction(EpNeighbor); 
            		
                    	startrc = System.currentTimeMillis();
                    	// Second filtering : check whether all nodes in one candidate are located within range R
                    	
                    	/*int[] flag = new int[stPatterns.size()];
                    	for(int i = 0; i < stPatterns.size(); i++)
                    		flag[i] = 0;
                    	
                    	for(int i = 0; i < stPatterns.size(); i++){
                    		if(flag[i] == 0){
	                    		if(alg1.rangeCheck(stPatterns.get(i), range) == true){
	                    			FstPatterns.add(stPatterns.get(i));
	                    			flag[i] = 1;
	                    			
	                    			for(int k =i ; k < stPatterns.size(); k++){
	                    				List<SpatialPoint> tmp = new ArrayList<SpatialPoint>(stPatterns.get(i));
	                    				
	                    				tmp.retainAll(stPatterns.get(k));
	                    				if(k != i && tmp.equals(stPatterns.get(k))){
	                    						FstPatterns.add(tmp);
	                    						flag[k] = 1;	
	                    						System.out.println("k=" + k);                    						
	                    				}
	                    			}
	                    		}
                    		}

                    	}*/
                    	
                    	/*while(stPatterns.isEmpty() == false){
                    		if(alg1.rangeCheck(stPatterns.get(0), range) == true){
                    			FstPatterns.add(new ArrayList<SpatialPoint>(stPatterns.get(0)));

                    			for(int k = 1; k < stPatterns.size(); k++){
            						//System.out.println("aa=" + stPatterns.get(0));
                    				List<SpatialPoint> tmp = new ArrayList<SpatialPoint>(stPatterns.get(0));
            						//System.out.println("tmp=" + tmp);
                    				
                    				tmp.retainAll(stPatterns.get(k));
                    				//System.out.println(tmp.retainAll(stPatterns.get(k)));
            						//System.out.println("tmp=" + tmp);
            						//System.out.println("st=" + stPatterns.get(k));
            						//System.out.println("k=" + k);
                    				if(tmp.equals(stPatterns.get(k))){
                						//System.out.println("true");
                    						FstPatterns.add(tmp);
                    						stPatterns.remove(k);
                    						k--;
                    						
                    				}
                    			}
                    		}
                    		stPatterns.remove(0);
                    	}*/
                    	
                    	for(int i = 0; i < stPatterns.size(); i++){
                    		if(alg1.rangeCheck(stPatterns.get(i), range) == true){
                    			FstPatterns.add(stPatterns.get(i));
                    		}
                    	}
                    	
                    	endrc = System.currentTimeMillis();  
                        rCheckRunTime.add(endrc - startrc);                  
                    	                    	
                        System.out.println("Cords to Label begin...");
                        startlb = System.currentTimeMillis();
                    	// change node pattern to label pattern and count frequency
                    	alg1.cordsToLabel(FstPatterns, result);
                    	endlb = System.currentTimeMillis();      
                        labelRunTime.add(endlb - startlb);              
                        
                        
                        end = System.currentTimeMillis();     
                        candsRunTime.add(( (end - start) -(endrc - startrc) -(endlb - startlb) ));
                    }              
                    
                }
                catch (Exception e) {
                    logger.traceError("Exception while processing line " + lineNum +
                            ". Skipped range search. message: "+e.getMessage());
                }
                catch (AssertionError error){
                    logger.traceError("Error while processing line " + lineNum +
                            ". Skipped range search. message: "+error.getMessage());
                }
                EpNeighbor.clear();
                stPatterns.clear();
                FstPatterns.clear();
			}			
			
			writeResult(outputFile, result);
			
			input.close();
		}
		catch (Exception e) {
			logger.traceError("Error while reading input file. Line " + lineNum + " Skipped\nError Details:");
		}
	}
	
	private void writeResult(String fname, HashMap<List<String>, Integer> pattern) throws IOException{
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
	}
	
}



