import java.io.BufferedReader;
import java.io.FileReader;

import functions.operations;
import util.Trace;

public class FSTPM {
	private RTreeBuilder rbuilder;
    private double range;
    private int duration;
	private String inputFile;
    private Trace logger;
    private operations ops;
    private algorithm1 v1;
    private algorithm2 v2;
    

    public static void main(String[] args) {
    	FSTPM controller = new FSTPM(args);

		// construct r-tree
		System.out.println("Reading input file ...");
		controller.processInput();
		System.out.println("Finished Processing file ...");
		
		controller.v1.stfpm(controller.rbuilder);
		//controller.v2.stfpm(controller.rbuilder);
	
	}

	public FSTPM(String[] args) {
		if(args.length == 3){
			this.inputFile = args[0];
            this.range = Double.parseDouble(args[1]);
            this.duration = Integer.parseInt(args[2]);
            String[] fName = args[0].split("\\.");

    		rbuilder = new RTreeBuilder();
    		ops = new operations();
    		v1 = new algorithm1(range, duration, fName[0], this.inputFile);
    		v2 = new algorithm2(range, duration, fName[0], this.inputFile);
    		
            logger = Trace.getLogger(this.getClass().getSimpleName());
		} else {
			this.printUsage();
			System.exit(1);
		}
	}

	protected void processInput() {
        int oid;
        double longitude, latitude;
        double x, y;
        String label;
        int time;
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
                    latitude = Double.parseDouble(lineSplit[1]);
                    longitude = Double.parseDouble(lineSplit[2]);
                    label = lineSplit[3];
                    time = Integer.parseInt(lineSplit[4]);

                    x = ops.Distance(0.0, 0.0, latitude, 0.0);
                    y = ops.Distance(0.0, 0.0, 0.0, longitude);
                    
                    rbuilder.insert(x, y, oid, label, time);

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
	
	protected void printUsage() {
		System.err.println("Usage: "+ this.getClass().getSimpleName() +
                " <path to input file> <range(km)> <duration(mins)>.\n");
	}
}



