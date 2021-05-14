package org.aksw.simba.lemming.metrics.single.edgemanipulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.aksw.simba.lemming.ColouredGraph;
import org.aksw.simba.lemming.metrics.single.MaxVertexDegreeMetric;
import org.aksw.simba.lemming.metrics.single.SingleValueMetric;
import org.aksw.simba.lemming.mimicgraph.constraints.TripleBaseSingleID;
import org.aksw.simba.lemming.tools.PrecomputingValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;
import com.github.andrewoma.dexx.collection.Map;

import grph.Grph.DIRECTION;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class EdgeModifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(EdgeModifier.class);
	
	private EdgeModification mEdgeModification;

	private List<SingleValueMetric> mLstMetrics;
	private ObjectDoubleOpenHashMap<String> mMapMetricValues;
	private ObjectDoubleOpenHashMap<String> mMapOrignalMetricValues;
	private List<TripleBaseSingleID> mLstRemovedEdges;
	private List<TripleBaseSingleID> mLstAddedEdges;
	private boolean isCoutingEdgeTriangles = false;
	private boolean isCountingNodeTriangles = false;
	private HashMap<String, IntSet> mMapCandidatesMaxDegMetric; //Map for storing candidate vertices for Max Degree metric computation
	private HashMap<String, Double> mMapCandidatesMaxDegreeMetricValues;
	
	public EdgeModifier(ColouredGraph clonedGraph, List<SingleValueMetric> lstMetrics){
		//list of metric
		mLstMetrics = lstMetrics;
		//initialize two list removed edges and added edges
		mLstRemovedEdges = new ArrayList<TripleBaseSingleID>();
		mLstAddedEdges = new ArrayList<TripleBaseSingleID>();
		//compute metric values
		computeMetricValues(clonedGraph, lstMetrics);
		//initialize EdgeModification
		mEdgeModification= new EdgeModification(clonedGraph,(int) mMapMetricValues.get("#nodetriangles"),(int) mMapMetricValues.get("#edgetriangles"));
		
		//Initialize Hash map for candidate vertices (MaxVertexDegreeMetric).
		mMapCandidatesMaxDegMetric = new HashMap<>();
		mMapCandidatesMaxDegMetric.put("RemoveAnEdgeIndegree", new IntOpenHashSet());
		mMapCandidatesMaxDegMetric.put("RemoveAnEdgeOutdegree", new IntOpenHashSet());
		mMapCandidatesMaxDegMetric.put("AddAnEdgeIndegree", new IntOpenHashSet());
		mMapCandidatesMaxDegMetric.put("AddAnEdgeOutdegree", new IntOpenHashSet());
		
		//Initialize Hash map for storing maximum vertex degrees for different cases.
		mMapCandidatesMaxDegreeMetricValues = new HashMap<>();
		mMapCandidatesMaxDegreeMetricValues.put("RemoveAnEdgeIndegree", 0.0);
		mMapCandidatesMaxDegreeMetricValues.put("RemoveAnEdgeOutdegree", 0.0);
		mMapCandidatesMaxDegreeMetricValues.put("AddAnEdgeIndegree", 0.0);
		mMapCandidatesMaxDegreeMetricValues.put("AddAnEdgeOutdegree", 0.0);
		
		
	}
	
	private void computeMetricValues(ColouredGraph clonedGraph, List<SingleValueMetric> lstMetrics){
		
		LOGGER.info("Compute "+lstMetrics.size()+ " metrics on the current mimic graph!");
		
		mMapMetricValues  = new ObjectDoubleOpenHashMap<String>();
		if(lstMetrics != null && lstMetrics.size()> 0 ){
			
			for(SingleValueMetric metric : lstMetrics){
				if(metric.getName().equalsIgnoreCase("#edgetriangles")){
					isCoutingEdgeTriangles = true;
				}else if(metric.getName().equalsIgnoreCase("#nodetriangles")){
					isCountingNodeTriangles = true;
				}
				
				double metVal = metric.apply(clonedGraph);
				String name = metric.getName();
				LOGGER.info("Value of "+ metric.getName() + " is " + metVal);
				//compute value for each of metrics
				mMapMetricValues.put(name, metVal);
			}
		}
		if(!isCountingNodeTriangles){
			mMapMetricValues.put("#nodetriangles", 0);
		}
		if(!isCoutingEdgeTriangles){
			mMapMetricValues.put("#edgetriangles", 0);
		}

		//create a backup map metric values
		mMapOrignalMetricValues = mMapMetricValues.clone();
	}
	
	public ColouredGraph getGraph(){
		return mEdgeModification.getGraph();
	}
	
	public ObjectDoubleOpenHashMap<String> tryToRemoveAnEdge(TripleBaseSingleID triple){
		if(triple != null && triple.edgeId != -1 &&
				triple.edgeColour != null &&
				triple.tailId != -1 && triple.headId !=-1){
			
			//add to list of removed edges
			mLstRemovedEdges.add(triple);
			
			ObjectDoubleOpenHashMap<String> mapChangedMetricValues = new ObjectDoubleOpenHashMap<String>();
			
			mEdgeModification.removeEdgeFromGraph(triple.edgeId);
			if(isCountingNodeTriangles){
				int newNodeTri = mEdgeModification.getNewNodeTriangles();
				mapChangedMetricValues.put("#nodetriangles", newNodeTri);	
			}
			
			if(isCoutingEdgeTriangles){
		        int newEdgeTri = mEdgeModification.getNewEdgeTriangles();
		        mapChangedMetricValues.put("#edgetriangles", newEdgeTri);				
			}

	        ColouredGraph graph = mEdgeModification.getGraph();
	        
	        for(SingleValueMetric metric: mLstMetrics){
	        	if(!metric.getName().equalsIgnoreCase("#edgetriangles") &&
	        			!metric.getName().equalsIgnoreCase("#nodetriangles")){
        			//double metVal = metric.apply(graph);
        			double metVal = tryToRemoveAnEdgeMetricComputation(triple, metric, graph); // Calling the Metric computation method
	        		mapChangedMetricValues.put(metric.getName(), metVal);   
	        	}
	        }
	        
	        //reverse the graph
	       // mEdgeModification.addEdgeToGraph(triple.tailId, triple.headId, triple.edgeColour);
	        mEdgeModification.addEdgeToGraph(triple.tailId, triple.headId, triple.edgeColour, 
	        		(int)mMapMetricValues.get("#nodetriangles"),(int) mMapMetricValues.get("#edgetriangles"));
	        
	        IntSet curEdges = mEdgeModification.getGraph().getEdges();
			if(!curEdges.contains(triple.edgeId)) {
				return null;
			}
	        
	        return mapChangedMetricValues;
		}else{
			LOGGER.warn("Invalid triple for removing an edge!");
			return null;
		}
	}
	
	public ObjectDoubleOpenHashMap<String> tryToAddAnEdge(TripleBaseSingleID triple){
		if(triple!= null && triple.edgeColour != null && triple.headId!= -1 && triple.tailId != -1){
			//add to list of added edges
			mLstAddedEdges.add(triple);
			
			ObjectDoubleOpenHashMap<String> mapMetricValues = new ObjectDoubleOpenHashMap<String>();
			triple.edgeId = mEdgeModification.addEdgeToGraph(triple.tailId,triple.headId, triple.edgeColour);
			
			if(isCountingNodeTriangles){
				int newNodeTri = mEdgeModification.getNewNodeTriangles();
				mapMetricValues.put("#nodetriangles", newNodeTri);
			}
			
			if(isCoutingEdgeTriangles){
				int newEdgeTri = mEdgeModification.getNewEdgeTriangles();
		        mapMetricValues.put("#edgetriangles", newEdgeTri);
			}
		    
		    ColouredGraph graph = mEdgeModification.getGraph();
		    for(SingleValueMetric metric: mLstMetrics){
	        	if(!metric.getName().equalsIgnoreCase("#edgetriangles") &&
	        			!metric.getName().equalsIgnoreCase("#nodetriangles")){
	        		
        			//double metVal = metric.apply(graph);
        			double metVal = tryToAddAnEdgeMetricComputation(triple, metric, graph);
	        		mapMetricValues.put(metric.getName(), metVal);   
	        	}
	        }
		    
		    //mEdgeModification.removeEdgeFromGraph(triple.edgeId);
		    mEdgeModification.removeEdgeFromGraph(triple.edgeId, (int)mMapMetricValues.get("#nodetriangles"), 
		    		(int)mMapMetricValues.get("#edgetriangles"));
			return mapMetricValues;
		}else{
			LOGGER.warn("Invalid triple for adding an edge!");
			return null;
		}
	}
	
	/**
	 * execute removing an edge
	 * 
	 * @param newMetricValues the already calculated metric from trial
	 */
	public void executeRemovingAnEdge(ObjectDoubleOpenHashMap<String> newMetricValues){
		if(mLstRemovedEdges.size() > 0 ){
			//store metric values got from trial
			updateMapMetricValues(newMetricValues);	
			//get the last removed edge
			TripleBaseSingleID lastTriple = mLstRemovedEdges.get(mLstRemovedEdges.size() -1);
			//remove the edge from graph again
			//mEdgeModification.removeEdgeFromGraph(lastTriple.edgeId);
			
			mEdgeModification.removeEdgeFromGraph(lastTriple.edgeId, (int) newMetricValues.get("#nodetriangles"),
					(int) newMetricValues.get("#edgetriangles"));
			
		}
	}
	
	/**
	 * execute adding an edge
	 * 
	 * @param newMetricValues the already calculated metric from trial
	 */
	public void executeAddingAnEdge(ObjectDoubleOpenHashMap<String> newMetricValues){
		if(mLstAddedEdges.size() > 0 ){
			//store metric values got from trial
			updateMapMetricValues(newMetricValues);
			//get the last added edge
			TripleBaseSingleID lastTriple = mLstAddedEdges.get(mLstAddedEdges.size() -1);
			//add the edge to graph again
			//mEdgeModification.addEdgeToGraph(lastTriple.tailId, lastTriple.headId, lastTriple.edgeColour);
			mEdgeModification.addEdgeToGraph(lastTriple.tailId, lastTriple.headId, 
									lastTriple.edgeColour, (int) newMetricValues.get("#nodetriangles"),
									(int) newMetricValues.get("#edgetriangles"));
		}
	}
	
	private void updateMapMetricValues(ObjectDoubleOpenHashMap<String> newMetricValues){
		mMapMetricValues = newMetricValues;
	}
	
	public ObjectDoubleOpenHashMap<String> getOriginalMetricValues(){
		return mMapOrignalMetricValues;
	}
	
	public ObjectDoubleOpenHashMap<String> getOptimizedMetricValues(){
		return mMapMetricValues;
	}
	
	private double tryToRemoveAnEdgeMetricComputation(TripleBaseSingleID triple, SingleValueMetric metric, ColouredGraph graph) {
		double metVal = 0;
		
		if(metric.getName().equals("maxInDegree")) { 
			
			metVal = metricComputationMaxDegree(metric, graph, "RemoveAnEdgeIndegree", DIRECTION.in, triple.headId);
			// Logic to check if maxDegreeMetric call is required for maxInDegree, defined in method : metricComputationMaxDegree
			
		}else if (metric.getName().equals("maxOutDegree")) {
			
			metVal = metricComputationMaxDegree(metric, graph, "RemoveAnEdgeOutdegree", DIRECTION.out, triple.tailId);
			
		}else { // If metric is other than maxInDegree and maxOutDegree then apply the metric
			metVal = metric.apply(graph);
		}
		
		return metVal;
	}
	
	private double tryToAddAnEdgeMetricComputation(TripleBaseSingleID triple, SingleValueMetric metric, ColouredGraph graph) {
		double metVal = 0;
		
		if(metric.getName().equals("maxInDegree")) { // Logic to check if maxDegreeMetric call is required for maxInDegree
			
			metVal = metricComputationMaxDegree(metric, graph, "AddAnEdgeIndegree", DIRECTION.in, triple.headId);
				
			
		}else if (metric.getName().equals("maxOutDegree")) {
			
			metVal = metricComputationMaxDegree(metric, graph, "AddAnEdgeOutdegree", DIRECTION.out, triple.tailId);
			
		}else { // If metric is other than maxInDegree and maxOutDegree then apply the metric
			metVal = metric.apply(graph);
		}
		
		return metVal;
	}
	
	private double metricComputationMaxDegree(SingleValueMetric metric, ColouredGraph graph, String metricName, DIRECTION direction, int tripleID) {
		double metVal;
		
		IntSet intSetTemp = mMapCandidatesMaxDegMetric.get(metricName);//Get the current candidate set
		
		if(intSetTemp.size() == 0) { //Initially the Candidate set will be empty, hence need to call the apply method and store the candidates
			
			metVal = metric.apply(graph); // apply the metric and get the value
			mMapCandidatesMaxDegreeMetricValues.replace(metricName, metVal); // Store the metric value for later use
			IntSet maxDegreeVertices = graph.getGraph().getVerticesOfDegree((int)metVal, direction); // Get the vertex with the metric value 
			intSetTemp.addAll(maxDegreeVertices); // store the vertex with metric value in candidate set
			
		}else {
			
			if(intSetTemp.contains(tripleID)) { // The Edge for vertex in candidate set is modified
				metVal = metric.apply(graph); // apply the metric and get the value
				mMapCandidatesMaxDegreeMetricValues.replace(metricName, metVal); // Store the metric value for later use
				IntSet maxDegreeVertices = graph.getGraph().getVerticesOfDegree((int)metVal, direction); // Get the vertex with the metric value 
				intSetTemp.addAll(maxDegreeVertices); // store the vertex with metric value in candidate set
			}
			else { // If Edge for vertex in candidate set is not modified then we can use the previously stored values
				metVal = mMapCandidatesMaxDegreeMetricValues.get(metricName); 
				int inVertexDegreeTemp = graph.getGraph().getInVertexDegree(tripleID); // Check the vertex degree for new edge
				if(inVertexDegreeTemp == metVal) { 
					intSetTemp.add(inVertexDegreeTemp); // If vertex has a degree similar to metric value previously stored, add the vertex in candidate set
				}
				
			}
		}
		
		return metVal;
	}
}
