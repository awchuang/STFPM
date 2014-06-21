import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import arbor.mining.rtree.rtree.LeafEntry;
import arbor.mining.rtree.rtree.RTree;
import arbor.mining.rtree.rtree.SpatialPoint;
import arbor.mining.rtree.spatialindex.IData;
import arbor.mining.rtree.spatialindex.INode;
import arbor.mining.rtree.spatialindex.ISpatialIndex;
import arbor.mining.rtree.spatialindex.IVisitor;
import arbor.mining.rtree.spatialindex.Point;
import arbor.mining.rtree.spatialindex.Region;
import arbor.mining.rtree.storagemanager.DiskStorageManager;
import arbor.mining.rtree.storagemanager.IBuffer;
import arbor.mining.rtree.storagemanager.IStorageManager;
import arbor.mining.rtree.storagemanager.MemoryStorageManager;
import arbor.mining.rtree.storagemanager.PropertySet;
import arbor.mining.rtree.storagemanager.RandomEvictionsBuffer;


public class RTreeBuilder {
	public ISpatialIndex tree = null;
	public HashMap<Integer, SpatialPoint> idMap = new HashMap<Integer, SpatialPoint>();
	
    public RTreeBuilder() {   
			// Create a memory based storage manager.
			PropertySet ps = new PropertySet();
			Boolean b = new Boolean(true);
			ps.setProperty("Overwrite", b);
				//overwrite the file if it exists.

			Integer i = new Integer(4096);
			ps.setProperty("PageSize", i);
 		    // specify the page size. Since the index may also contain user defined data
			// there is no way to know how big a single node may become. The storage manager
			// will use multiple pages per node if needed. Off course this will slow down performance.

			IStorageManager memoryStorage = new MemoryStorageManager();

			IBuffer mfile = new RandomEvictionsBuffer(memoryStorage, 10, false);
			// applies a main memory random buffer on top of the persistent storage manager
			// (LRU buffer, etc can be created the same way).

			// Create a new, empty, RTree with dimensionality 2, minimum load 70%, using "file" as
			// the StorageManager and the RSTAR splitting policy.
			PropertySet ps2 = new PropertySet();

			Double f = new Double(0.7);
			ps2.setProperty("FillFactor", f);

			i = new Integer(32);
			ps2.setProperty("IndexCapacity", i);
			ps2.setProperty("LeafCapacity", i);
			// Index capacity and leaf capacity may be different.

			i = new Integer(2);
			ps2.setProperty("Dimension", i);

			tree = new RTree(ps2, mfile);
	}
    public void insert(double x, double y, int p_id, String label, int time) {
    	double[] f = new double[2];
    	f[0]=x;f[1]=y;
		Point p = new Point(f);
        SpatialPoint e = new SpatialPoint(f, p_id, label, time);
        idMap.put(p_id, e);
        //String data = (new Integer(p_id)).toString();
			// associate some data with this region. I will use a string that represents the
			// region itself, as an example.
			// NOTE: It is not necessary to associate any data here. A null pointer can be used. In that
			// case you should store the data externally. The index will provide the data IDs of
			// the answers to any query, which can be used to access the actual data from the external
			// storage (e.g. a hash table or a database table, etc.).
			// Storing the data in the index is convinient and in case a clustered storage manager is
			// provided (one that stores any node in consecutive pages) performance will improve substantially,
			// since disk accesses will be mostly sequential. On the other hand, the index will need to
			// manipulate the data, resulting in larger overhead. If you use a main memory storage manager,
			// storing the data externally is highly recommended (clustering has no effect).
			// A clustered storage manager is NOT provided yet.
			// Also you will have to take care of converting you data to and from binary format, since only
			// array of bytes can be inserted in the index (see RTree::Node::load and RTree::Node::store for
			// an example of how to do that).

		tree.insertData(null, p, p_id);
		//example of passing a null pointer as the associated data.
		//if you need to store data in tree, use tree.insertData(data.getBytes(), r, id);
    }
    public int getRtreeSize() {
    	return idMap.size();
    }
    public ArrayList<SpatialPoint> rangeQuery(double x, double y, double range_x, double range_y) {
    	double[] bbox_1 = new double[2];
    	double[] bbox_2 = new double[2];
    	bbox_1[0]=x-range_x;
    	bbox_1[1]=y-range_y;
    	
    	bbox_2[0]=x+range_x;
    	bbox_2[1]=y+range_y;
    	
    			
		MyVisitor vis = new MyVisitor();
		Region reg = new Region(bbox_1, bbox_2);
		tree.containmentQuery(reg, vis);
		
		ArrayList<SpatialPoint> ret = new ArrayList<SpatialPoint>();
		for (int i = 0;i<vis.list.size();i++) {
			ret.add(idMap.get(vis.list.get(i)));
		}
		return ret;
	}

	// example of a Visitor pattern.
	// see RTreeQuery for a more elaborate example.
	class MyVisitor implements IVisitor
	{
		ArrayList list = new ArrayList<Integer>();
		public void visitNode(final INode n) {}

		public void visitData(final IData d)
		{
			list.add(new Integer(d.getIdentifier()));
			//System.out.println(d.getIdentifier());
				// the ID of this data entry is an answer to the query. I will just print it to stdout.
		}
	}
}
