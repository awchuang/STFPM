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

import algorithms.Version1;
import arbor.mining.rtree.rtree.LeafEntry;
import arbor.mining.rtree.rtree.SpatialPoint;
import util.Trace;

public class FSTPM {
	//private RStarTree tree;
	private RTreeBuilder rbuilder;
	private Version1 alg1;
    private int dimension;
    private double range;
    private int duration;
    private double diff;
	private String inputFile;
	private String resultFile;
	private List<Long> insertRunTime;
	private List<Long> rangeRunTime;
	private List<Long> durationRunTime;
	private List<Long> candsRunTime;
	private List<Long> labelRunTime;
	private List<Long> rCheckRunTime;
	private List<Long> fstpmRunTime;
    private Trace logger;

    public static void main(String[] args) {
    	FSTPM controller = new FSTPM(args);
    	
		System.out.println("Reading input file ...");
		controller.processInput();
		System.out.println("Finished Processing file ...");
		
		controller.patternExtraction();

        controller.writeRuntimeToFile(controller.insertRunTime, "Insertion_runtime.txt");
        controller.writeRuntimeToFile(controller.rangeRunTime, "RangeSearch_runtime.txt");
        controller.writeRuntimeToFile(controller.durationRunTime, "DurationCheck_runtime.txt");
        controller.writeRuntimeToFile(controller.candsRunTime, "Candidates_runtime.txt");
        controller.writeRuntimeToFile(controller.candsRunTime, "rCheck_runtime.txt");
        controller.writeRuntimeToFile(controller.labelRunTime, "LabelPattern_runtime.txt");
        controller.writeRuntimeToFile(controller.fstpmRunTime, "FSTPM_runtime.txt");

		controller.printResults();
	}

	public FSTPM(String[] args) {
		if(args.length == 4){
			this.inputFile = args[0];
            this.dimension = Integer.parseInt(args[1]);
            this.range = Double.parseDouble(args[2]);
            this.duration = Integer.parseInt(args[3]);
            
            this.resultFile = this.getClass().getSimpleName() + "r" + this.range + "d" + this.duration + "_Results.txt";

		} else {
			this.printUsage();
			System.exit(1);
		}
		rbuilder = new RTreeBuilder();
		
		//tree = new RStarTree(dimension);
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
        double[] point;
        long start, end;
        int lineNum = 0;
        String label;
        int time;

        try {
            BufferedReader input =  new BufferedReader(new FileReader(this.inputFile));
            String line;
            String[] lineSplit;
        	String ini;
			
        	ini = input.readLine();
            
        	while ((line = input.readLine()) != null) {
				lineNum++;
                lineSplit = line.split(",");

                //insertion
				try {
                    oid = Integer.parseInt(lineSplit[0]);
                    point = extractPoint(lineSplit, 1);
                    label = lineSplit[3];
                    time = Integer.parseInt(lineSplit[4]);

                    start = System.currentTimeMillis();
                    rbuilder.insert(point[0], point[1], oid, label, time);
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
        	lineSplit = ini.split(",");
        	alg1.setting(Double.parseDouble(lineSplit[0]), Double.parseDouble(lineSplit[1]), Double.parseDouble(lineSplit[2]));
        	diff = Double.parseDouble(lineSplit[2]);
        	
			input.close();
            //tree.save();
		}
		catch (Exception e) {
			logger.traceError("Error while reading input file. Line " + lineNum + " Skipped\nError Details:");
		}
	}

    private double[] extractPoint(String[] points, int startPos) throws NumberFormatException
    {
    	double[] tmp = new double[this.dimension];
        for (int i = startPos, lineSplitLength = points.length;
             ((i < lineSplitLength) && (i < (startPos + this.dimension))); i++)
        {
            tmp[i-startPos] = Float.parseFloat(points[i]);
        }
        return tmp;
    }

	protected void printResults() {
		logger.trace("\nPerforming Run Time calculations..");

		List<Long> combined = new ArrayList<Long>();
		combined.addAll(insertRunTime);
		combined.addAll(rangeRunTime);
		combined.addAll(durationRunTime);
		combined.addAll(candsRunTime);
		combined.addAll(labelRunTime);
		combined.addAll(fstpmRunTime);

		String result = "\n"+this.getClass().getSimpleName()+" --RESULTS--";

		String temp = "\n\nInsertion operations:(in milliseconds) "+ generateRuntimeReport(insertRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nRange Search operations:(in milliseconds) "+ generateRuntimeReport(rangeRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nDuration Check operations: (in milliseconds) " + generateRuntimeReport(durationRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nCandidates Generation operations: (in milliseconds) " + generateRuntimeReport(candsRunTime);
        logger.trace(temp);
        result += temp;
        temp = "\n\nRange Check Generation operations: (in milliseconds) " + generateRuntimeReport(rCheckRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nCords to Label operations: (in milliseconds) " + generateRuntimeReport(labelRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nCombined operations:(in milliseconds) "+ generateRuntimeReport(combined);
        logger.trace( temp);
        result += temp;
        temp = "\n\nFSTPM operations:(in milliseconds) "+ generateRuntimeReport(fstpmRunTime);
        logger.trace( temp);
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
                " <path to input file> <dimension of points> <range> <duration>.\n");
	}
	
	//////////////// r-tree///////////////////
	
	
	//////////////// FSTPM ///////////////////
	protected void patternExtraction(){
		int oid;
        double[] point;
        long start, end;
        long startrc, startlb, endrc, endlb;
        int lineNum = 0;
        int time;
        int count = 0;
		HashMap<List<String>, Integer> pattern = new HashMap<List<String>, Integer>();
        
		try{
			BufferedReader input =  new BufferedReader(new FileReader(this.inputFile));
			String line;
			String[] lineSplit;
			
			line = input.readLine();
			// For all nodes
			while ((line = input.readLine()) != null) {
				lineNum++;
                lineSplit = line.split(",");
                count++;
                
                try{
                	// Pick one node to be pivot : center
                    oid = Integer.parseInt(lineSplit[0]);
                    
                    SpatialPoint sp = rbuilder.idMap.get(oid);
                    
                    System.out.println("\nprocessing node " + oid);

                    
                    
                    System.out.println("Range search begin...");
                    // Find all neighbors within 2R
                    //System.out.println(center.getCords()[0] + "   " + center.getCords()[1] + "  " + this.range*0.01*2*diff*100000);
                    start = System.currentTimeMillis();
                    double[] coord = sp.getCords();
                    
                    List<SpatialPoint> result = rbuilder.rangeQuery(coord[0],coord[1], this.range*1000, this.range*1000);
                    //List<SpatialPoint> result = tree.rangeSearch(center, this.range*0.01*2*diff/100000);    
                    end = System.currentTimeMillis();                    
                    
                    rangeRunTime.add(( end - start ));  
                    System.out.println("There are "+result.size()+" points found from "+rbuilder.getRtreeSize());
                                       
                    System.out.println("Duration check begin...");
                    // First filtering : remove nodes those time duration > T 
                    start = System.currentTimeMillis();
                    result = alg1.durationCheck(result, sp.getTime(), oid, duration);
                    end = System.currentTimeMillis();                    
                    
                    durationRunTime.add(( end - start ));  

                    
                    System.out.println("Cands generation begin...");
                    // Generate all possible pattern : candidates
                    if(result.size() > 1){
                        start = System.currentTimeMillis();
                    	List<List<SpatialPoint>> candidates = alg1.candExtraction(result);      
            		
                    	startrc = System.currentTimeMillis();
                    	// Second filtering : check whether all nodes in one candidate are located within range R
                    	List<List<SpatialPoint>> stPattern = new ArrayList<List<SpatialPoint>>();
                    	for(int i = 0; i < candidates.size(); i++){
                    		if(alg1.rangeCheck(candidates.get(i), sp, range) == true){
                    			stPattern.add(candidates.get(i));
                    		}
                    	}
                    	endrc = System.currentTimeMillis();  
                        rCheckRunTime.add(endrc - startrc);                  
                    	                    	
                        System.out.println("Cords to Label begin...");
                        startlb = System.currentTimeMillis();
                    	// change node pattern to label pattern and count frequency
                    	alg1.cordsToLabel(stPattern, pattern);
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
                if(count % 5000 == 0){
                	String fname = "output" + Integer.toString(count) + ".txt";
                	writeResult(fname, pattern);
                }
			}			
			
			writeResult("output.txt", pattern);
			
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



