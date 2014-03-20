package micropolisj.engine;

import java.math.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;

/**
 * Simulates traffic stuff
 */
public class TrafficSim {
	Micropolis engine;
	HashMap<Integer,SpecifiedTile> unready;
	HashMap<RoadSpecifiedTile,SpecifiedTile> ready;
	HashMap<RoadSpecifiedTile,Integer> mapBack;
	HashSet<CityLocation> goal;
	HashSet<CityLocation> found;
	int currentRoadType;
	
	
	
	
	public TrafficSim(){
		this(new Micropolis());
	}
	
	public TrafficSim(Micropolis city){
		engine = city;
		ready = new HashMap<RoadSpecifiedTile,SpecifiedTile>();
		unready = new HashMap<Integer,SpecifiedTile>();
		goal = new HashSet<CityLocation>();
		found = new HashSet<CityLocation>();
		mapBack = new HashMap<RoadSpecifiedTile,Integer>();
	}
	/**
	 * The function is called to generate traffic from the starting position
	 * uses A*-Algorithm to find ways
	 * @param startP starting position
	 * @return length of the way (-1 for no way)
	 */
	public int genTraffic(CityLocation startP) {
		CityLocation end=findEnd(startP);
		int way=findWay(startP,end);
		if (way!=-1) {
			engine.putVisits(startP);
			engine.putVisits(end);
		} else {
			engine.noWay();
		}
		return way;
	}
	
	/**
	 * determinates the end of a way starting at a given field
	 * @param startpos
	 * @return the endpos
	 */
	
	private CityLocation findEnd(CityLocation startpos){
		/* iterates through engine.visits and puts them (together with a specifically calculated weight)
		 * into a new HashMap. From there we will randomly create the "end" of the route.
		 */
		HashMap<CityLocation,Integer> help = new HashMap<CityLocation,Integer>();
		Iterator<CityLocation> it = engine.visits.keySet().iterator();
		while(it.hasNext()){
			CityLocation temp = it.next();		
			help.put(temp,getValue(startpos,temp));
		}
		it.remove();
		
		int sum=0;
		int t;
		Iterator<Integer> it2 = help.values().iterator();
		while(it.hasNext()){
			t=(int)it2.next();
			sum+=t;
		}		
		int i = engine.PRNG.nextInt(sum)+1;
		for(CityLocation b : help.keySet()){
			i-=(int)help.get(b);
			if(i<=0){
				return b;
			}
		}
		return startpos;
	}
	
	
	/**
	 * This function returns the weight for later more or less randomly determine the end 
	 * of the route you want to travel.
	 * @param start
	 * @param end
	 * @return
	 */
	private int getValue(CityLocation start, CityLocation end){
		int factor;
		factor = getFactor(engine.getTile(start.x, start.y), engine.getTile(end.x,end.y));
		if (evalfunc(start,(HashSet<CityLocation>) findPeriphereRoad(end).keySet())>=150) {
			return 0;
		}
		return (200000/(evalfunc(start,(HashSet<CityLocation>) findPeriphereRoad(end).keySet()))+20)*factor; 
		//factor 200 000 for making randomization easyer later on.
	}
	
	/**
	 * Just calculating the factor for later calculating the weight in "getValue"
	 * @param start
	 * @param end
	 * @return
	 */
	private static int getFactor(char start, char end){
		if(TileConstants.isResidentialZone((int)start)){
			if(TileConstants.isResidentialZone((int)end)){
				return 2;
			}
			if(TileConstants.isCommercialZone((int)end)){
				return 20;
			}
			if(TileConstants.isIndustrialZone((int)end)){
				return 12;
			}
			if((int)end==964){
				return 8;	//School
			}
			if((int)end==982 || (int)end==991){
				return 9;	//UniversityA or UniversityB
			}
			if((int)end==973){
				return 7;	//Museum
			}
			if((int)end==1012){
				return 5;	//OpenAir
			}
			if((int)end==750 || (int)end==816){
				return 5;	//PowerPlant or Nuclear	
			}
			if((int)end==716){
				return 6;	//Airport
			}
			if((int)end==784 || (int)end==800){
				return 7;	//Stadium or FullStadium
			}			
		}
		if(TileConstants.isCommercialZone((int)start)){
			if((int)end==716){
				return 2;	//Airport
			}
			if((int)end==698){
				return 1;	//Port
			}
		}
		if(TileConstants.isIndustrialZone((int)start)){
			if((int)end==698){ //Port
				return 1;
			}
		}
		if((int)start==964){	//School
			if((int)end==973){
				return 5;	//Museum
			}
			if((int)end==982 || (int)end==991){
				return 1;	//UniversityA or UniversityB
			}
		}
		if((int)start==716){	//Airport
			if((int)end==784 || (int)end==800){
				return 2;	//Stadium or FullStadium
			}
			if((int)end==1012){
				return 1;	//OpenAir
			}
		}		
		return 0;
	}
	
	
	/**
	 * finds way from A to B, if exists
	 * in general, does A*-algorithm
	 * and increase traffic along the way
	 * @param startpos
	 * @param endpos
	 * @return length of the way
	 */
	
	protected int findWay(CityLocation startpos, CityLocation endpos){
		int currentCost=0;
		CityLocation currentLocation=new CityLocation(-1,-1);
		ready = new HashMap<RoadSpecifiedTile,SpecifiedTile>();
		HashMap<CityLocation,SpecifiedTile> temp=findPeriphereRoad(startpos);
		for (CityLocation f : temp.keySet()) {
			for (Integer tm : calcRoadType(f,1)) {
				ready.put(new RoadSpecifiedTile(f,tm), temp.get(f)); //generate starts
			}
			found.add(f);
		}
		goal=(HashSet<CityLocation>) findPeriphereRoad(endpos).keySet(); //generate ends
		int best=3000;
		RoadSpecifiedTile fastGoal=new RoadSpecifiedTile(new CityLocation(-1,-1),1);
		if (ready.isEmpty()) {
			return -1;
		}
		for (RoadSpecifiedTile f : ready.keySet()) { //take roads adj to starts
			for (RoadSpecifiedTile g : findAdjRoads(f.getLocation(),f.getRoadType())) {
				for (int roadType : calcRoadType(engine.getTile(g.getLocation()),ready.get(f).getRoadType())) {
					int keyi=16384*10*evalfunc(f.getLocation(),goal)+g.getLocation().y*5+roadType;
					unready.put(keyi,new SpecifiedTile(g.getLocation(),f,false,roadType));
					mapBack.put(g,keyi);
				}
				found.add(g.getLocation());
			}
		}
		while (!unready.isEmpty() && best>(Collections.min(unready.keySet()))) { //main algorithm A*
			int current=Collections.min(unready.keySet()); //add new field to ready
			currentLocation=unready.get(current).getLoc();
			currentRoadType=unready.get(current).getRoadType();
			currentCost=engine.getTrafficCost(currentLocation,currentRoadType);
			
			ready.put(new RoadSpecifiedTile(currentLocation,currentRoadType), new SpecifiedTile(currentCost,unready.get(current).getPred(),true,currentRoadType));
			unready.remove(current);
			for (RoadSpecifiedTile g : findAdjRoads(currentLocation, currentRoadType)) { //go through adj roads
				if (!found.contains(g)) { //new road part found
					this.found.add(g.getLocation());
					int keyi=16384*(10*evalfunc(currentLocation,goal)+ready.get(new RoadSpecifiedTile(currentLocation,currentRoadType)).getCosts())+g.getLocation().y*5;
					for (int roadType : calcRoadType(TileConstants.roadType(engine.getTile(g.getLocation())),currentRoadType)) {
						unready.put(keyi+roadType,new SpecifiedTile(g.getLocation(),new RoadSpecifiedTile(currentLocation,currentRoadType),false,roadType));
						mapBack.put(new RoadSpecifiedTile(g.getLocation(),roadType), keyi+roadType);
					}
				} else { //was already found before
					if (!RoadSpecifiedTile.equals(new RoadSpecifiedTile(g.getLocation(),g.getRoadType()),ready.get(current).getPred())) {//if not pred
						if (ready.containsKey(g)) { //if it is already ready update ready
							int c=evalfunc(g.getLocation(), goal)+currentCost+ready.get(ready.get(g).getPred()).getCosts();
							if (ready.get(g).getCosts()<=c) {
								ready.put(g, new SpecifiedTile(c,new RoadSpecifiedTile(currentLocation,currentRoadType),true,ready.get(g).getRoadType()));
							}
						} else { //if not, update it unready
							int keyi=16384*(10*evalfunc(currentLocation,goal)+ready.get(new RoadSpecifiedTile(currentLocation,currentRoadType)).getCosts())+g.getLocation().y*5;
							if (keyi<= mapBack.get(g)) {
								for (int tm : calcRoadType(g.getLocation(),currentRoadType)) {
									unready.put(keyi+tm,new SpecifiedTile(g.getLocation(),new RoadSpecifiedTile(currentLocation,currentRoadType),false,tm));
									mapBack.put(new RoadSpecifiedTile(g.getLocation(),tm), keyi+tm);
								}
							}
						}
					}
				}
			}
			if (goal.contains(currentLocation)) { //if it is a goal update goal
				best=Math.min(best, ready.get(new RoadSpecifiedTile(currentLocation,currentRoadType)).getCosts());
				fastGoal=new RoadSpecifiedTile(new CityLocation(currentLocation.x,currentLocation.y),currentRoadType);
			}
		}
		if (best==3000) {
			return -1;
		}
		Vector<CityLocation> way=new Vector<CityLocation>();
		while (!RoadSpecifiedTile.equals(ready.get(fastGoal).getPred(),new RoadSpecifiedTile(new CityLocation(-1,-1),0))) { //add traffic to way 
			engine.addTraffic(fastGoal.getLocation().x, fastGoal.getLocation().y, engine.getTrafficCost(fastGoal.getLocation(),fastGoal.getRoadType()));
			way.add(fastGoal.getLocation());
			fastGoal=ready.get(fastGoal).getPred();
		}
		engine.paths(way);
		return best;
	}
	
	private boolean sameRoadType(int roadType1, int roadType2) {
		return roadType1==4 || roadType2==4 || (RoadSpecifiedTile.isRoad(roadType1)==RoadSpecifiedTile.isRoad(roadType2) && RoadSpecifiedTile.isRail(roadType1)==RoadSpecifiedTile.isRail(roadType2));
	}
	
	/**
	 *  caluclate possible roadTypes
	 * @param me int between 1 and 6
	 * @param prevType int between 1 and 4
	 * @return int between 1 and 4
	 */
	
	private static Vector<Integer> calcRoadType(int me,int prevType) {
		Vector<Integer> ret =new Vector<Integer>();
		int myType =TileConstants.roadType(me);
		if (myType==4) {
			ret.add(4);
			return ret;
		}
		if (myType!=5 && myType!=6) {
			ret.add(myType);
			return ret;
		}
		if (prevType==4) {
			ret.add(myType-4);
			ret.add(3);
			return ret;
		} else {
			ret.add(prevType);
			return ret;
		}
	}
	
	private Vector<Integer> calcRoadType(CityLocation me,int prevType) {
		return calcRoadType(engine.getTile(me), prevType);
	}
	
	/*private HashSet<RoadSpecifiedTile> findAdjRoads(CityLocation loc) {
		HashSet<RoadSpecifiedTile> ret=new HashSet<RoadSpecifiedTile>();
		
		for (int dir=0;dir<4;dir++) {
			if (engine.onMap(loc,dir) && TileConstants.isRoadAny(engine.getTile(Micropolis.goToAdj(loc,dir).x, Micropolis.goToAdj(loc,dir).y))) {
				CityLocation destination=Micropolis.goToAdj(loc,dir);
				for (int u : calcRoadType(destination,engine.getTile(loc))) {
					ret.add(new RoadSpecifiedTile(destination,u));
				}
			}
		}
		return ret;
	}*/
	
	private HashSet<RoadSpecifiedTile> findAdjRoads(CityLocation loc, int roadTyp) {
		HashSet<RoadSpecifiedTile> ret=new HashSet<RoadSpecifiedTile>();
		
		for (int dir=0;dir<4;dir++) {
			if (engine.onMap(loc,dir) && TileConstants.isRoadAny(engine.getTile(Micropolis.goToAdj(loc,dir).x, Micropolis.goToAdj(loc,dir).y))) {
				CityLocation destination=Micropolis.goToAdj(loc,dir);
				for (int u : calcRoadType(destination,roadTyp)) {
					ret.add(new RoadSpecifiedTile(destination,u));
				}
			}
		}
		return ret;
	}
	
	/**
	 * finds out if there are streets next to our zone and return a HashMap with default values
	 * @param pos zone center
	 * @return keys are the streets next to the zone, values are default
	 */
	
	public HashMap<CityLocation,SpecifiedTile> findPeriphereRoad(CityLocation pos){
		char tiletype;
		HashMap<CityLocation,SpecifiedTile> ret=new HashMap<CityLocation,SpecifiedTile>();
		tiletype=engine.getTile(pos.x, pos.y);
		int dimension; //height (and so width) of the tile
		if(tiletype==716){       //if tiletype==AIRPORT -> see TileConstants  needs to be changed for some new buildings potentially!!!
			dimension=4;
		}else{
			dimension=3;
		}
		for(int i=-1; i<dimension-1;i++){
			for (int roadType : calcRoadType(new CityLocation(pos.x-2,pos.y+i),1)) {
				if (engine.onMap(new CityLocation(pos.x-2,pos.y+i))&&TileConstants.isRoadAny(engine.getTile(pos.x-2, pos.y+i))){  
					ret.put(new CityLocation(pos.x-2,pos.y+i),new SpecifiedTile(roadType));
				}
			}
			for (int roadType : calcRoadType(new CityLocation(pos.x+dimension-1,pos.y+i),1)) {
				if (engine.onMap(new CityLocation(pos.x+dimension-1,pos.y+i))&&TileConstants.isRoadAny(engine.getTile(pos.x+2, pos.y+i))){ 
					ret.put(new CityLocation(pos.x+dimension-1,pos.y+i),new SpecifiedTile(roadType));
				}
			}
			for (int roadType : calcRoadType(new CityLocation(pos.x+i,pos.y-2),1)) {
				if (engine.onMap(new CityLocation(pos.x+i,pos.y-2))&&TileConstants.isRoadAny(engine.getTile(pos.x+i, pos.y-2))){  
					ret.put(new CityLocation(pos.x+i,pos.y-2),new SpecifiedTile(roadType));
				}
			}
			for (int roadType : calcRoadType(new CityLocation(pos.x+i,pos.y+dimension-1),1)) {
				if (engine.onMap(new CityLocation(pos.x+i,pos.y+dimension-1))&&TileConstants.isRoadAny(engine.getTile(pos.x+i, pos.y+2))){  
					ret.put(new CityLocation(pos.x+i,pos.y+dimension-1),new SpecifiedTile(roadType));
				}
			}
		}
		
		return ret;
	}
	/**
	 * capped at 32 767
	 * @param start
	 * @param finish
	 * @return
	 */
	
	private static int evalfunc(CityLocation start, HashSet<CityLocation> finish){
		int ret=32767;
		for (CityLocation g : finish) {
			ret=Math.min(ret,Math.abs(start.x-g.x)+Math.abs(start.y-g.y)+1);
		}
		return ret;
	}
	
	
	
	
	
}