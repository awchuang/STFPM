package arbor.mining.rtree.rtree;

public class LeafEntry {
	public int oid;
	public String label;
	public int time;
	public double[] point = new double[2];
	public LeafEntry(int oid, double x, double y, String label, int time) {
		this.oid = oid;
		this.label = label;
		this.time = time;
		point[0] = x;
		point[1] = y;
	}
}
