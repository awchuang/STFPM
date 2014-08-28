import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import util.Trace;
import functions.operations;
import functions.writeResult;
import arbor.mining.rtree.rtree.SpatialPoint;


public class algorithm1 {
	private double range;
	private int duration;
	private String outputFile;
	private static operations ops;
	private static writeResult output;
    private static Trace logger;
	
	public algorithm1(double range, int duration, String fname){
		logger = Trace.getLogger(this.getClass().getSimpleName());
		ops = new operations();
		output = new writeResult();
		this.range = range;
		this.duration = duration;

		output.resultfile = this.getClass().getSimpleName() + "_" + fname + "r" + range + "d" + duration + "_Results.txt";
        outputFile = this.getClass().getSimpleName() + "_" + fname + "_r" + range + "t" + duration + "_output.txt";
	}
    
	public void stfpm(RTreeBuilder rbuilder){
		int oid;
        long start, end;
        long startrc, startlb, endrc, endlb;
        int lineNum = 0;
		HashMap<List<String>, Integer> result = new HashMap<List<String>, Integer>();
		
		try{
			List<SpatialPoint> EpNeighbor = new ArrayList<SpatialPoint>();
			List<List<SpatialPoint>> stPatterns = new ArrayList<List<SpatialPoint>>();
			List<List<SpatialPoint>> FstPatterns = new ArrayList<List<SpatialPoint>>();
			for(oid = 0; oid < rbuilder.getRtreeSize(); oid++){
                try{
                	// Pick one event to be pivot : Ep
                    SpatialPoint Ep = rbuilder.idMap.get(oid);
                    
                    System.out.println("\nprocessing node " + oid);

                    // Find all neighbors within 2R : EpNeighbor
                    System.out.println("Range search begin...");
                    start = System.currentTimeMillis();
                    double[] coord = Ep.getCords();
                    EpNeighbor = rbuilder.rangeQuery(coord[0],coord[1], range, range);
                    end = System.currentTimeMillis();  
                    output.rangeRunTime.add(( end - start ));
                    
                    // First filtering : remove nodes those time duration > T 
                    System.out.println("Duration check begin...");
                    start = System.currentTimeMillis();
                    EpNeighbor = ops.durationCheck(EpNeighbor, Ep.getTime(), oid, duration);
                    end = System.currentTimeMillis();                    
                    System.out.println("There are "+EpNeighbor.size()+" points found from "+rbuilder.getRtreeSize());

                    output.durationRunTime.add(( end - start ));  


                    // Generate all possible pattern : patterns
                    System.out.println("Cands generation begin...");
                    if(EpNeighbor.size() > 1){
                        start = System.currentTimeMillis();
                    	stPatterns = ops.patternExtraction(EpNeighbor); 
                    	for(int i = 0; i < stPatterns.size(); i++){
                    		//System.out.print(stPatterns.get(i).size() + ",");
                    	}
            		
                    	startrc = System.currentTimeMillis();
                    	// Second filtering : check whether all nodes in one candidate are located within range R
                    	while(stPatterns.isEmpty() == false){
                    		if(ops.rangeCheck(stPatterns.get(0), range) == true){
                    			FstPatterns.add(stPatterns.get(0));
                    			
                    			for(int k = 0; k < stPatterns.size(); k++){
                    				ArrayList<SpatialPoint> tmp = new ArrayList<SpatialPoint>(stPatterns.get(0));
                    				//System.out.println(tmp.retainAll(stPatterns.get(k)));
                    				if(tmp.equals(stPatterns.get(k))){
                    						//System.out.println(tmp);
                    						stPatterns.remove(k);
                    						k--;
                    						
                    				}
                    			}
                    		}
                    		stPatterns.remove(0);
                    	}
                    	for(int i = 0; i < stPatterns.size(); i++){
                    		if(ops.rangeCheck(stPatterns.get(i), range) == true){
                    			FstPatterns.add(stPatterns.get(i));
                    		}
                    	}
                    	endrc = System.currentTimeMillis();  
                    	output.rCheckRunTime.add(endrc - startrc);                  
                    	                    	
                        System.out.println("Cords to Label begin...");
                        startlb = System.currentTimeMillis();
                    	// change node pattern to label pattern and count frequency
                    	ops.cordsToLabel(FstPatterns, result);
                    	endlb = System.currentTimeMillis();      
                    	output.labelRunTime.add(endlb - startlb);              
                        
                        
                        end = System.currentTimeMillis();     
                        output.candsRunTime.add(( (end - start) -(endrc - startrc) -(endlb - startlb) ));
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
			
			output.writeOutput(outputFile, result);
			
		}
		catch (Exception e) {
			logger.traceError("Error while reading input file. Line " + lineNum + " Skipped\nError Details:");
		}
	}

}
