package functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import arbor.mining.rtree.rtree.SpatialPoint;

public class Version1 {
	
	// generate all combination size = 2~5
	public List<List<SpatialPoint>> patternExtraction(List<SpatialPoint> src){
		List<List<SpatialPoint>> result = new ArrayList<List<SpatialPoint>>();
    	List<SpatialPoint> to = new ArrayList<SpatialPoint>();
		SpatialPoint Ep = new SpatialPoint();
		SpatialPoint head = new SpatialPoint();
		head = src.remove(0);
		
		System.out.println(src.size());
		

		for (int i = 0; i < src.size(); i++){
			List<SpatialPoint> one = new ArrayList<SpatialPoint>();
			one.add(head);
			one.add(src.get(i));
			result.add(one);
		}
		for (int i = 2; i <= 9; i++) {
			for (int k = 0; k < i; k++) {
				to.add(Ep);
			}
			comb(result, src, to, i, src.size(), i);
			to.clear();
	    }
		for(int i = src.size(); i < result.size(); i++){
			result.get(i).add(0, head);
		}
		Collections.reverse(result);
	    return result;
	}
	
	//C(m, n) = C(m-1, n-1) + C(m-1, n)
	private void comb(List<List<SpatialPoint>> result, List<SpatialPoint> from, List<SpatialPoint> to, int len, int m, int n) {
		if (n == 0) {
			List<SpatialPoint> tmp = new ArrayList<SpatialPoint>(to);
			result.add(tmp);
		} else {
			to.set(n-1, from.get(m - 1));    	
			if (m > n - 1) {
				comb(result, from, to, len, m - 1, n - 1);
			}
			if (m > n) {
				comb(result, from, to, len, m - 1, n);
			}
		}
	}
	


	public Boolean rangeCheck(List<SpatialPoint> stPatterns, double range){
		double dis = smallestenclosingcircle.makeCircle(stPatterns);

		if(dis < range)
			return true;
		else
			return false;
	}
	
	public double Distance(double latitude1, double longitude1, double latitude2, double longitude2)
	{
	   double radLatitude1 = latitude1 * Math.PI / 180;
	   double radLatitude2 = latitude2 * Math.PI / 180;
	   double l = radLatitude1 - radLatitude2;
	   double p = longitude1 * Math.PI / 180 - longitude2 * Math.PI / 180;
	   double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(l / 2), 2)
	                    + Math.cos(radLatitude1) * Math.cos(radLatitude2)
	                    * Math.pow(Math.sin(p / 2), 2)));
	   distance = distance * 6378137.0;
	   distance = Math.round(distance * 10000) / 10000;

	   return distance/1000;  // km
	}
	
	public List<SpatialPoint> durationCheck(List<SpatialPoint> result, int time, float oid, int duration){
		List<SpatialPoint> re = new ArrayList<SpatialPoint>(1);
		int size = result.size();
		
		for(int i = 0; i < size ; i++){
			if(result.get(i).getOid() == oid)
				re.add(0, result.get(i));
			else if(result.get(i).getTime() - time < duration && result.get(i).getTime() - time >= 0)
				re.add(result.get(i));
		}		
		return re;
	}
	
	public void cordsToLabel(List<List<SpatialPoint>> src, HashMap<List<String>, Integer> dst){
		for(int i = 0; i < src.size(); i++){
			List<String> temp = new ArrayList<String>();
			for(int k = 0; k < src.get(i).size(); k++){
				temp.add(src.get(i).get(k).getLabel());
			}
			if(dst.containsKey(temp))
				dst.put(temp, dst.get(temp)+1);
			else
				dst.put(temp, 1);
		}
	}
}
