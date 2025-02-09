package org.aksw.simba.lemming.simplexes.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aksw.simba.lemming.ColouredGraph;
import org.aksw.simba.lemming.mimicgraph.colourmetrics.utils.OfferedItemByRandomProb;
import org.aksw.simba.lemming.mimicgraph.constraints.ColourMappingRules;
import org.aksw.simba.lemming.mimicgraph.constraints.IColourMappingRules;
import org.aksw.simba.lemming.mimicgraph.generator.AbstractGraphGeneration;
import org.aksw.simba.lemming.mimicgraph.generator.IGraphGeneration;
import org.aksw.simba.lemming.simplexes.TriColos;
import org.aksw.simba.lemming.simplexes.analysis.Connected1Simplexes;
import org.aksw.simba.lemming.simplexes.analysis.ConnS2;
import org.aksw.simba.lemming.simplexes.analysis.FindSelfLoops;
import org.aksw.simba.lemming.simplexes.analysis.FindTri;
import org.aksw.simba.lemming.simplexes.analysis.Isolated1Simplexes;
import org.aksw.simba.lemming.simplexes.analysis.IsoS2;
import org.aksw.simba.lemming.simplexes.analysis.IsolatedSelfLoops;
import org.aksw.simba.lemming.simplexes.analysis.S0C;
import org.aksw.simba.lemming.simplexes.analysis.S1ConnToS2;
import org.aksw.simba.lemming.simplexes.analysis.S1ConnectingS2;
import org.aksw.simba.lemming.simplexes.distribution.EdgeDistIS;
import org.aksw.simba.lemming.simplexes.distribution.TriDistI;
import org.aksw.simba.lemming.simplexes.distribution.VertDistI;
import org.aksw.simba.lemming.util.Constants;
import org.aksw.simba.lemming.util.IntSetUtil;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.ext.com.google.common.collect.Sets.SetView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.ObjectObjectOpenHashMap;

import grph.DefaultIntSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Generator 1 based on Sampling approach for selecting 1-simplexes.
 */
public class GraphGenerationSimplexApproachS extends AbstractGraphGeneration implements IGraphGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphGenerationSimplexApproachS.class);
	private static int maximumIteration;
	
	//******************** Variables for storing estimation of edges and nodes for different type of simplexes**************************//
	private int estimatedEdgesTriangle;//Estimated number of edges for connected 2-simplexes.
	private int estimatedVerticesTriangle; // Estimated number of Vertices for connected 2-simplexes
	private int estEdgesSelfLoopConnTri;
	private int estimatedEdgesCommon; // Estimated number of edges linked to connected 2-simplexes
	private int estVerts1SimplexesConntoTri; // Estimated number of Vertices forming 1-simplexes that are connected to triangles.
	private int estEdgesSelfLoop1SimplexConnToTri;
	private int estimatedEdges1SimplexesConnect2Simplexes; // Estimated number of 1-simplexes connecting 2-simplexes
	private int estimatedEdgesIsolatedTriangle; // Estimated number of edges for isolated triangles.
	private int estimatedVerticesIsolatedTriangle; // Estimated number of Vertices forming triangles
	private int estEdgesSelfLoopIsoTri;
	private int estimatedEdges1Simplexes; // Estimated number of edges forming 1-simplexes
	private int estimatedVertices1Simplexes; // Estimated number of Vertices forming 1-simplexes
	private int estimatedEdgesIsoSelfLoop; // Estimated number of edges forming isolated self loop (1-simplexes with same head and tail).
	private int estimatedVerticesIsoSelfLoop; // Estimated number of Vertices forming 1-simplexes
	private int estimatedEdgesSelfLoopIn1Simplex; // Estimated number of edges forming self loop in isolated 1-simplexes
	private int estEdgesSelfLoopConn1Simplexes; // self loops in connected 1-simplexes
	private int estimatedVertices0Simplexes; // Estimated number of Vertices forming 0-simplexes
	private int estimatedEdgesConnS1; //Estimated number of edges for connected 1-simplexes
	private int estimatedVertsConnS1; //Estimated number of vertices for connected 1-simplexes
	
	//********************** Color mappers for different simplexes **********************************************//
	private IColourMappingRules mColourMapperTriangles; // ColourMappingRules define the possible combination of colours between vertices and edges. This variable is specifically for Triangles
	private IColourMappingRules mColourMapperCommonEdges; // This Colour Mapper variable is specifically for common edges connecting triangles in input graphs with vertices that are not part of triangles.
	private IColourMappingRules mColourMapperIsolatedTriangles; // ColourMappingRules define the possible combination of colours between vertices and edges. This variable is specifically for Triangles
	private IColourMappingRules mColourMapperConnected1Simplexes; // This Colour Mapper variable is specifically for connected 1-simplexes
	private IColourMappingRules mColourMapper1SimplexesConnTri; // This Colour Mapper variable is specifically for 1-simplexes that link triangles.
	private IColourMappingRules mColourMapper1Simplexes; // This Colour Mapper variable is specifically for isolated 1-simplexes.
	private IColourMappingRules mColourMapperIsoSelfLoop; // This Colour Mapper variable is specifically for isolated self loop (1-simplexes with same head and tail).
	private IColourMappingRules mColourMapperSelfLoopIn1Simplex; // This Colour Mapper variable is specifically for self loop in isolated 1-simplexes
	private IColourMappingRules mColourMapperSelfLoopIsoTri; // This Colour Mapper variable is specifically for self loops for isolated triangles.
	private IColourMappingRules mColourMapperSelfLoopConnTri; // This Colour Mapper variable is specifically for self loops for connected triangles.
	private IColourMappingRules mColourMapperSelfLoop1SimplexConnToTri; // This Colour Mapper variable is specifically for self loops for 1-simplexes linked to connected triangles.
	private IColourMappingRules mColourMapperSelfLoopConn1Simplexes; // This Colour Mapper variable is specifically for self loops for connected 1-simplexes.
	
	// Map for tracking vertices denoting classes
	private Map<BitSet, Integer> mMapClassColourToVertexIDSimplexes = new ConcurrentHashMap<BitSet, Integer>();
	
	//******************** Additional Simplex Analysis variables for connected 2-simplexes **********************************************//
	private ObjectObjectOpenHashMap<TriColos, double[]> mTriangleColoursTriangleEdgeCounts; // Map for storing vertex colors of triangles as keys and array of triangle and edge counts as value.
	private Set<TriColos> setAllTriangleColours; // Set for storing all triangles
	private ObjectObjectOpenHashMap<TriColos, List<IntSet>> mTriangleColorsVertexIds; // Map for tracking triangle colors and vertex IDs forming triangles using those colors 
	private Map<BitSet, IntSet> mMapColourToVertexIDs2Simplex = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for 2-simplexes
	private Map<BitSet, IntSet> mMapColourToEdgeIDs2Simplex = new ConcurrentHashMap<BitSet, IntSet>(); //Map for storing vertex colours as Keys and Vertex IDs as Value for 2-simplexes 
	//TODO: Remove Maps for Edge Ids, since they are not used. Could probably be used when working with probabilities for properties?
	private Map<BitSet, IntSet> mMapColourToVertexIDsConnectedTo2Simplex = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for 2-simplexes
	private ObjectObjectOpenHashMap<BitSet, ObjectObjectOpenHashMap<BitSet, ObjectObjectOpenHashMap<BitSet, double[]>>> mTriColosCountsAvgProb; // Map for storing triangles along with their statistics
	
	
	//**************** Additional Simplex Analysis variables for isolated 2-simplexes ************************************//
	private Map<BitSet, IntSet> mMapColourToVertexIDs2SimplexIsolated = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for isolated 2-simplexes
	private Map<BitSet, IntSet> mMapColourToEdgeIDs2SimplexIsolated = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for isolated 2-simplexes
	private ObjectObjectOpenHashMap<TriColos, List<IntSet>> mIsolatedTriangleColorsVertexIds = new ObjectObjectOpenHashMap<TriColos, List<IntSet>>(); // Map for tracking triangle colors and vertex IDs forming isolated triangles using those colors
	
	//*************** Distribution objects ***************************************//
	private TriDistI triangleDistribution; // store distributions of isolated and connected 2-simplexes
	private EdgeDistIS s1connToTriDist; // 1-simplexes connected to triangles
	private VertDistI s1connToTriVertDist; // Distribution of vertices connected to triangles
	private EdgeDistIS s1connTriDist; // 1-simplexes connecting triangles
	private EdgeDistIS s1IsoDist; // isolated 1-simplexes
	private EdgeDistIS s1ConnDist; // connected 1-simplexes
	private VertDistI s0Dist; // 0-simplexes
	
	//************* Distribution objects self loop *******************************//
	private VertDistI selfLoopIsoTritDist; // Distribution of self loops for isolated triangles
	private VertDistI selfLoopConnTriDist; // Distribution of self loops for connected triangles
	private VertDistI selfLoops1ConnToTriDist; // Distribution of self loops for vertices connected to triangles
	private VertDistI selfLoops1IsoS1; // Distribution of isolated self loops
	private VertDistI selfLoopsInS1Dist; // Distribution of self loops for vertices in isolated 1-simplexes
	private VertDistI selfLoopsInConnS1Dist; // Distribution of self loops for vertices in connected 1-simplexes
	
	//**************** Additional Simplex Analysis variables for isolated 1-simplexes ******************************//
	private Map<BitSet, IntSet> mMapColourToVertexIDs1Simplex = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for 1-simplexes
	private Map<BitSet, IntSet> mMapColourToVertexIDsIsoSelfLoop = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for isolated self loop 1-simplexes (1-simplexes with same head and tail color)
	private Map<BitSet, IntSet> mMapColourToEdgeIDs1Simplex = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value
	private Map<BitSet, IntSet> mMapColourToVertexIDs1SimplexConnected = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for connected 1-simplexes
	
	//******************* Objects for 0-simplexes ************************//
	private Map<BitSet, IntSet> mMapColourToVertexIDs0Simplex = new ConcurrentHashMap<BitSet, IntSet>(); // Map for storing vertex colours as Keys and Vertex IDs as Value for 0-simplexes
	
	
	public GraphGenerationSimplexApproachS(int iNumberOfVertices,
			ColouredGraph[] inputGrphs, int iNumberOfThreads, long seed, int maximumIterationInput) {
		super();
		
		this.seed = seed;
		mRandom = new Random(this.seed);
		
		maximumIteration = maximumIterationInput;
		
		int iNoOfVersions; // Variable to store number of input graphs.
		
		// Logic to check number of input graphs
		iNoOfVersions = 0;
		for (ColouredGraph tempGrph: inputGrphs) {
			if(tempGrph!=null) {
				iNoOfVersions = iNoOfVersions + 1;
			}
		}
		
		//creating clone since original graphs are needed for later steps.
		ColouredGraph[] origGrphs = new ColouredGraph[iNoOfVersions];
		int i = 0;
		for (ColouredGraph inputGraph: inputGrphs) {
			if (inputGrphs[i] != null) {
				origGrphs[i] = inputGraph.clone();
				i++;
			}		
		}
		
		//number of vertices
		mIDesiredNoOfVertices = iNumberOfVertices;
		this.seed = seed+1;
		// mimic grpah
		mMimicGraph = new ColouredGraph();
		
		//copy all colour palette to the mimic graph
		copyColourPalette(origGrphs, mMimicGraph);
		
		// below maps are required during graph optimization phase
		mReversedMapClassVertices = new HashMap<Integer, BitSet>(); // map for storing class vertices and color
		mMapEdgeColoursToConnectedVertices = new HashMap<BitSet, Map<Integer, IntSet>>(); // map for storing edge colors, tail ids and head ids
		
		// colour of rdf:type edge
		mRdfTypePropertyColour = mMimicGraph.getRDFTypePropertyColour();
		
		// color mapper analyzing all the graphs (required refine process)
		mColourMapper = new ColourMappingRules();
		mColourMapper.analyzeRules(origGrphs);
	
		// Compute triangles for input graphs
		FindTri computedTriangles = callMetricToGetTriangleInformation(origGrphs);
		
		// Get all triangles found in input graphs. Note:- metric is invoked by above function call, thus set of colors for different triangle vertices are already computed.
		mTriangleColoursTriangleEdgeCounts = computedTriangles.getmTriColoEdgesTriCountDistAvg();
		
		// Create HashSet of Triangle Colours. This set is used to randomly select an object of TriangleColours while generating mimic graph.
		createSetForTriangleColours();
		
		//******************* Simplex Analysis connected triangles ********************//
		ConnS2 connTriAnalysis = new ConnS2(origGrphs, mIDesiredNoOfVertices, iNoOfVersions, computedTriangles);
		mColourMapperTriangles = connTriAnalysis.getmColourMapperSimplexes();
		estimatedEdgesTriangle = connTriAnalysis.getEstEdges();
		estimatedVerticesTriangle = connTriAnalysis.getEstVertices();
		
		//******************* Simplex Analysis isolated triangles ********************//
		IsoS2 isoTriAnalysis = new IsoS2(origGrphs, mIDesiredNoOfVertices, iNoOfVersions, computedTriangles);
		mColourMapperIsolatedTriangles = isoTriAnalysis.getmColourMapperSimplexes();
		estimatedEdgesIsolatedTriangle = isoTriAnalysis.getEstEdges();
		estimatedVerticesIsolatedTriangle = isoTriAnalysis.getEstVertices();
		
		//****************** Compute Triangle distributions *********************//
		triangleDistribution = new TriDistI(connTriAnalysis.getmTriColoEdgesTriCountDistAvg(), isoTriAnalysis.getmIsolatedTriColoEdgesTriCountDistAvg(), iNoOfVersions, mIDesiredNoOfVertices, mRandom);
		mTriColosCountsAvgProb = triangleDistribution.getmTriangleColorsv1v2v3(); // get hash map for triangle vertex colors storing count of triangle distribution
		
		//****************** Simplex Analysis 1-Simplexes connected to 2-simplexes **********************//
		S1ConnToS2 s1ConnToTri = new S1ConnToS2(origGrphs, mIDesiredNoOfVertices, iNoOfVersions, computedTriangles);
		mColourMapperCommonEdges = s1ConnToTri.getmColourMapperSimplexes();
		estimatedEdgesCommon = s1ConnToTri.getEstEdges();
		estVerts1SimplexesConntoTri = s1ConnToTri.getEstVertices();
		
		// Initializing hash map used for storing triangle colors and list of vertices for them
		mTriangleColorsVertexIds = new ObjectObjectOpenHashMap<TriColos, List<IntSet>>();
		
		//******************* Compute Distribution of 1-simplexes connected to triangles ****************************//
		s1connToTriDist = new EdgeDistIS(s1ConnToTri.getmHeadColoCount(), s1ConnToTri.getmHeadColoTailColoCount() , iNoOfVersions, mRandom);
		s1connToTriVertDist = new VertDistI(s1ConnToTri.getmColoCountVertConnectedToTriangle(), iNoOfVersions, mRandom);
		
		
		//***************** Simplex Analysis 1-Simplexes connecting Triangles *************************//
		S1ConnectingS2 s1ConnectingTri = new S1ConnectingS2(origGrphs, mIDesiredNoOfVertices, iNoOfVersions, computedTriangles);
		estimatedEdges1SimplexesConnect2Simplexes = s1ConnectingTri.getEstEdges();
		mColourMapper1SimplexesConnTri = s1ConnectingTri.getmColourMapperSimplexes();
		
		//**************** Compute Distribution of 1-simplexes connecting Triangles ****************************//
		s1connTriDist = new EdgeDistIS(s1ConnectingTri.getmHeadColoCount(), s1ConnectingTri.getmHeadColoTailColoCount(), iNoOfVersions, mRandom);
		
		removeTypeEdgesFromGraphs(origGrphs); // commenting removal of edges since node iterator creates a new graph to compute triangles and the computed edge Ids don't match with the original graph
		// Initially, tried not to remove the RDF type edges and instead use RDF type edges to identify class nodes. 
		//Seems to be some issue with this approach. Thus, removing type edges and then computing 1- and 0-Simplexes
		
		//**************** Self Loop Analysis ******************************//
		// Isolated triangles
		FindSelfLoops selfLoopIsoTri = new FindSelfLoops( origGrphs, mIDesiredNoOfVertices, iNoOfVersions, isoTriAnalysis.getmGraphsVertIds());
		estEdgesSelfLoopIsoTri = selfLoopIsoTri.getEstEdges();
		mColourMapperSelfLoopIsoTri = selfLoopIsoTri.getmColourMapperSimplexes();
		selfLoopIsoTritDist = new VertDistI(selfLoopIsoTri.getmColoCountSelfLoop(), iNoOfVersions, mRandom);
		
		// Connected triangles
		FindSelfLoops selfLoopConnTri = new FindSelfLoops( origGrphs, mIDesiredNoOfVertices, iNoOfVersions, connTriAnalysis.getmGraphsVertIds());
		estEdgesSelfLoopConnTri = selfLoopConnTri.getEstEdges();
		mColourMapperSelfLoopConnTri = selfLoopConnTri.getmColourMapperSimplexes();
		selfLoopConnTriDist = new VertDistI(selfLoopConnTri.getmColoCountSelfLoop(), iNoOfVersions, mRandom);
		
		// 1-simplexes only connected triangles
		FindSelfLoops selfLoops1ConnToTri = new FindSelfLoops( origGrphs, mIDesiredNoOfVertices, iNoOfVersions, s1ConnToTri.getmGraphsVertIds());
		estEdgesSelfLoop1SimplexConnToTri = selfLoops1ConnToTri.getEstEdges();
		mColourMapperSelfLoop1SimplexConnToTri = selfLoops1ConnToTri.getmColourMapperSimplexes();
		selfLoops1ConnToTriDist = new VertDistI(selfLoops1ConnToTri.getmColoCountSelfLoop(), iNoOfVersions, mRandom);

		
		//************* Simplex Analysis for isolated 1-Simplexes ********************************//
		Isolated1Simplexes isoS1Analysis = new Isolated1Simplexes(origGrphs, mIDesiredNoOfVertices, iNoOfVersions);
		mColourMapper1Simplexes = isoS1Analysis.getmColourMapperSimplexes();
		estimatedEdges1Simplexes = isoS1Analysis.getEstEdges();
		estimatedVertices1Simplexes = isoS1Analysis.getEstVertices();
		s1IsoDist = new EdgeDistIS(isoS1Analysis.getmHeadColoCount1Simplex(), isoS1Analysis.getmHeadColoTailColoCount(), iNoOfVersions, mRandom); // Distribution for isolated 1-simplexes
		
		// ******* Computations related to isolated self loops (1-simplexes with same head and tail) ***************//
		IsolatedSelfLoops isoS1SelfLoopAnalysis = new IsolatedSelfLoops(origGrphs, mIDesiredNoOfVertices, iNoOfVersions);
		mColourMapperIsoSelfLoop = isoS1SelfLoopAnalysis.getmColourMapperSimplexes();
		estimatedEdgesIsoSelfLoop = isoS1SelfLoopAnalysis.getEstEdges();
		estimatedVerticesIsoSelfLoop = isoS1SelfLoopAnalysis.getEstVertices();
		selfLoops1IsoS1 = new VertDistI(isoS1SelfLoopAnalysis.getmColoCountSelfLoop(), iNoOfVersions, mRandom);
		
		// ****************** Self loops in 1-simplexes **********************//
		FindSelfLoops selfLoopsInIsoS1 = new FindSelfLoops( origGrphs, mIDesiredNoOfVertices, iNoOfVersions, isoS1Analysis.getmGraphsVertIds());
		estimatedEdgesSelfLoopIn1Simplex = selfLoopsInIsoS1.getEstEdges();
		mColourMapperSelfLoopIn1Simplex = selfLoopsInIsoS1.getmColourMapperSimplexes();
		selfLoopsInS1Dist = new VertDistI(selfLoopsInIsoS1.getmColoCountSelfLoop(), iNoOfVersions, mRandom);
		
		//************ 0-Simplexes Analysis *********************************//
		S0C s0Analysis = new S0C(origGrphs, mIDesiredNoOfVertices, iNoOfVersions);
		estimatedVertices0Simplexes = s0Analysis.getEstVertices();
		s0Dist = new VertDistI(s0Analysis.getmColoCount0Simplex(), iNoOfVersions, mRandom);
		
		
		//************* Connected 1-Simplexes Analysis ****************************//
		ObjectObjectOpenHashMap<Integer, IntSet> edgeIdsUnionMap = addEdgeIdsForDifferentSimplexes(connTriAnalysis.getmGraphsEdgesIds(), isoTriAnalysis.getmGraphsEdgesIds(), s1ConnToTri.getmGraphsEdgesIds(), s1ConnectingTri.getmGraphsEdgesIds(), selfLoopIsoTri.getmGraphsEdgesIds(),
				selfLoopConnTri.getmGraphsEdgesIds(), selfLoops1ConnToTri.getmGraphsEdgesIds(), isoS1Analysis.getmGraphsEdgesIds(), isoS1SelfLoopAnalysis.getmGraphsEdgesIds(), selfLoopsInIsoS1.getmGraphsEdgesIds(), iNoOfVersions);
		
		Connected1Simplexes connS1Analysis = new Connected1Simplexes(origGrphs, mIDesiredNoOfVertices, iNoOfVersions, edgeIdsUnionMap);
		mColourMapperConnected1Simplexes = connS1Analysis.getmColourMapperSimplexes();
		estimatedEdgesConnS1 = connS1Analysis.getEstEdges();
		estimatedVertsConnS1 = connS1Analysis.getEstVertices();
		s1ConnDist = new EdgeDistIS(connS1Analysis.getmHeadColoCountConnected1Simplex(), connS1Analysis.getmHeadColoTailColoCountConnected(), iNoOfVersions, mRandom);
		
		// Self loop analysis for connected 1-Simplexes
		FindSelfLoops selfLoopsInConnS1 = new FindSelfLoops( origGrphs, mIDesiredNoOfVertices, iNoOfVersions, connS1Analysis.getmGraphsVertIds());
		estEdgesSelfLoopConn1Simplexes = selfLoopsInConnS1.getEstEdges();
		mColourMapperSelfLoopConn1Simplexes = selfLoopsInConnS1.getmColourMapperSimplexes();
		selfLoopsInConnS1Dist = new VertDistI(selfLoopsInConnS1.getmColoCountSelfLoop(), iNoOfVersions, mRandom);
		
		// compute estimated edges
		mIDesiredNoOfEdges = estimateNoEdges(inputGrphs, mIDesiredNoOfVertices);
		
		
	}
	
	/**
	 * The function computes the union of edge ids found in different input graphs for vairous simplexes. The edge ids are provided as input maps.
	 */
	private ObjectObjectOpenHashMap<Integer, IntSet> addEdgeIdsForDifferentSimplexes(ObjectObjectOpenHashMap<Integer, IntSet> m1,
			ObjectObjectOpenHashMap<Integer, IntSet> m2, ObjectObjectOpenHashMap<Integer, IntSet> m3,
			ObjectObjectOpenHashMap<Integer, IntSet> m4, ObjectObjectOpenHashMap<Integer, IntSet> m5,
			ObjectObjectOpenHashMap<Integer, IntSet> m6, ObjectObjectOpenHashMap<Integer, IntSet> m7,
			ObjectObjectOpenHashMap<Integer, IntSet> m8, ObjectObjectOpenHashMap<Integer, IntSet> m9,
			ObjectObjectOpenHashMap<Integer, IntSet> m10, int iNoOfVersions) {
		
		ObjectObjectOpenHashMap<Integer, IntSet> outputMap = new ObjectObjectOpenHashMap<Integer, IntSet>();
		for (int graphId = 1; graphId <= iNoOfVersions; graphId++) {
			IntSet result1 = IntSetUtil.union(IntSetUtil.union(IntSetUtil.union(IntSetUtil.union(m1.get(graphId), m2.get(graphId)), m3.get(graphId)), m4.get(graphId)), m5.get(graphId));
			IntSet result2 = IntSetUtil.union(IntSetUtil.union(IntSetUtil.union(IntSetUtil.union(m6.get(graphId), m7.get(graphId)), m8.get(graphId)), m9.get(graphId)), m10.get(graphId));
			IntSet finalResult = IntSetUtil.union(result1, result2);
			outputMap.put(graphId, finalResult);
		}
		
		return outputMap;
		
	}
	
	private FindTri callMetricToGetTriangleInformation(ColouredGraph[] origGrphs) {
		// Initialize FindTriangleObject, which will collect triangles found in Input graphs.
		FindTri findTriObj = new FindTri();
		for (ColouredGraph graph : origGrphs) {
			if (graph!= null) {
				// compute triangles information for input graph
				findTriObj.computeTriangles(graph);
			}
		}
		return findTriObj;
	}
	
	private void createSetForTriangleColours() {
		// Initialize set variable
		setAllTriangleColours = new HashSet<TriColos>();
		
		Object[] keysTriangleColours = mTriangleColoursTriangleEdgeCounts.keys;
		for(int i = 0; i < keysTriangleColours.length ; i++) {
			if(mTriangleColoursTriangleEdgeCounts.allocated[i]) {
				TriColos triangleColorObj = (TriColos) keysTriangleColours[i];
				setAllTriangleColours.add(triangleColorObj);
			}
		}
	}
	
	@Override
	public ColouredGraph generateGraph(){
		
		//*************************************** 2-simplex creation (that could be connected to each other) ************************************************//
		LOGGER.info("Case 1: Model higher dimensional simplexes with 2-simplexes");
		LOGGER.info("Estimated Edges: " + estimatedEdgesTriangle);
		LOGGER.info("Estimated Vertices: " + estimatedVerticesTriangle);
		// get random triangle
		TriColos initialRandomTriangle = getRandomTriangle();
		
		// Variable to track number of edges added to mimic graph in previous iteration.
		// Note: This variable is used to stop the iteration if no new edges could be added to the mimic graph after trying for predefined number of iterations
		int numOfIterationAddingEdgesToGraph = 0;
		
		if ((initialRandomTriangle!=null) && (estimatedEdgesTriangle >= 3) && (estimatedVerticesTriangle >=3)) { 
			
			// Update the count of triangles in the map
			double[] arrTriProbCount = mTriColosCountsAvgProb.get(initialRandomTriangle.getA()).get(initialRandomTriangle.getB()).get(initialRandomTriangle.getC());
			arrTriProbCount[3] = arrTriProbCount[3] - 1; // Triangle count is stored at first index, updating its count.
			// Note: As it is the first triangle that is added not validating if we are allowed to add a triangle or not.
			
			// Variables to track number of edges and vertices added in triangle
			int actualEdgesInTriangles = 0;
			int actualVerticesInTriangles = 0;
			
			// Variable to track edges that cannot form triangle
			IntSet edgesNotFormingTriangle = new DefaultIntSet(Constants.DEFAULT_SIZE);
			
			// Variable to track set of triangle added to the mimic graph (i.e. set of Colors of the vertices forming the triangle)
			Set<TriColos> setTriangleColorsMimicGraph = new HashSet<TriColos>();
			
			// add the selected random triangle to mimic graph			
			addTriangleToMimicGraph( initialRandomTriangle, mColourMapperTriangles, mMapColourToVertexIDs2Simplex, mMapColourToEdgeIDs2Simplex, mTriangleColorsVertexIds);
			
			//increment no of vertices in triangle
			actualVerticesInTriangles = actualVerticesInTriangles + 3;
			
			//increment no. of edges in triangle
			actualEdgesInTriangles = actualEdgesInTriangles + 3;
			
			// Add the triangle colors to set variable
			setTriangleColorsMimicGraph.add(initialRandomTriangle);
			
			// 
			while(actualEdgesInTriangles< estimatedEdgesTriangle) {
				
				if(actualVerticesInTriangles < estimatedVerticesTriangle) {
					//If we can add more triangles when we are allowed to include additional vertices otherwise edges need to be added to existing triangles
				
					// get all edges of the mimic graph
					IntSet edgesMimicGraph = mMimicGraph.getEdges();
					edgesMimicGraph = IntSetUtil.difference(edgesMimicGraph, edgesNotFormingTriangle);
					
					if (edgesMimicGraph.size() != 0) {
						// Continue to randomly select an edge and grow the graph by adding triangle only if candidate edges are found. These candidate edges will be evaluated to check if triangles can be created for them.
			
						// select an edge at random from the mimic graph
						int randomEdgeID = edgesMimicGraph.toArray(new Integer[edgesMimicGraph.size()])[mRandom.nextInt(edgesMimicGraph.size())];
						IntSet verticesIncidentToEdge = mMimicGraph.getVerticesIncidentToEdge(randomEdgeID);
				
						// Find the vertices for the randomly selected edge
						int tempIndex = 0;
						int selectedVertex1 = -1, selectedVertex2 = -1;
						for (int vertexIDTemp: verticesIncidentToEdge) {
							if (tempIndex == 0) {
								selectedVertex1 = vertexIDTemp;
								tempIndex++;
							} else {
								selectedVertex2 = vertexIDTemp;
							}
						}
				
						// Get colors for the selected vertices
						if ((selectedVertex1 == -1) || (selectedVertex2 == -1)) {
							throw new RuntimeException("Vertices not selected correctly for adding triangles. Vertex ID 1: " + selectedVertex1 + ", Vertex ID 2: " + selectedVertex2 + ". Edge ID: " + randomEdgeID);
						}
						
						BitSet selectedVertex1Colo = mMimicGraph.getVertexColour(selectedVertex1);
						BitSet selectedVertex2Colo = mMimicGraph.getVertexColour(selectedVertex2);
				
						//Get the color for the third vertex
						BitSet proposedVertex3Colo = triangleDistribution.proposeVertexColorForVertex3(selectedVertex1Colo, selectedVertex2Colo);
				
						// Add new Triangle for the selected vertices
						
						// boolean variable to track if new edge are added
						boolean newEdgesNotAddedToTriangle = true;
						
						if (proposedVertex3Colo != null) {
							// If third vertex color is proposed, create a triangle with it
							
							// create a temporary triangle colors object
							TriColos newPossibleTriangle = new TriColos( selectedVertex1Colo, selectedVertex2Colo, proposedVertex3Colo);
							//System.out.println(newPossibleTriangle.getA());
							//System.out.println(newPossibleTriangle.getB());
							//System.out.println(newPossibleTriangle.getC());
							
							// get triangle count
							double[] arrNewPossTriProbCount = mTriColosCountsAvgProb.get(newPossibleTriangle.getA()).get(newPossibleTriangle.getB()).get(newPossibleTriangle.getC());// get count of triangle for the proposed new triangle
							
							// temporary variable to track count of loops
							int numOfLoopsTri = 0;
							
							// try to propose a color for third vertex multiple times if it is not possible to create a triangle
							while ( (arrNewPossTriProbCount[3] < 1)  && (numOfLoopsTri < 500)) { // trying to create a new triangle 100 times
								proposedVertex3Colo = triangleDistribution.proposeVertexColorForVertex3(selectedVertex1Colo, selectedVertex2Colo);
								newPossibleTriangle = new TriColos( selectedVertex1Colo, selectedVertex2Colo, proposedVertex3Colo);
								arrNewPossTriProbCount = mTriColosCountsAvgProb.get(newPossibleTriangle.getA()).get(newPossibleTriangle.getB()).get(newPossibleTriangle.getC());// get count of triangle for the proposed new triangle
								numOfLoopsTri++;
							}
							
							//if (( !setTrianglesForEdge.contains( newPossibleTriangle ) )) {
							if (arrNewPossTriProbCount[3] >= 1) {
								//Update the count of triangle since a triangle will be created
								arrNewPossTriProbCount[3] = arrNewPossTriProbCount[3] - 1;
					
								// create vertex for the proposed color
								int proposedVertId = addVertexToMimicGraph(proposedVertex3Colo, mMapColourToVertexIDs2Simplex);
						
								// add edges among selected vertices and proposed color
								//Note: Ideally properties should exist among them. since they were also forming a triangle in input graphs
								addEdgeTriangle(selectedVertex1Colo, proposedVertex3Colo, selectedVertex1, proposedVertId, mMapColourToEdgeIDs2Simplex, mColourMapperTriangles, newPossibleTriangle);
								addEdgeTriangle(selectedVertex2Colo, proposedVertex3Colo, selectedVertex2, proposedVertId, mMapColourToEdgeIDs2Simplex, mColourMapperTriangles, newPossibleTriangle);
						
								// increment number of vertices and edges added in the mimic graph for triangles
								actualVerticesInTriangles = actualVerticesInTriangles + 1;
								actualEdgesInTriangles = actualEdgesInTriangles + 2;
								
								//Add the created triangle Colors to set
								setTriangleColorsMimicGraph.add(newPossibleTriangle);
								
								
								// Track the triangle colors along with vertex ids
								updateMapTriangleColorsVertices(selectedVertex1, selectedVertex2, proposedVertId, newPossibleTriangle, mTriangleColorsVertexIds);
								
								//update the boolean variable
								newEdgesNotAddedToTriangle = false;
							} 
						} 
						
						if (newEdgesNotAddedToTriangle){
							// Logic if no third vertex color could be proposed
							// Don't consider the randomly selected edge, since it is not able to form a triangle
							edgesNotFormingTriangle.add(randomEdgeID);
						}
						
					} // end if condition - check if triangles can be added to the edges
					else {
						LOGGER.info("Growing 2-simplexes not possible.... Proposing new 2-simplex");
						// If no candidate edges exist then new random triangle should be added to the mimic graph
						TriColos randomTriangle = getRandomTriangle();
						
						// get triangle count
						double[] arrNewTriProbCount = mTriColosCountsAvgProb.get(randomTriangle.getA()).get(randomTriangle.getB()).get(randomTriangle.getC());// get count of triangle for the proposed new triangle
						
						// variable to track number of times a random triangle was selected
						int numOfIterationRandomTri = 1;
						
						// check if it is possible to add new triangle
						while ((arrNewTriProbCount[3] < 1) && (numOfIterationRandomTri < 500)) { // discontinue after trying 500 times
							randomTriangle = getRandomTriangle();
							arrNewTriProbCount = mTriColosCountsAvgProb.get(randomTriangle.getA()).get(randomTriangle.getB()).get(randomTriangle.getC());// get count of triangle for the proposed new triangle
							numOfIterationRandomTri++;
						}
						
						if (arrNewTriProbCount[3] > 1) {
							// Update the triangle count
							arrNewTriProbCount[3] = arrNewTriProbCount[3] - 1;
							
							// Add the triangle to mimic graph
							addTriangleToMimicGraph(randomTriangle, mColourMapperTriangles, mMapColourToVertexIDs2Simplex, mMapColourToEdgeIDs2Simplex, mTriangleColorsVertexIds);
							
							//increment no of vertices in triangle
							actualVerticesInTriangles = actualVerticesInTriangles + 3;
							
							//increment no. of edges in triangle
							actualEdgesInTriangles = actualEdgesInTriangles + 3;
							
							// Add the triangle colors to set variable
							setTriangleColorsMimicGraph.add(new TriColos(randomTriangle.getA(), randomTriangle.getB(), randomTriangle.getC()));
						} else {
							break; // terminate while condition if it is not possible to add random triangle
						}
						
					}
				} else {
					break; // Cannot add more vertices 
				}
			} // end while condition checking if actual number of edges is less than estimated number of edges
			LOGGER.info("Growing 2-simplexes phase completed");
			LOGGER.info("Added Edges: " + actualEdgesInTriangles);
			LOGGER.info("Added Vertices: " + actualVerticesInTriangles);
			
			LOGGER.info("Adding additional Edges to created 2-simplexes");
			
			numOfIterationAddingEdgesToGraph = 0; // initialize iteration count
			while ((actualEdgesInTriangles< estimatedEdgesTriangle) && (numOfIterationAddingEdgesToGraph < maximumIteration)) { // Case: Triangles cannot be added to the mimic graph but edges can be added to existing triangles
					
					if (setTriangleColorsMimicGraph.size() != 0) {
					
					// Logic for adding edges to existing triangles
					TriColos proposeTriangleToAddEdge = triangleDistribution.proposeTriangleToAddEdge(setTriangleColorsMimicGraph);
					
					// Best case: A triangle is returned
					List<IntSet> selectedTrianglesList = mTriangleColorsVertexIds.get(proposeTriangleToAddEdge);
					
					// randomly selecting one of these triangles
					IntSet selectedVertices = selectedTrianglesList.toArray(new IntSet[selectedTrianglesList.size()])[mRandom.nextInt(selectedTrianglesList.size())];
					
					// Considering different vertex pairs to add an edge
					
					//Convert vertices to Array
					Integer[] vertexIDExistingTriangle = selectedVertices.toArray(new Integer[selectedVertices.size()]);
					
					// creating different pairs of combinations for 3 vertices of a triangle
					List<List<Integer>> differentPairsOfVertices = new ArrayList<List<Integer>>();
					differentPairsOfVertices.add(Arrays.asList(vertexIDExistingTriangle[0], vertexIDExistingTriangle[1]));
					differentPairsOfVertices.add(Arrays.asList(vertexIDExistingTriangle[1], vertexIDExistingTriangle[2]));
					differentPairsOfVertices.add(Arrays.asList(vertexIDExistingTriangle[0], vertexIDExistingTriangle[2]));
					
					for(List<Integer> pairOfVertices: differentPairsOfVertices) {
					
						// Get vertex colours for a pair
						BitSet existingVertexColo1 = mMimicGraph.getVertexColour(pairOfVertices.get(0));
						BitSet existingVertexColo2 = mMimicGraph.getVertexColour(pairOfVertices.get(1));
						
						boolean edgeAdded = addEdgeInAnyDirectionWithDuplicateCheck(existingVertexColo1, existingVertexColo2, pairOfVertices.get(0), pairOfVertices.get(1), mMapColourToEdgeIDs2Simplex, mColourMapperTriangles);
						
						if (edgeAdded) {
							//increment no. of edges in triangle
							actualEdgesInTriangles = actualEdgesInTriangles + 1;
							numOfIterationAddingEdgesToGraph = 0;// update count to 0 since edge was added successfully
							// break iterating over pair of vertices, since an edge is found
							break;
						}else {
							numOfIterationAddingEdgesToGraph++;
						}
						
					}//end iterating over vertex pairs
					
				} // end if condition - check if candidate triangles exists for adding edges to them
				else { 
					
						// No edges can be added to existing triangles
						// Give warning and break while loop
					LOGGER.warn("Not able to add edges to existing triangles and add new triangles to the mimic graph. Estimated number is greater than actual number of edges! The process of adding triangle ends. ");
					break; // terminate out of while condition
					
				}
			
			} // end else condition adding edges to existing triangles
			
		}// end if condition - initial random triangle is not null
		LOGGER.info("Case 1 completed!");
		LOGGER.info("Added Edges: " + mMimicGraph.getEdges().size());
		LOGGER.info("Added Vertices: " + mMimicGraph.getVertices().size());
		
		//************************ Logic for isolated 1-simplexes ***********************************//
		
		//temporary variables to track addition of vertices and edges for 1-simplexes
		int actualVerticesSimplexes = 0;
		int actualEdgesSimplexes = 0;
		
		//initialize variable tracking iteration count for this case
		numOfIterationAddingEdgesToGraph = 0;
		
		// get head proposer defined for 1-simplex distribution
		OfferedItemByRandomProb<BitSet> potentialHeadColoProposer = s1IsoDist.getPotentialHeadColoProposer();
		
		LOGGER.info("Case 2a: Isolated 1-simplexes (with different source and target node)");
		LOGGER.info("Estimated Edges: " + estimatedEdges1Simplexes);
		LOGGER.info("Estimated Vertices: " + estimatedVertices1Simplexes);
		
		while((estimatedEdges1Simplexes > actualEdgesSimplexes) && (potentialHeadColoProposer != null) && (numOfIterationAddingEdgesToGraph < maximumIteration)) { // additional condition that color proposer should not be null
			// check until it is possible to add more edges for 1-simplexes
			
			//variable to track if new vertex is not added
			//boolean newVertexNotAdded = true;
			
			// get potential head for it and create vertex
			BitSet potentialheadColo = potentialHeadColoProposer.getPotentialItem();
			
			// Get potential tail color and add edge for it
			BitSet potentialTailColo = s1IsoDist.proposeVertColo(potentialheadColo).getPotentialItem();
			
			// temporary assignment for head vertex id
			int vertexIDHead = -1;
			// temporary assignment for tail vertex id
			int vertexIDTail = -1;
			
			
			
			//check if vertex can be added or existing vertex should be selected
			if ((estimatedVertices1Simplexes - actualVerticesSimplexes) >= 2 ) {
				// add a vertex if enough vertices are not created for 1-simplexes
				vertexIDHead = addVertexToMimicGraph(potentialheadColo, mMapColourToVertexIDs1Simplex);
				vertexIDTail = addVertexToMimicGraph(potentialTailColo, mMapColourToVertexIDs1Simplex);
				actualVerticesSimplexes = actualVerticesSimplexes + 2;
				
				//newVertexNotAdded = false;//set the variable to false, since new variable is added

			}else {
				// New vertex cannot be created, select an existing vertex of the potential head color at random
				IntSet vertexIDshead = mMapColourToVertexIDs1Simplex.get(potentialheadColo);
				if (vertexIDshead == null) {
					numOfIterationAddingEdgesToGraph++;
					continue; // it is possible there is no vertex exist for the proposed head color, continue to check with new head color in that case
				}
				
				
				while((vertexIDshead.size() > 0) && (vertexIDTail == -1)) { // randomly select a head vertex and check if it has a tail with proposed color
					
					vertexIDHead = vertexIDshead.toArray(new Integer[vertexIDshead.size()])[mRandom.nextInt(vertexIDshead.size())];
					
					// get neighbors of selected vertex head
					IntSet neighbors = IntSetUtil.union(mMimicGraph.getInNeighbors(vertexIDHead), mMimicGraph.getOutNeighbors(vertexIDHead));
					
					IntSet vertexIDstail = new DefaultIntSet(Constants.DEFAULT_SIZE);//temporary set for storing potential tail IDs
					
					for (int potentialTail: neighbors) {
						BitSet vertexColour = mMimicGraph.getVertexColour(potentialTail);
						if (vertexColour.equals(potentialTailColo))
							vertexIDstail.add(potentialTail);
					}
					
					if (vertexIDstail.size() > 0)
						vertexIDTail = vertexIDstail.toArray(new Integer[vertexIDstail.size()])[mRandom.nextInt(vertexIDstail.size())];
					else
						vertexIDshead.remove(vertexIDHead); // randomly select head does not have the tail node with proposed colors
				}
				
				if (vertexIDTail == -1) {
					numOfIterationAddingEdgesToGraph++;
					continue;// propose a new head and tail colors when proposed colors are not found.
				}
				
			}
			
			boolean edgeAdded = addEdgeWithTriangleCheck(potentialheadColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapper1Simplexes, false); // last parameter should always be false since we are selecting isolated 1-simplexes
			//boolean addedEdge = addEdgeInAnyDirectionWithDuplicateCheck(potentialheadColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapper1Simplexes);
			//addEdgeToMimicGraph(potentialheadColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapper1Simplexes); // commenting: using new method with duplicate check
			if (edgeAdded) {
				actualEdgesSimplexes++; // increment count if edge was added
				numOfIterationAddingEdgesToGraph = 0;
			} else {
				numOfIterationAddingEdgesToGraph++;
			}
			
			
		}
		LOGGER.info("Case 2a completed!");
		LOGGER.info("Added Edges: " + actualEdgesSimplexes);
		LOGGER.info("Added Vertices: " + actualVerticesSimplexes);
		
		//****************************** Logic for isolated self loop (i.e. 1-simplexes with same source and target node) *******************************************//
		
		actualVerticesSimplexes = 0;
		actualEdgesSimplexes = 0;
		
		//initialize variable tracking iteration count for this case
		numOfIterationAddingEdgesToGraph = 0;
		
		// get head proposer defined for 1-simplex distribution
		potentialHeadColoProposer = selfLoops1IsoS1.getPotentialColoProposer();
		
		LOGGER.info("Case 2b: Isolated self loop");
		LOGGER.info("Estimated Edges: " + estimatedEdgesIsoSelfLoop);
		LOGGER.info("Estimated Vertices: " + estimatedVerticesIsoSelfLoop);
		
		numOfIterationAddingEdgesToGraph = 0; // initialize iteration count
		while((estimatedEdgesIsoSelfLoop > actualEdgesSimplexes) && (potentialHeadColoProposer != null) && (numOfIterationAddingEdgesToGraph < maximumIteration)) { // additional condition that color proposer should not be null
			
			//variable to track if new vertex is not added
			boolean newVertexNotAdded = true;
			
			// get potential head for it and create vertex
			BitSet potentialColoSelfLoop = potentialHeadColoProposer.getPotentialItem();
			
			// temporary assignment for head vertex id
			int vertexIDHead;
			
			//check if vertex can be added or existing vertex should be selected
			if (actualVerticesSimplexes < estimatedVerticesIsoSelfLoop) {
				// add a vertex if enough vertices are not created for 1-simplexes
				vertexIDHead = addVertexToMimicGraph(potentialColoSelfLoop, mMapColourToVertexIDsIsoSelfLoop);
				actualVerticesSimplexes++;
				
				newVertexNotAdded = false;//set the variable to false, since new variable is added
				
			}else {
				// New vertex cannot be created, select an existing vertex of the potential head color at random
				IntSet vertexIDshead = mMapColourToVertexIDsIsoSelfLoop.get(potentialColoSelfLoop);
				if (vertexIDshead == null) {
					numOfIterationAddingEdgesToGraph++;
					continue; // it is possible there is no vertex exist for the proposed head color, continue to check with new head color in that case
				}
				vertexIDHead = vertexIDshead.toArray(new Integer[vertexIDshead.size()])[mRandom.nextInt(vertexIDshead.size())];
			}
			
			// temporary assignment for tail vertex id
			int vertexIDTail = vertexIDHead;
			
			boolean edgeAdded = addEdgeWithTriangleCheck(potentialColoSelfLoop, potentialColoSelfLoop, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapperIsoSelfLoop, newVertexNotAdded);
			//boolean addedEdge = addEdgeInAnyDirectionWithDuplicateCheck(potentialheadColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapper1Simplexes);
			//addEdgeToMimicGraph(potentialheadColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapper1Simplexes); // commenting: using new method with duplicate check
			if (edgeAdded) {
				actualEdgesSimplexes++; // increment count if edge was added
				numOfIterationAddingEdgesToGraph = 0;
			}else {
				numOfIterationAddingEdgesToGraph++;
			}
			
		}
		
		LOGGER.info("Case 2b completed!");
		LOGGER.info("Added Edges: " + actualEdgesSimplexes);
		LOGGER.info("Added Vertices: " + actualVerticesSimplexes);
		
		//************************ Logic for 0-simplexes ***********************************//
		LOGGER.info("Case 3: Isolated 0-simplexes");
		LOGGER.info("Estimated Vertices: " + estimatedVertices0Simplexes);
		//define proposer for 0-simplexes 
		OfferedItemByRandomProb<BitSet> potentialColoProposer0Simplex = s0Dist.getPotentialColoProposer();
		
		//initialize tracking variable
		actualVerticesSimplexes = 0;
		
		while((actualVerticesSimplexes < estimatedVertices0Simplexes) && (potentialColoProposer0Simplex != null)) {
			// get possible color
			BitSet potentialColo0Simplex = potentialColoProposer0Simplex.getPotentialItem();
			
			// Add 0-simplex to mimic graph
			addVertexToMimicGraph(potentialColo0Simplex, mMapColourToVertexIDs0Simplex);
			actualVerticesSimplexes++;
		}
		LOGGER.info("Case 3 completed!");
		LOGGER.info("Added Vertices: " + actualVerticesSimplexes);
		
		//********************** Add RDF Type Edges *************************************//
		LOGGER.info("Starting to add RDF Type edges for above cases");
		generateRDFTypeEdges(mMapColourToVertexIDs2Simplex); // Add for 2-simplexes
		generateRDFTypeEdges(mMapColourToVertexIDs1Simplex); // Add for 1-simplexes
		generateRDFTypeEdges(mMapColourToVertexIDs0Simplex); // Add for 0-simplexes
		LOGGER.info("Added RDF Type edges for above cases");
		LOGGER.info("Total Edges in mimic graph: " + mMimicGraph.getEdges().size());
		LOGGER.info("Total Vertices in mimic graph: " + mMimicGraph.getVertices().size());
		
		//*********************** Logic for connecting triangles using 1-simplexes *********************//
		// Initially, find isolated triangles. We can proceed with this case only if isolated triangles are added to the mimic graph by previous process
		LOGGER.info("Case 4: Connecting two-simplexes with 1-simplex");
		LOGGER.info("Estimated Edges: " + estimatedEdges1SimplexesConnect2Simplexes);
		
		//********************** Connect Isolated triangles *********************************//
		LOGGER.info("Finding Isolated triangles to connect (Case 4a)");
		// temporary map for storing isolated triangle colors and their vertices
		ObjectObjectOpenHashMap<TriColos, List<IntSet>> mTriangleColorsVertexIdsIsolated = new ObjectObjectOpenHashMap<TriColos, List<IntSet>>();
		
		// Variable to store colors found in isolated triangles
		Set<BitSet> allColoIsolatedTri = new HashSet<BitSet>();
		
		// iterate over all store triangle colors
		Object[] triColoskeys = mTriangleColorsVertexIds.keys;
		for (int i=0; i < triColoskeys.length; i++) {
			if (mTriangleColorsVertexIds.allocated[i]) {
				TriColos triColosToCheck = (TriColos) triColoskeys[i];
				
				//get vertices related to this triangle color
				List<IntSet> listVerticesTri = mTriangleColorsVertexIds.get(triColosToCheck);
				
				//check vertices neighbors and find isolated triangles
				for(IntSet setVerticesTri:listVerticesTri) {
					// collect all neighbors
					IntSet setNeighbors = new DefaultIntSet(Constants.DEFAULT_SIZE);
					for (int vertexIdTri: setVerticesTri) {
						setNeighbors = IntSetUtil.union(setNeighbors, mMimicGraph.getInNeighbors(vertexIdTri));
						setNeighbors = IntSetUtil.union(setNeighbors, mMimicGraph.getOutNeighbors(vertexIdTri));
					}
					
					// Remove vertices forming triangles
					setNeighbors = IntSetUtil.difference(setNeighbors, setVerticesTri);
					
					if (setNeighbors.size() == 0) {
						// isolated triangle found
						
						//Convert vertices to Array
						Integer[] vertexIDTriArr = setVerticesTri.toArray(new Integer[setVerticesTri.size()]);
					
						//update map
						updateMapTriangleColorsVertices(vertexIDTriArr[0], vertexIDTriArr[1], vertexIDTriArr[2], triColosToCheck, mTriangleColorsVertexIdsIsolated);
						
						allColoIsolatedTri.add(triColosToCheck.getA());
						allColoIsolatedTri.add(triColosToCheck.getB());
						allColoIsolatedTri.add(triColosToCheck.getC());
						
					}
					
				}
				
			}
			
		}
		
		if (mTriangleColorsVertexIdsIsolated.size() > 0) {
			// try to connect isolated triangles using 1-simplexes
			LOGGER.info("Isolated triangles found connecting them with 1-simplex.");
			
			//initialize variable to track number of edges added in the mimic graph 
			actualEdgesSimplexes = 0;
			
			//initialize variable tracking iteration count for this case
			numOfIterationAddingEdgesToGraph = 0;
			OfferedItemByRandomProb<BitSet> headColoProposerIsolatedTri = s1connTriDist.getPotentialHeadColoProposer();
			
			while((actualEdgesSimplexes < estimatedEdges1SimplexesConnect2Simplexes) && (numOfIterationAddingEdgesToGraph < maximumIteration) && (headColoProposerIsolatedTri!=null)) {
			
				// get head color from the colors available in the triangle
				BitSet potentialHeadColoIsolatedTri = null; // initialize head color
				
				
				if (headColoProposerIsolatedTri != null)//check if head color proposer is not null
					potentialHeadColoIsolatedTri = headColoProposerIsolatedTri.getPotentialItem(allColoIsolatedTri);
				
				if (potentialHeadColoIsolatedTri != null) { //check for tail color only if head color is not null
				
					// get tail colors based on head color and available colors in the triangle
					BitSet potentialTailColoIsolatedTri = null; // initialize tail color
					
					OfferedItemByRandomProb<BitSet> tailColoProposer = s1connTriDist.proposeVertColo(potentialHeadColoIsolatedTri);
					if (tailColoProposer != null) // propose a color if proposer is not null
						potentialTailColoIsolatedTri = tailColoProposer.getPotentialItem(allColoIsolatedTri);
					
					if (potentialTailColoIsolatedTri != null) { // try to connect triangles using 1-simplexes if tail color is also not null
						
						//temporary variable to store the Triangle for proposed head color
						TriColos potentialTriangleHead = null;
						
						//temporary variable to store the Triangle for proposed tail color
						TriColos potentialTriangleTail = null;
						
						//find triangle with a head color 
						Object[] triColoIsolatedkeys = mTriangleColorsVertexIdsIsolated.keys;
						for (int i=0; i< triColoIsolatedkeys.length; i++) {
							if (mTriangleColorsVertexIdsIsolated.allocated[i]) {
								TriColos triColoIsolated = (TriColos) triColoIsolatedkeys[i];
								
								// variable to track same triangles are not selected for head and tail colors
								boolean triangleFoundForHead = false;
								
								//check triangle for head color
								if (triColoIsolated.getA().equals(potentialHeadColoIsolatedTri) || triColoIsolated.getB().equals(potentialHeadColoIsolatedTri) || triColoIsolated.getC().equals(potentialHeadColoIsolatedTri) && (potentialTriangleHead == null)) {
									potentialTriangleHead = triColoIsolated;
								}
								
								//check triangle for tail color
								if (triColoIsolated.getA().equals(potentialTailColoIsolatedTri) || triColoIsolated.getB().equals(potentialTailColoIsolatedTri) || triColoIsolated.getC().equals(potentialTailColoIsolatedTri) && !triangleFoundForHead && (potentialTriangleTail == null)) {
									potentialTriangleTail = triColoIsolated;
								}
								
								//stop looking for triangles of head and tail colors once found
								if ((potentialTriangleHead!=null) && (potentialTriangleTail!=null))
									break;
							}
						}
						
						if ((potentialTriangleHead == null) || (potentialTriangleTail == null)) {
							numOfIterationAddingEdgesToGraph++;
							continue;// retry to propose a different head or tail and find triangle for it
						}
						
						// variables storing head and tail IDs for Triangles to connect
						int possHeadIDIsolatedTri = 0;
						int possTailIDIsolatedTri = 0;
						
						//get vertices for head color
						List<IntSet> possVerticesListhead = mTriangleColorsVertexIdsIsolated.get(potentialTriangleHead);
						IntSet verticesTriIsolatedhead = possVerticesListhead.get(mRandom.nextInt(possVerticesListhead.size()));

						//get vertices for tail color
						List<IntSet> possVerticesListtail = mTriangleColorsVertexIdsIsolated.get(potentialTriangleTail);
						IntSet verticesTriIsolatedtail = possVerticesListtail.get(mRandom.nextInt(possVerticesListtail.size()));
						
						// compute concrete vertex id for head color and tail color
						for (int vertexIDheadIsoTri: verticesTriIsolatedhead) {
							if (mMimicGraph.getVertexColour(vertexIDheadIsoTri).equals(potentialHeadColoIsolatedTri)) {
								possHeadIDIsolatedTri = vertexIDheadIsoTri;
								break;
							}
						}
						
						for (int vertexIDTailIsoTri: verticesTriIsolatedtail) {
							if (mMimicGraph.getVertexColour(vertexIDTailIsoTri).equals(potentialTailColoIsolatedTri)) {
								possTailIDIsolatedTri = vertexIDTailIsoTri;
								break;
							}
						}
						
						// Compute possible edge color
						Set<BitSet> possibleLinkingEdgeColours = mColourMapper1SimplexesConnTri.getPossibleLinkingEdgeColours(potentialTailColoIsolatedTri, potentialHeadColoIsolatedTri);
						BitSet possEdgeColo = possibleLinkingEdgeColours.toArray(new BitSet[possibleLinkingEdgeColours.size()])[mRandom.nextInt(possibleLinkingEdgeColours.size())];
						
						// add edge to the graph
						mMimicGraph.addEdge(possTailIDIsolatedTri, possHeadIDIsolatedTri, possEdgeColo);
						actualEdgesSimplexes++;
						numOfIterationAddingEdgesToGraph = 0;
						
						// update the mapping of edge color and tail-head IDs
						updateMappingOfEdgeColoHeadTailColo(possEdgeColo, possHeadIDIsolatedTri, possTailIDIsolatedTri);
						
						//update the map storing isolated triangles
						// get vertices for isolated triangles having head color and remove them. Since, the triangle is connected with an edge to another edge.
						List<IntSet> listHeadVerticesIsoTri = mTriangleColorsVertexIdsIsolated.get(potentialTriangleHead);
						listHeadVerticesIsoTri.remove(verticesTriIsolatedhead);
						
						// get vertices for isolated triangles having tail color and remove them.
						List<IntSet> listTailVerticesIsoTri = mTriangleColorsVertexIdsIsolated.get(potentialTriangleTail);
						listTailVerticesIsoTri.remove(verticesTriIsolatedtail);
						LOGGER.info("Two isolated triangles connected with 1-simplex");
					} else {
						numOfIterationAddingEdgesToGraph++;
					}
					
				} else {
					numOfIterationAddingEdgesToGraph++;
				}
			}
			
		}
		
		//******************************** Link set of connected triangles with 1-simplex ***************************************************//
		// Note: This is a second scenario possible for connecting 2-simplexes using 1-simplexes. Here, we use a sampling approach to select created triangles and try to link them without creating a new triangle
		LOGGER.info("Trying to connect set of connected triangles using 1-simplex (Case 4b)");
		
		//initialize variable tracking iteration count for this case
		numOfIterationAddingEdgesToGraph = 0;
		
		OfferedItemByRandomProb<BitSet> headColoProposercase4b = s1connTriDist.getPotentialHeadColoProposer();
		
		while((actualEdgesSimplexes < estimatedEdges1SimplexesConnect2Simplexes) && (numOfIterationAddingEdgesToGraph < maximumIteration) && (headColoProposercase4b != null)) { // check if we can add more edge
			
			// Propose a possible colors for head
			
			BitSet potentialHeadColocase4b = null; // initialize head color
			
			if (headColoProposercase4b != null) {
				potentialHeadColocase4b = headColoProposercase4b.getPotentialItem();
				
				// get tail colors based on head color
				OfferedItemByRandomProb<BitSet> tailColoProposercase4b = s1connTriDist.proposeVertColo(potentialHeadColocase4b);
				BitSet potentialTailColocase4b = null;
				if (tailColoProposercase4b != null) {
					potentialTailColocase4b = tailColoProposercase4b.getPotentialItem();
				}
				
				// search for triangles when potential head and tail colors are not null
				if ( (potentialHeadColocase4b!= null) && (potentialTailColocase4b!=null)) {
					
					// Temporary triangles for head and tail color
					TriColos potentialTriangleHeadCase4b = null;
					TriColos potentialTriangleTailCase4b = null;
					
					
					
					// Iterate over triangles to find head and tail colors
					Object[] connTrianglesColos = mTriangleColorsVertexIds.keys;
					for (int i=0; i< connTrianglesColos.length; i++) {
						if (mTriangleColorsVertexIds.allocated[i]) {
							TriColos tempTriColo = (TriColos) connTrianglesColos[i];
							
							// variable to track same triangles are not selected for head and tail colors
							boolean triangleFoundForHead = false;
							
							//check triangle for head color (if not already found)
							if ((tempTriColo.getA().equals(potentialHeadColocase4b) || tempTriColo.getB().equals(potentialHeadColocase4b) || tempTriColo.getC().equals(potentialHeadColocase4b)) && (potentialTriangleHeadCase4b == null)) {
								potentialTriangleHeadCase4b = tempTriColo;
								triangleFoundForHead = true;
							}
							
							//check triangle for tail color
							if ( (tempTriColo.getA().equals(potentialTailColocase4b) || tempTriColo.getB().equals(potentialTailColocase4b) || tempTriColo.getC().equals(potentialTailColocase4b)) && !triangleFoundForHead && (potentialTriangleTailCase4b == null)) {
								potentialTriangleTailCase4b = tempTriColo;
							}
							
							//stop looking for triangles of head and tail colors once found
							if ((potentialTriangleHeadCase4b!=null) && (potentialTriangleTailCase4b!=null))
								break;
							
						}// end if condition: Triangle color allocated to Open hash map
						
						
					} // end for loop
					
					
					//If triangles are not found retry by proposing a head and a tail color
					if ((potentialTriangleHeadCase4b == null) || (potentialTriangleTailCase4b == null)) {
						numOfIterationAddingEdgesToGraph++;
						continue;
					}
					
					
					//***************** Potential head and tail colors found for triangles to connect ***********//
					// Select triangles at random to connect
					// There could be multiple triangles for the found triangle colors, selecting one of them at random
					
					// We try to add edges in multiple iterations for the found head and tail colors. 
					// It is possible that such iterations do not allow to add edges between two triangles since it forms a new triangle.
					
					int numIterationsCase4b = 0; // temporary variable to track number of iterations
					
					while ( (numIterationsCase4b < 51)) {
						
						numIterationsCase4b++;//increment number of edges
						
						//get vertices for head color
						List<IntSet> possVerticesListhead = mTriangleColorsVertexIds.get(potentialTriangleHeadCase4b);
						IntSet verticesheadcase4b = possVerticesListhead.get(mRandom.nextInt(possVerticesListhead.size()));
						
						//get vertices for tail color
						List<IntSet> possVerticesListtail = mTriangleColorsVertexIds.get(potentialTriangleTailCase4b);
						IntSet verticestailcase4b = possVerticesListtail.get(mRandom.nextInt(possVerticesListtail.size()));
						
						// variables storing head and tail IDs for Triangles to connect
						int possHeadIDcase4b = 0;
						int possTailIDcase4b = 0;
						
						// compute concrete vertex id for head color and tail color
						for (int vertexIDheadtemp: verticesheadcase4b) {
							if (mMimicGraph.getVertexColour(vertexIDheadtemp).equals(potentialHeadColocase4b)) {
								possHeadIDcase4b = vertexIDheadtemp;
								break;
							}
						}
						
						for (int vertexIDTailTemp: verticestailcase4b) {
							if (mMimicGraph.getVertexColour(vertexIDTailTemp).equals(potentialTailColocase4b)) {
								possTailIDcase4b = vertexIDTailTemp;
								break;
							}
						}
						
						//Add edge between selected vertices if they do not have a vertex in common using below function call
						boolean edgeAdded = addEdgeWithTriangleCheck(potentialHeadColocase4b, potentialTailColocase4b, possHeadIDcase4b, possTailIDcase4b, mMapColourToEdgeIDs1Simplex, mColourMapper1SimplexesConnTri, true);
						if (edgeAdded) {
							actualEdgesSimplexes++;
							LOGGER.info("Successfully connected triangles using 1-simplex (Case 4b)");
							numOfIterationAddingEdgesToGraph = 0;
							
							break;//terminate while loop trying to add edge for the found head and tail color
						} else {
							numOfIterationAddingEdgesToGraph++;
						}
						
						
					}
					
					
				}// end if condition: null check for head and tail color 
				else {
					numOfIterationAddingEdgesToGraph++;
				}
			}
		}
		
		
		LOGGER.info("Case 4 completed!");
		LOGGER.info("Added Edges: " + actualEdgesSimplexes);
		
		//******************* Logic for creating isolated 2-simplexes ***********************//
		LOGGER.info("Case 5: Isolated 2-simplexes");
		LOGGER.info("Estimated Edges: " + estimatedEdgesIsolatedTriangle);
		LOGGER.info("Estimated Vertices: " + estimatedVerticesIsolatedTriangle);
		OfferedItemByRandomProb<TriColos> potentialIsolatedTriangleProposer = triangleDistribution.getPotentialIsolatedTriangleProposer(); // get isolated triangle proposer
		
		// initialize tracker variable
		actualVerticesSimplexes = 0;
		actualEdgesSimplexes = 0;
		
		// Variable to track set of triangle added to the mimic graph (i.e. set of Colors of the vertices forming the triangle)
		Set<TriColos> setIsoTriInMimicGraph = new HashSet<TriColos>();
		
		while ( ((estimatedVerticesIsolatedTriangle - actualVerticesSimplexes) >= 3) && ((estimatedEdgesIsolatedTriangle - actualEdgesSimplexes) >= 3 ) && (potentialIsolatedTriangleProposer != null) ) {
			TriColos possIsoTri = potentialIsolatedTriangleProposer.getPotentialItem();
			// add the selected random triangle to mimic graph			
			addTriangleToMimicGraph( possIsoTri, mColourMapperIsolatedTriangles, mMapColourToVertexIDs2SimplexIsolated, mMapColourToEdgeIDs2SimplexIsolated, mIsolatedTriangleColorsVertexIds);
			
			setIsoTriInMimicGraph.add(possIsoTri);
						
			//increment no of vertices in triangle
			actualVerticesSimplexes = actualVerticesSimplexes + 3;
						
			//increment no. of edges in triangle
			actualEdgesSimplexes = actualEdgesSimplexes + 3;
		}
		
		int iterationCount = 0;
		
		while ((estimatedEdgesIsolatedTriangle > actualEdgesSimplexes) && (iterationCount < maximumIteration)) {
			
			if (setIsoTriInMimicGraph.size() > 0) {
			
				// Logic for adding edges to existing triangles
				TriColos proposeTriangleToAddEdge = triangleDistribution.proposeIsoTriToAddEdge(setIsoTriInMimicGraph);
				
				// Best case: A triangle is returned
				List<IntSet> selectedTrianglesList = mIsolatedTriangleColorsVertexIds.get(proposeTriangleToAddEdge);
				
				// randomly selecting one of these triangles
				IntSet selectedVertices = selectedTrianglesList.toArray(new IntSet[selectedTrianglesList.size()])[mRandom.nextInt(selectedTrianglesList.size())];
				
				// temporary variable to track if edge was added to existing triangle
				boolean edgeNotAddedToExistingTriangle = true;
				
				// Considering different vertex pairs to add an edge
				
				//Convert vertices to Array
				Integer[] vertexIDExistingTriangle = selectedVertices.toArray(new Integer[selectedVertices.size()]);
				
				// creating different pairs of combinations for 3 vertices of a triangle
				List<List<Integer>> differentPairsOfVertices = new ArrayList<List<Integer>>();
				differentPairsOfVertices.add(Arrays.asList(vertexIDExistingTriangle[0], vertexIDExistingTriangle[1]));
				differentPairsOfVertices.add(Arrays.asList(vertexIDExistingTriangle[1], vertexIDExistingTriangle[2]));
				differentPairsOfVertices.add(Arrays.asList(vertexIDExistingTriangle[0], vertexIDExistingTriangle[2]));
				
				for(List<Integer> pairOfVertices: differentPairsOfVertices) {
				
					// Get vertex colours for a pair
					BitSet existingVertexColo1 = mMimicGraph.getVertexColour(pairOfVertices.get(0));
					BitSet existingVertexColo2 = mMimicGraph.getVertexColour(pairOfVertices.get(1));
					
					boolean edgeAdded = addEdgeInAnyDirectionWithDuplicateCheck(existingVertexColo1, existingVertexColo2, pairOfVertices.get(0), pairOfVertices.get(1), mMapColourToEdgeIDs2SimplexIsolated, mColourMapperIsolatedTriangles);
					
					if (edgeAdded) {
						//increment no. of edges in triangle
						actualEdgesSimplexes = actualEdgesSimplexes + 1;
						
						//update boolean tracker variable
						edgeNotAddedToExistingTriangle = false;
						iterationCount = 0;
						
						// break iterating over pair of vertices, since an edge is found
						break;
					}
					
				}
				
				if (edgeNotAddedToExistingTriangle) {
					iterationCount++;
				}
				
			} else {
				break; // no isolated triangles are present in the mimic graph
			}
		}
		
		LOGGER.info("Case 5 completed!");
		LOGGER.info("Added Edges: " + actualEdgesSimplexes);
		LOGGER.info("Added Vertices: " + actualVerticesSimplexes);
		
		
		//******************* Logic for connected 1-simplexes ***********************//
		
		int actualEdgesMimicGraph = mMimicGraph.getEdges().size();
		//int edgesConnected1Simplexes = mIDesiredNoOfEdges - actualEdgesMimicGraph - estimatedEdgesCommon - estEdgesSelfLoopIsoTri - estEdgesSelfLoopConnTri - estEdgesSelfLoop1SimplexConnToTri - estEdgesSelfLoopConn1Simplexes;
		int edgesConnected1Simplexes = estimatedEdgesConnS1;
				
		int actualVerticesMimicGraph = mMimicGraph.getVertices().size();
		int verticesConnected1Simplexes = mIDesiredNoOfVertices - actualVerticesMimicGraph - estVerts1SimplexesConntoTri; // subtracting estimated number of vertices for 1-simplexes connected only to triangles. Such vertices are created in next step.
		//int verticesConnected1Simplexes = estimatedVertsConnS1;
		
		// initialize tracker variable
		actualVerticesSimplexes = 0;
		actualEdgesSimplexes = 0;
		
		//initialize variable tracking iteration count for this case
		numOfIterationAddingEdgesToGraph = 0;
		
		// Get vertex color proposer
		OfferedItemByRandomProb<BitSet> potentialVertColoProposer = s1ConnDist.getPotentialHeadColoProposer();
		
		// set to track class colors. New vertices need to be created for them later
		Set<BitSet> vertexClassColoSet = new HashSet<BitSet>();
		LOGGER.info("Case 6: Connected 1-simplexes");
		LOGGER.info("Estimated Edges: " + edgesConnected1Simplexes);
		LOGGER.info("Estimated Vertices: " + verticesConnected1Simplexes);
		
		while((edgesConnected1Simplexes > actualEdgesSimplexes) && (potentialVertColoProposer != null) && (numOfIterationAddingEdgesToGraph < maximumIteration)) {
			// check until it is possible to add more edges for 1-simplexes
			
			//variable to track if new vertex is not added
			boolean newVertexNotAdded = true;
			
			// get potential head for it and create vertex
			BitSet potentialvertColo = potentialVertColoProposer.getPotentialItem();
			
			// get class colors for head color and check if class vertex exist for them
			Set<BitSet> classColourSet = mMimicGraph.getClassColour(potentialvertColo);
			int numberOfClassVertices = 0;
			for (BitSet classColour: classColourSet) {
				Integer vertexIdClass = mMapClassColourToVertexIDSimplexes.get(classColour);
				if ((vertexIdClass == null) && (!vertexClassColoSet.contains(classColour))) {
					numberOfClassVertices++;//class vertex need to be created for this vertex
					vertexClassColoSet.add(classColour);
				}
			}
			
			// temporary assignment for head vertex id
			int vertexIDHead;
			
			//check if vertex can be added or existing vertex should be selected
			if ((actualVerticesSimplexes+numberOfClassVertices) < verticesConnected1Simplexes) {
				// add a vertex if enough vertices are not created for 1-simplexes
				vertexIDHead = addVertexToMimicGraph(potentialvertColo, mMapColourToVertexIDs1SimplexConnected);
				actualVerticesSimplexes++;
				//updating estimated vertices for this case. Note: class nodes are created in next step
				verticesConnected1Simplexes = verticesConnected1Simplexes - numberOfClassVertices;
				//updating estimated edges for this case. Since type edges need to be added for this vertices depending upon number of classes assigned to the vertex
				//edgesConnected1Simplexes = edgesConnected1Simplexes - classColourSet.size();
				
				newVertexNotAdded = false;//set the variable to false, since new variable is added
				
			}else {
				// New vertex cannot be created, select an existing vertex of the potential head color at random
				IntSet vertexIDshead = mMapColourToVertexIDs1SimplexConnected.get(potentialvertColo);
				if (vertexIDshead == null) {
					numOfIterationAddingEdgesToGraph++;
					continue; // Propose another color if no vertex found.
				}
				vertexIDHead = vertexIDshead.toArray(new Integer[vertexIDshead.size()])[mRandom.nextInt(vertexIDshead.size())];
			}
			
			// Get potential tail color and add edge for it
			BitSet potentialTailColo = s1ConnDist.proposeVertColo(potentialvertColo).getPotentialItem();
			
			// get class colors for head color and check if class vertex exist for them
			classColourSet = mMimicGraph.getClassColour(potentialTailColo);
			numberOfClassVertices = 0;
			for (BitSet classColour: classColourSet) {
				Integer vertexIdClass = mMapClassColourToVertexIDSimplexes.get(classColour);
				if ((vertexIdClass == null) && (!vertexClassColoSet.contains(classColour))) {
					numberOfClassVertices++;//class vertex need to be created for this vertex
					vertexClassColoSet.add(classColour);
				}
			}
			
			// temporary assignment for tail vertex id
			int vertexIDTail;
			
			//Check similar to above
			if ((actualVerticesSimplexes+numberOfClassVertices) < verticesConnected1Simplexes) {
				vertexIDTail = addVertexToMimicGraph(potentialTailColo, mMapColourToVertexIDs1SimplexConnected);
				actualVerticesSimplexes++;
				
				//updating estimated vertices for this case. Note: class nodes are created in next step
				verticesConnected1Simplexes = verticesConnected1Simplexes - numberOfClassVertices;
				//updating estimated edges for this case. Since type edges need to be added for this vertices
				//edgesConnected1Simplexes = edgesConnected1Simplexes - classColourSet.size();
				
				newVertexNotAdded = false;//set the variable to false, since new variable is added
			} else {
				IntSet vertexIDstail = mMapColourToVertexIDs1SimplexConnected.get(potentialTailColo);
				
				if (vertexIDstail == null) {
					numOfIterationAddingEdgesToGraph++;
					continue; // Propose another color if no vertex found. 
				}
				
				vertexIDTail = vertexIDstail.toArray(new Integer[vertexIDstail.size()])[mRandom.nextInt(vertexIDstail.size())];
			}
			
			boolean edgeAdded = addEdgeWithTriangleCheck(potentialvertColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapperConnected1Simplexes, newVertexNotAdded);
			//boolean edgeAdded = addEdgeInAnyDirectionWithDuplicateCheck(potentialvertColo, potentialTailColo, vertexIDHead, vertexIDTail, mMapColourToEdgeIDs1Simplex, mColourMapperConnected1Simplexes);
			if (edgeAdded) {
				actualEdgesSimplexes++;
				numOfIterationAddingEdgesToGraph = 0;
			} else {
				numOfIterationAddingEdgesToGraph++;
			}
			
		}
		LOGGER.info("Case 6 completed!");
		LOGGER.info("Added Edges: " + actualEdgesSimplexes);
		LOGGER.info("Added Vertices: " + actualVerticesSimplexes);
		
		// Add RDF Type edges for connected 1-simplexes
		generateRDFTypeEdges(mMapColourToVertexIDs1SimplexConnected);
		LOGGER.info("Added RDF Type edges for case 6");
		LOGGER.info("Total Edges in graph: " + mMimicGraph.getEdges().size());
		LOGGER.info("Total Vertices in graph: " + mMimicGraph.getVertices().size());
		
		//******************* Logic for 2-simplexes connected to rest of the graph ********************// 
		//update temporary variables
		actualVerticesSimplexes = 0;
		
		// get actual vertices count
		actualVerticesMimicGraph = mMimicGraph.getVertices().size();
		
		// compute remaining vertices and edges
		//int remainingVertices = mIDesiredNoOfVertices - actualVerticesMimicGraph;
		
		//update the estimated counts for "2-simplexes connected" if remaining counts is greater than estimated counts
		int estimatedVerticesCommon = estVerts1SimplexesConntoTri; // initially assuming no vertices can be created for creating "2-simplexes connected" case
		//if (remainingVertices > 0) {
		//	estimatedVerticesCommon = remainingVertices;
		//}
		
		LOGGER.info("Case 7: Connect 2-simplexes to other vertices");
		LOGGER.info("Estimated Vertices: " + estimatedVerticesCommon);
		
		// get proposer of Vertex color (to create vertices connected to triangles) 
		OfferedItemByRandomProb<BitSet> potentialColoProposerForVertConnectedToTriangle = s1connToTriVertDist.getPotentialColoProposer();
		
		// set to track class colors. New vertices need to be created for them later
		vertexClassColoSet = new HashSet<BitSet>();
		
		// Add vertices if not enough vertices
		while((actualVerticesSimplexes < estimatedVerticesCommon) && (potentialColoProposerForVertConnectedToTriangle!= null)) {
			BitSet potentialvertexColo = potentialColoProposerForVertConnectedToTriangle.getPotentialItem();
			
			// check class node exists for the proposed color
			Set<BitSet> classColourSet = mMimicGraph.getClassColour(potentialvertexColo);
			int numberOfClassVertices = 0;
			for (BitSet classColour: classColourSet) {
				Integer vertexIdClass = mMapClassColourToVertexIDSimplexes.get(classColour);
				if ((vertexIdClass == null) && (!vertexClassColoSet.contains(classColour))) {
					numberOfClassVertices++;
					vertexClassColoSet.add(classColour);
				}
			}
			
			// create a new vertex only if we can add class nodes
			if ((actualVerticesSimplexes+numberOfClassVertices) < estimatedVerticesCommon) {
				addVertexToMimicGraph(potentialvertexColo, mMapColourToVertexIDsConnectedTo2Simplex);
				actualVerticesSimplexes++;
				//updating estimated vertices for this case. Note: class nodes are created in next step
				estimatedVerticesCommon = estimatedVerticesCommon - numberOfClassVertices;
			}
		}
		
		LOGGER.info("Vertex addition completed");
		LOGGER.info("Added Vertices: " + actualVerticesSimplexes);
		
		//Add RDF Type edges for vertices
		generateRDFTypeEdges(mMapColourToVertexIDsConnectedTo2Simplex);
		LOGGER.info("Added RDF Type edges for above created vertices");
		
		// get actual edges count and update temporary variable
		actualEdgesMimicGraph = mMimicGraph.getEdges().size();
		actualEdgesSimplexes = 0;
		
		//initialize variable tracking iteration count for this case
		numOfIterationAddingEdgesToGraph = 0;
		
		// compute remaining edges
		int remainingEdges = mIDesiredNoOfEdges - actualEdgesMimicGraph - estEdgesSelfLoopConnTri - estEdgesSelfLoop1SimplexConnToTri - estEdgesSelfLoopIsoTri - estimatedEdgesSelfLoopIn1Simplex - estEdgesSelfLoopConn1Simplexes;
		
		//update the estimated counts for "2-simplexes connected" if remaining counts is greater than estimated counts
		//if (remainingEdges > estimatedEdgesCommon) {
			//estimatedEdgesCommon = remainingEdges;
		//}
		
		LOGGER.info("Estimated Edges: " + estimatedEdgesCommon);
		
		// Get head color proposer for creating 1-simplexes connected to triangles
		OfferedItemByRandomProb<BitSet> potentialHeadColoCommon2Simplex = s1connToTriDist.getPotentialHeadColoProposer();
		
		while((actualEdgesSimplexes < estimatedEdgesCommon) && (potentialHeadColoCommon2Simplex != null) && (numOfIterationAddingEdgesToGraph < maximumIteration)) {
			
			// Get possible head color from distribution
			BitSet potentialheadColo = potentialHeadColoCommon2Simplex.getPotentialItem();
			
			//Get possible tail Color for the head color
			BitSet potentialTailColo = s1connToTriDist.proposeVertColo(potentialheadColo).getPotentialItem();
			
			// Check if head Color is available in 2-Simplexes
			IntSet vertsWithHeadColo = mMapColourToVertexIDs2Simplex.get(potentialheadColo);
			
			// variable to track if head color is in 2-simplex
			boolean headColoIn2Simplex = true;
			
			if (vertsWithHeadColo == null) {
				// check head color is available in connected 1-simplexes
				vertsWithHeadColo = mMapColourToVertexIDs1SimplexConnected.get(potentialheadColo);
				
				//check head color is available in new created vertex for connecting with triangle
				if (vertsWithHeadColo == null) {
					vertsWithHeadColo = mMapColourToVertexIDsConnectedTo2Simplex.get(potentialheadColo);
				}
				
				headColoIn2Simplex = false;
			}
			
			if (vertsWithHeadColo == null) {
				numOfIterationAddingEdgesToGraph++;
				// There does not exist any vertex with potential color, continue and try to use other head color
				continue;
			}
			
			
			// Get vertices with tail color from 2-simplex or connected 1-simplex depending where the vertex of head color exist
			IntSet vertsWithTailColo = null;
			if (headColoIn2Simplex) {
				vertsWithTailColo = mMapColourToVertexIDs1SimplexConnected.get(potentialTailColo);
				
				//check tail color is available in new created vertex for connecting with triangle
				if (vertsWithTailColo == null) {
					vertsWithTailColo = mMapColourToVertexIDsConnectedTo2Simplex.get(potentialTailColo);
					
				}
			}
			else
				vertsWithTailColo = mMapColourToVertexIDs2Simplex.get(potentialTailColo);
			
			if (vertsWithTailColo == null) {
				// Vertex for tail color is not found, Look for head color and tail color in different simplexes
				
				if (headColoIn2Simplex) {
					// Earlier head color was in 2-simplex, now looking in connected 1-simplex
					vertsWithHeadColo = mMapColourToVertexIDs1SimplexConnected.get(potentialheadColo);
					
					//check head color is available in new created vertex for connecting with triangle
					if (vertsWithHeadColo == null) {
						vertsWithHeadColo = mMapColourToVertexIDsConnectedTo2Simplex.get(potentialheadColo);
					}
					
				} else {
					vertsWithHeadColo = mMapColourToVertexIDs2Simplex.get(potentialheadColo);
				}
				
				// Continue if vertices are not found for head color even after swap
				if (vertsWithHeadColo == null) {
					numOfIterationAddingEdgesToGraph++;
					continue;
				}
				
				// Get vertices for tail color
				if (headColoIn2Simplex) {
					vertsWithTailColo = mMapColourToVertexIDs1SimplexConnected.get(potentialTailColo);
					//check tail color is available in new created vertex for connecting with triangle
					if (vertsWithTailColo == null) {
						vertsWithTailColo = mMapColourToVertexIDsConnectedTo2Simplex.get(potentialTailColo);
						
					}
				}
				else
					vertsWithTailColo = mMapColourToVertexIDs2Simplex.get(potentialTailColo);
				
				// Continue if vertices are not found for tail color even after swap
				if (vertsWithTailColo == null) {
					numOfIterationAddingEdgesToGraph++;
					continue;
				}
				
			}
			
			// Select any random vertex with head color
			int vertexIDWithHeadColo = vertsWithHeadColo.toArray(new Integer[vertsWithHeadColo.size()])[mRandom.nextInt(vertsWithHeadColo.size())];
			
			// Select any random vertex with head color
			int vertexIDWithTailColo = vertsWithTailColo.toArray(new Integer[vertsWithTailColo.size()])[mRandom.nextInt(vertsWithTailColo.size())];
			
			//  Get possible edge color using the mapper
			Set<BitSet> possibleLinkingEdgeColours = mColourMapperCommonEdges.getPossibleLinkingEdgeColours(potentialTailColo, potentialheadColo);
			
			// check existing edge colors between vertices and remove them from the possible edge colors set
			possibleLinkingEdgeColours = removeDuplicateEdgeColors(vertexIDWithHeadColo, vertexIDWithTailColo, possibleLinkingEdgeColours);
			
			boolean havingVertices = commonVertices(vertexIDWithHeadColo, vertexIDWithTailColo);
			
			if ((possibleLinkingEdgeColours.size() > 0) && !havingVertices) {
				//select a random edge
				BitSet possEdgeColo = possibleLinkingEdgeColours.toArray(new BitSet[possibleLinkingEdgeColours.size()])[mRandom.nextInt(possibleLinkingEdgeColours.size())];
			
				// add the edge to graph
				mMimicGraph.addEdge(vertexIDWithTailColo, vertexIDWithHeadColo, possEdgeColo);
				actualEdgesSimplexes++;
				
				numOfIterationAddingEdgesToGraph = 0;
				
				//update the map to track edge colors, tail id and head ids
				updateMappingOfEdgeColoHeadTailColo(possEdgeColo, vertexIDWithHeadColo, vertexIDWithTailColo);
				
			} else {
				numOfIterationAddingEdgesToGraph++;
			}
			
		}
		LOGGER.info("Case 7 completed!");
		LOGGER.info("Added Edges: " + actualEdgesSimplexes);
		
		//************************* Logic to add self loops for simplexes created for different cases *********************************//
		LOGGER.info("Adding Self loops...........");
		
		//Isolated 2-simplexes		
		LOGGER.info("Isolated 1-simplexes");
		addSelfLoops(estimatedEdgesSelfLoopIn1Simplex, selfLoopsInS1Dist.getPotentialColoProposer(), mMapColourToVertexIDs1Simplex, mColourMapperSelfLoopIn1Simplex, mMapColourToEdgeIDs1Simplex);
		
		//Isolated 2-simplexes		
		LOGGER.info("Isolated 2-simplexes");
		addSelfLoops(estEdgesSelfLoopIsoTri, selfLoopIsoTritDist.getPotentialColoProposer(), mMapColourToVertexIDs2SimplexIsolated, mColourMapperSelfLoopIsoTri, mMapColourToEdgeIDs2SimplexIsolated);
		
		// Connected 2-simplexes		
		LOGGER.info("Connected 2-simplexes");
		addSelfLoops(estEdgesSelfLoopConnTri, selfLoopConnTriDist.getPotentialColoProposer(), mMapColourToVertexIDs2Simplex, mColourMapperSelfLoopConnTri, mMapColourToEdgeIDs2Simplex);
		
		//1-simplexes only connected to triangles
		LOGGER.info("1-simplexes connected only to triangles");
		addSelfLoops(estEdgesSelfLoop1SimplexConnToTri, selfLoops1ConnToTriDist.getPotentialColoProposer(), mMapColourToVertexIDsConnectedTo2Simplex, mColourMapperSelfLoop1SimplexConnToTri, mMapColourToEdgeIDs1Simplex);
		
		//Connected 1-simplexes
		LOGGER.info("Connected 1-simplexes");
		addSelfLoops(estEdgesSelfLoopConn1Simplexes, selfLoopsInConnS1Dist.getPotentialColoProposer(), mMapColourToVertexIDs1SimplexConnected, mColourMapperSelfLoopConn1Simplexes, mMapColourToEdgeIDs1Simplex);
		
		System.out.println("Number of edges in the mimic graph: " + mMimicGraph.getEdges().size());
		System.out.println("Number of vertices in the mimic graph: " + mMimicGraph.getVertices().size());
		
		// Update mMapColourToVertexIDs used for adding edges when improving the graph in next phase
		updateVertexColoMap(mMapColourToVertexIDs1Simplex); //isolated 1-simplexes
		updateVertexColoMap(mMapColourToVertexIDs2Simplex); // connected 2-simplexes
		updateVertexColoMap(mMapColourToVertexIDsIsoSelfLoop); // isolated self loop
		updateVertexColoMap(mMapColourToVertexIDs2SimplexIsolated); // isolated 2-simplexes
		updateVertexColoMap(mMapColourToVertexIDs1SimplexConnected); // connected 1-simplexes
		updateVertexColoMap(mMapColourToVertexIDsConnectedTo2Simplex); // 1-simplexes connected to 2-simplexes
		
		return mMimicGraph;
	}
	
	private void addSelfLoops(int estEdgesInput, OfferedItemByRandomProb<BitSet> distColoProposerSelfLoopInput, Map<BitSet, IntSet> mMapColourToVertexIDsInput, IColourMappingRules mColourMapperSelfLoopInput, Map<BitSet, IntSet> mMapColourToEdgeIDsInput) {
		LOGGER.info("Estimated edges: " + estEdgesInput);
		int actualEdgesSimplexes = 0;
		int iterationCountSelf = 0;
		while ((estEdgesInput > actualEdgesSimplexes) && (iterationCountSelf < maximumIteration) && (distColoProposerSelfLoopInput!=null)) {
			BitSet proposedVertexColor = distColoProposerSelfLoopInput.getPotentialItem();
			IntSet possVertices = mMapColourToVertexIDsInput.get(proposedVertexColor);
			if (possVertices != null) {
				Integer vertexID = possVertices.toArray(new Integer[possVertices.size()])[mRandom.nextInt(possVertices.size())];
				boolean edgeAdded = addEdgeInAnyDirectionWithDuplicateCheck(proposedVertexColor, proposedVertexColor, vertexID, vertexID, mMapColourToEdgeIDsInput, mColourMapperSelfLoopInput);
				if (edgeAdded) {
					actualEdgesSimplexes++;
					iterationCountSelf = 0;
				} else {
					iterationCountSelf++;
				}
			} else {
				iterationCountSelf++;
			}
		}
		LOGGER.info("Added edges: " + actualEdgesSimplexes);
	}
	
	private void updateVertexColoMap(Map<BitSet, IntSet> mMapColourToVertexIDsinput) {
		Set<BitSet> keySetIso1Simplexes = mMapColourToVertexIDsinput.keySet();
		for (BitSet vertexColo: keySetIso1Simplexes) {
			IntSet tempSet = mMapColourToVertexIDs.get(vertexColo);
			if (tempSet == null)
				tempSet = new DefaultIntSet(Constants.DEFAULT_SIZE);
			tempSet.addAll(mMapColourToVertexIDsinput.get(vertexColo));
		}
	}
	
	
	private void generateRDFTypeEdges(Map<BitSet, IntSet> mMapColourToVertexIDsForTypeEdges) {
		// Check number of remaining edges to add
		int actualEdgesMimicGraph = mMimicGraph.getEdges().size();
		int remainingEdges = mIDesiredNoOfEdges - actualEdgesMimicGraph;
				
		int actualVerticesMimicGraph = mMimicGraph.getVertices().size();
		int remainingVertices = mIDesiredNoOfVertices - actualVerticesMimicGraph;
				
		if ((remainingEdges < 0) || (remainingVertices < 0)) {
			//throw new RuntimeException("Cannot Add RDF Type edges!, Remaining Vertices (to add): " + remainingVertices + ", Remaining Edges (to add): " + remainingEdges);
			LOGGER.warn("Cannot Add RDF Type edges!, Remaining Vertices (to add): " + remainingVertices + ", Remaining Edges (to add): " + remainingEdges);
		}
		
		// Variable to track number of rdf type edges & vertices for these edges added to the mimic graph
		int trackerEdgesCount = 0;
					
		// temporary variable to track colors
		Set<BitSet> classColorsCreateVertices = new HashSet<BitSet>();
		
		//create a set for vertex ids for which the type edges need to be added
		IntSet vertexIdsTypeEdges = new DefaultIntSet(Constants.DEFAULT_SIZE);
		
					
		// iterate over every vertex colour and get all class colors
		for(BitSet vertexColor : mMapColourToVertexIDsForTypeEdges.keySet()) {
			Set<BitSet> classColours = mMimicGraph.getClassColour(vertexColor);
			classColorsCreateVertices.addAll(classColours);
			vertexIdsTypeEdges.addAll(mMapColourToVertexIDsForTypeEdges.get(vertexColor));
		}
					
		// Remove existing found  class colors
		classColorsCreateVertices.removeAll(mMapClassColourToVertexIDSimplexes.keySet());
					
		if (classColorsCreateVertices.size() > remainingVertices)
			LOGGER.warn("More number of vertices are required for class nodes. Number of Vertices required: " + classColorsCreateVertices.size() + ", Remaining Vertices: " + remainingVertices);
			//throw new RuntimeException("More number of vertices are required for class nodes. Number of Vertices required: " + classColorsCreateVertices.size() + ", Remaining Vertices: " + remainingVertices);
		
		// commenting below since mMapColourToVertexIDs should store different vertex colors and not only class vertices
		// Add class vertices to mMapColourToVertexIDs
//		BitSet emptyBitset = new BitSet(); // empty bitset for storing nodes created for classes
//		IntSet vertexIDsClass = mMapColourToVertexIDs.get(emptyBitset);// get vertex ids for this empty bitset 
//		if (vertexIDsClass == null) {
//			vertexIDsClass = new DefaultIntSet(Constants.DEFAULT_SIZE);
//			mMapColourToVertexIDs.put(emptyBitset, vertexIDsClass);
//		}
					
		// create vertex for each class color and store the  in the map
		for(BitSet classColor: classColorsCreateVertices) {
			int classVertex = mMimicGraph.addVertex();
			mMapClassColourToVertexIDSimplexes.put(classColor, classVertex);
			mReversedMapClassVertices.put(classVertex, classColor); // reverse map of class color and vertex ids, required when optimizing the graph
			//vertexIDsClass.add(classVertex);//add vertices created for classes in mMapColourToVertexID // commenting this since mMapColourToVertexIDs should store different vertex colors and not only class vertices
		}
					
		// iterate over every vertex and connect it to vertices for class
		for (int vertexIdMimicGraph:vertexIdsTypeEdges) {
			// get colors for vertex id
			BitSet vertexColor = mMimicGraph.getVertexColour(vertexIdMimicGraph);
			
			// get class colors for vertex color
			Set<BitSet> classColoursForVertex = mMimicGraph.getClassColour(vertexColor);
			
			//Add Type edge for every class color
			for (BitSet classColor: classColoursForVertex) {
				if (trackerEdgesCount > remainingEdges) {
					LOGGER.info("While adding RDF Types, maximum number of edges are added");
					//return;
				}
				int classVertexId = mMapClassColourToVertexIDSimplexes.get(classColor);
				mMimicGraph.addEdge(vertexIdMimicGraph, classVertexId, mRdfTypePropertyColour);
				trackerEdgesCount++;
			}
		}
	}
	
	/**
	 * The method adds a triangle to mimic graph for the input TriangleColours Object. Edge colors are selected for the given vertex colors of the triangle to create edge and form the complete triangle
	 * @param inputTriangleColours - TriangleColours Object
	 */
	private void addTriangleToMimicGraph(TriColos inputTriangleColours, IColourMappingRules mColourMapperToUse, Map<BitSet, IntSet> mMapColourToVertexIDsToUpdate, Map<BitSet, IntSet> mMapColourToEdgeIDsToUpdate
			, ObjectObjectOpenHashMap<TriColos, List<IntSet>> mTriangleColorsVertexIdsToUpdate) {
		// storing colors of vertices for initial triangle
		BitSet vertex1Color = inputTriangleColours.getA();
		BitSet vertex2Color = inputTriangleColours.getB();
		BitSet vertex3Color = inputTriangleColours.getC();
					
		// create vertex for the triangle colors in the mimic graph
		int vert1Id = addVertexToMimicGraph(vertex1Color, mMapColourToVertexIDsToUpdate);
		int vert2Id = addVertexToMimicGraph(vertex2Color, mMapColourToVertexIDsToUpdate);
		int vert3Id = addVertexToMimicGraph(vertex3Color, mMapColourToVertexIDsToUpdate);
		
		// Add edges between found vertices
		addEdgeTriangle(vertex1Color, vertex2Color, vert1Id, vert2Id, mMapColourToEdgeIDsToUpdate, mColourMapperToUse, inputTriangleColours);
		addEdgeTriangle(vertex1Color, vertex3Color, vert1Id, vert3Id, mMapColourToEdgeIDsToUpdate, mColourMapperToUse, inputTriangleColours);
		addEdgeTriangle(vertex2Color, vertex3Color, vert2Id, vert3Id, mMapColourToEdgeIDsToUpdate, mColourMapperToUse, inputTriangleColours);
		
		// update the map for trackign the colours of the triangle
		updateMapTriangleColorsVertices(vert1Id, vert2Id, vert3Id, inputTriangleColours, mTriangleColorsVertexIdsToUpdate);
		
	}
	
	/**
	 * This method is used to update the map that tracks the triangle colours and vertices for those colours.
	 * @param vert1Id - Integer vertex id for the first vertex of the triangle
	 * @param vert2Id - Integer vertex id  for the second vertex of the triangle
	 * @param vert3Id - Integer vertex id for the third vertex of the triangle
	 * @param inputTriangleColours - TriangleColours object with the colours for the vertices of the triangle
	 */
	private void updateMapTriangleColorsVertices(int vert1Id, int vert2Id, int vert3Id, TriColos inputTriangleColours, ObjectObjectOpenHashMap<TriColos, List<IntSet>> mTriColVertexIDsToUpdate) {
		
		//list of vertex ids, initially checks if the vertex ids exists for the input triangle colours
		List<IntSet> tempVerticesList = mTriColVertexIDsToUpdate.get(inputTriangleColours);
		if (tempVerticesList == null) {
			tempVerticesList = new ArrayList<IntSet>();
		}
		
		// create set for vertex ids
		IntSet verticesOfNewTriangle = new DefaultIntSet(Constants.DEFAULT_SIZE);
		verticesOfNewTriangle.add(vert1Id);
		verticesOfNewTriangle.add(vert2Id);
		verticesOfNewTriangle.add(vert3Id);
		
		tempVerticesList.add(verticesOfNewTriangle);
		mTriColVertexIDsToUpdate.put(inputTriangleColours, tempVerticesList);
	}
	
	/**
	 * The method adds a vertex of the input color to the mimic graph and returns the vertex id. It also updates a map, which stores the mapping of vertex colors and vertex IDs.
	 * @param vertexColor - BitSet for the vertex color
	 * @return - vertex id
	 */
	private int addVertexToMimicGraph(BitSet vertexColor, Map<BitSet, IntSet> mMapColourToVertexIDsToUpdate) {
		int vertId = mMimicGraph.addVertex(vertexColor);
		IntSet setVertIDs = mMapColourToVertexIDsToUpdate.get(vertexColor); //mMapColourToVertexIDs.get(vertexColor);
		if(setVertIDs == null){
			setVertIDs = new DefaultIntSet(Constants.DEFAULT_SIZE);
			mMapColourToVertexIDsToUpdate.put(vertexColor, setVertIDs); //mMapColourToVertexIDs.put(vertexColor, setVertIDs);
			mMapColourToVertexIDs.put(vertexColor, setVertIDs); // updating this map, required when refining the graph
		}
		setVertIDs.add(vertId);
		
		return vertId;
	}
	
	/**
	 * 
	 * This method add an edge for the input vertex 1 and vertex 2. It utilizes colormapper object to determine edge color and the head and tail vertex.
	 * For the edge color found, an edge is added between vertex 1 and vertex 2, and the edge id is stored with its color information in the map.
	 * @param inputVertex1Colo - Color for input vertex 1.
	 * @param inputVertex2Colo - Color for input vertex 2.
	 * @param inputVertex1ID - ID for input vertex 1.
	 * @param inputVertex2ID - ID for input vertex 2.
	 */
//	private void addEdgeToMimicGraph(BitSet inputVertex1Colo, BitSet inputVertex2Colo, int inputVertex1ID, int inputVertex2ID, Map<BitSet, IntSet> mMapColourToEdgeIDsToUpdate, IColourMappingRules mColourMapperToUse) {
//		boolean isEdgeFromFirstToSecondVertex = true;
//		// Get edge between vertex1 and vertex 2, assuming vertex 1 is tail and vertex 2 is head
//		Set<BitSet> possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex1Colo, inputVertex2Colo);
//		if (possEdgeColov1tailv2head.size() == 0) {
//			// When vertex 1 is not tail and vertex 2 is not head
//			// get edge assuming vertex 1 is head and vertex 2 is tail
//			possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex2Colo, inputVertex1Colo);
//			isEdgeFromFirstToSecondVertex = false;
//		}
//		
//		
//		if (possEdgeColov1tailv2head.size() != 0) { // Add edge if edge color is found for the vertices
//			
//		// randomly select edge colo
//		BitSet randomEdgeColov1v2 = possEdgeColov1tailv2head.toArray(new BitSet[possEdgeColov1tailv2head.size()])[mRandom.nextInt(possEdgeColov1tailv2head.size())];
//		
//		// add the edge to mimic graph
//		int edgeIdTemp;
//		if (isEdgeFromFirstToSecondVertex)
//			edgeIdTemp = mMimicGraph.addEdge(inputVertex1ID, inputVertex2ID, randomEdgeColov1v2);
//		else
//			edgeIdTemp = mMimicGraph.addEdge(inputVertex2ID, inputVertex1ID, randomEdgeColov1v2);
//		
//		// Update or Add to the mapping of edge color and edge Id
//		// Note: This generator does uses Real Edge IDs instead of fake IDs, as compared to previously developed generators. 
//		IntSet setOfEdgeIds = mMapColourToEdgeIDsToUpdate.get(randomEdgeColov1v2);//mMapColourToEdgeIDs.get(randomEdgeColov1v2);
//		if (setOfEdgeIds == null) {
//			setOfEdgeIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
//			mMapColourToEdgeIDsToUpdate.put(randomEdgeColov1v2, setOfEdgeIds); //mMapColourToEdgeIDs.put(randomEdgeColov1v2, setOfEdgeIds);
//		}
//		setOfEdgeIds.add(edgeIdTemp);
//		
//		}else {
//			LOGGER.warn("No edge could be found for vertices with Colors: " + inputVertex1Colo + ", " + inputVertex2Colo);
//		}
//	}
	
	/**
	 * 
	 * This method add an edge for the input vertex 1 and vertex 2. It utilizes colormapper object to determine edge color and the head and tail vertex.
	 * For the edge color found, an edge is added between vertex 1 and vertex 2, and the edge id is stored with its color information in the map.
	 * In contrast with earlier method, this method specifically evaluates the existing edge colors between input vertices and does not add an edge with duplicate color
	 * @param inputVertex1Colo - Color for input vertex 1.
	 * @param inputVertex2Colo - Color for input vertex 2.
	 * @param inputVertex1ID - ID for input vertex 1.
	 * @param inputVertex2ID - ID for input vertex 2.
	 */
	private boolean addEdgeTriangle(BitSet inputVertex1Colo, BitSet inputVertex2Colo, int inputVertex1ID, int inputVertex2ID, Map<BitSet, IntSet> mMapColourToEdgeIDsToUpdate, IColourMappingRules mColourMapperToUse, TriColos inputTriangleColours) {
		boolean isEdgeFromFirstToSecondVertex = true;
		// Get edge between vertex1 and vertex 2, assuming vertex 1 is tail and vertex 2 is head
		Set<BitSet> possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex1Colo, inputVertex2Colo);
		if (possEdgeColov1tailv2head.size() == 0) {
			// When vertex 1 is not tail and vertex 2 is not head
			// get edge assuming vertex 1 is head and vertex 2 is tail
			possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex2Colo, inputVertex1Colo);
			isEdgeFromFirstToSecondVertex = false;
		}
		
		if (possEdgeColov1tailv2head.size() != 0) { // Add edge if edge color is found for the vertices
			
		// randomly select edge colo
		BitSet randomEdgeColov1v2 = possEdgeColov1tailv2head.toArray(new BitSet[possEdgeColov1tailv2head.size()])[mRandom.nextInt(possEdgeColov1tailv2head.size())];
		
		// Get the map storing edge colors and corresponding tail and head ids
		Map<Integer, IntSet> mTailHead = mMapEdgeColoursToConnectedVertices.get(randomEdgeColov1v2);
		if (mTailHead == null) {
			mTailHead = new HashMap<Integer, IntSet>();
		}
		
		// initialize head ids for the map
		IntSet headIds = null;
		
		// add the edge to mimic graph
		int edgeIdTemp;
		if (isEdgeFromFirstToSecondVertex) {
			edgeIdTemp = mMimicGraph.addEdge(inputVertex1ID, inputVertex2ID, randomEdgeColov1v2);
			
			// get existing head ids if present
			headIds = mTailHead.get(inputVertex1ID);
			if (headIds == null) {
				headIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
			}
			headIds.add(inputVertex2ID);
			
			//update the map for tail id and head ids
			mTailHead.put(inputVertex1ID, headIds);
			
		}
		else {
			edgeIdTemp = mMimicGraph.addEdge(inputVertex2ID, inputVertex1ID, randomEdgeColov1v2);
			
			
			headIds = mTailHead.get(inputVertex2ID);
			if (headIds == null) {
				headIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
			}
			headIds.add(inputVertex1ID);
			
			mTailHead.put(inputVertex2ID, headIds);
			
		}
		
		mMapEdgeColoursToConnectedVertices.put(randomEdgeColov1v2, mTailHead);
		
		
		
		// Update or Add to the mapping of edge color and edge Id
		// Note: This generator does uses Real Edge IDs instead of fake IDs, as compared to previously developed generators. 
		IntSet setOfEdgeIds = mMapColourToEdgeIDsToUpdate.get(randomEdgeColov1v2);//mMapColourToEdgeIDs.get(randomEdgeColov1v2);
		if (setOfEdgeIds == null) {
			setOfEdgeIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
			mMapColourToEdgeIDsToUpdate.put(randomEdgeColov1v2, setOfEdgeIds); //mMapColourToEdgeIDs.put(randomEdgeColov1v2, setOfEdgeIds);
		}
		setOfEdgeIds.add(edgeIdTemp);
		
		return true;
		
		}else {
			return false;
		}
	}
	
	/**
	 * 
	 * This method add an edge for the input vertex 1 and vertex 2. It utilizes colormapper object to determine edge color and the head and tail vertex.
	 * For the edge color found, an edge is added between vertex 1 and vertex 2, and the edge id is stored with its color information in the map.
	 * In contrast with earlier method, this method specifically evaluates the existing edge colors between input vertices and does not add an edge with duplicate color
	 * @param inputVertex1Colo - Color for input vertex 1.
	 * @param inputVertex2Colo - Color for input vertex 2.
	 * @param inputVertex1ID - ID for input vertex 1.
	 * @param inputVertex2ID - ID for input vertex 2.
	 */
	private boolean addEdgeInAnyDirectionWithDuplicateCheck(BitSet inputVertex1Colo, BitSet inputVertex2Colo, int inputVertex1ID, int inputVertex2ID, Map<BitSet, IntSet> mMapColourToEdgeIDsToUpdate, IColourMappingRules mColourMapperToUse) {
		boolean isEdgeFromSecondToFirstVertex = true;
		// Get edge between vertex1 and vertex 2, assuming vertex 1 is tail and vertex 2 is head
		Set<BitSet> possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex2Colo, inputVertex1Colo);
		if (possEdgeColov1tailv2head.size() == 0) {
			// When vertex 1 is not tail and vertex 2 is not head
			// get edge assuming vertex 1 is head and vertex 2 is tail
			possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex1Colo, inputVertex2Colo);
			isEdgeFromSecondToFirstVertex = false;
		}
		
		// Check for duplicate edge color if it is essential
		// Note: This check is not required when a triangle is created for the first time or a edge is created between vertices for the first time
		possEdgeColov1tailv2head = removeDuplicateEdgeColors(inputVertex2ID, inputVertex1ID, possEdgeColov1tailv2head);	
		
		// when no edge can be added and second assumption is not evaluated (vertex 1 - head and vertex 2 - tail) get edge colors for the second case
		if ((possEdgeColov1tailv2head.size() == 0) && isEdgeFromSecondToFirstVertex) {
			possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(inputVertex1Colo, inputVertex2Colo);
			isEdgeFromSecondToFirstVertex = false;
				
			possEdgeColov1tailv2head = removeDuplicateEdgeColors(inputVertex2ID, inputVertex1ID, possEdgeColov1tailv2head);
		} 
		
		if (possEdgeColov1tailv2head.size() != 0) { // Add edge if edge color is found for the vertices
			
		// randomly select edge colo
		BitSet randomEdgeColov1v2 = possEdgeColov1tailv2head.toArray(new BitSet[possEdgeColov1tailv2head.size()])[mRandom.nextInt(possEdgeColov1tailv2head.size())];
		
		// Get the map storing edge colors and corresponding tail and head ids
		Map<Integer, IntSet> mTailHead = mMapEdgeColoursToConnectedVertices.get(randomEdgeColov1v2);
		if (mTailHead == null) {
			mTailHead = new HashMap<Integer, IntSet>();
		}
		
		// initialize head ids for the map
		IntSet headIds = null;
		
		// add the edge to mimic graph
		int edgeIdTemp;
		if (isEdgeFromSecondToFirstVertex) {
			edgeIdTemp = mMimicGraph.addEdge(inputVertex2ID, inputVertex1ID, randomEdgeColov1v2);
			
			// get existing head ids if present
			headIds = mTailHead.get(inputVertex2ID);
			if (headIds == null) {
				headIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
			}
			headIds.add(inputVertex1ID);
						
			//update the map for tail id and head ids
			mTailHead.put(inputVertex2ID, headIds);
		}
		else {
			edgeIdTemp = mMimicGraph.addEdge(inputVertex1ID, inputVertex2ID, randomEdgeColov1v2);
			
			headIds = mTailHead.get(inputVertex1ID);
			if (headIds == null) {
				headIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
			}
			headIds.add(inputVertex2ID);
			
			mTailHead.put(inputVertex1ID, headIds);
		}
		
		mMapEdgeColoursToConnectedVertices.put(randomEdgeColov1v2, mTailHead);
		
		// Update or Add to the mapping of edge color and edge Id
		// Note: This generator does uses Real Edge IDs instead of fake IDs, as compared to previously developed generators. 
		IntSet setOfEdgeIds = mMapColourToEdgeIDsToUpdate.get(randomEdgeColov1v2);//mMapColourToEdgeIDs.get(randomEdgeColov1v2);
		if (setOfEdgeIds == null) {
			setOfEdgeIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
			mMapColourToEdgeIDsToUpdate.put(randomEdgeColov1v2, setOfEdgeIds); //mMapColourToEdgeIDs.put(randomEdgeColov1v2, setOfEdgeIds);
		}
		setOfEdgeIds.add(edgeIdTemp);
		
		return true;
		
		}else {
			return false;
		}
	}
	
	/**
	 * The method returns true if the input vertices have one or more vertices in common.
	 * @param headID
	 * @param tailID
	 * @return
	 */
	public boolean commonVertices(int headID, int tailID) {
		// get vertices incident on input ids
		IntSet verticesIncidentHead = IntSetUtil.union(mMimicGraph.getInNeighbors(headID), mMimicGraph.getOutNeighbors(headID));
		IntSet verticesIncidentTail = IntSetUtil.union(mMimicGraph.getInNeighbors(tailID), mMimicGraph.getOutNeighbors(tailID));
		
		//find vertices incident to both
		IntSet commonVertices = IntSetUtil.intersection(verticesIncidentHead, verticesIncidentTail);
		//do not consider class vertices
		Set<Integer> classVertices = mReversedMapClassVertices.keySet();
		Set<Integer> commonVerticesSet = new HashSet<Integer>();
		for (int vertexId:commonVertices)
			commonVerticesSet.add(vertexId);
		commonVerticesSet = Sets.difference(commonVerticesSet, classVertices);
		if (commonVerticesSet.size() > 0)
			return true;
		return false;
	}
	
	
	/**
	 * This method adds a unique edge between given head and tail ids. Additionally, it will check that edge should be added only such that no triangles are formed between input head and tail ids if the last parameter of the method is true.
	 * @param headColo - input color for the head vertex.
	 * @param tailColo - input color for the tail vertex.
	 * @param headID - id for the head vertex.
	 * @param tailID - id for the tail vertex.
	 * @param mMapColourToEdgeIDsToUpdate - map to update the edge color and ids if edge is added.
	 * @param mColourMapperToUse - Color mapper to use for edge colors.
	 * @param triangleCheck - boolean variable to indicate if it should check that triangle is not formed. When set to true the edge is added between input vertices only if it does not form a triangle.
	 * @return - will return true if edge is added to the mimic graph else false.
	 */
	private boolean addEdgeWithTriangleCheck(BitSet headColo, BitSet tailColo, int headID, int tailID, Map<BitSet, IntSet> mMapColourToEdgeIDsToUpdate, IColourMappingRules mColourMapperToUse, boolean triangleCheck) {
		// Get edge between head and tail, assuming vertex 1 is tail and vertex 2 is head
		Set<BitSet> possEdgeColov1tailv2head = mColourMapperToUse.getPossibleLinkingEdgeColours(tailColo, headColo);
		
		// Check for duplicate edge color if it is essential
		// Note: This check is not required when a triangle is created for the first time or a edge is created between vertices for the first time
		possEdgeColov1tailv2head = removeDuplicateEdgeColors(tailID, headID, possEdgeColov1tailv2head);	
		
		if (possEdgeColov1tailv2head.size() != 0) { // Add edge if edge color is found for the vertices
			
			// check the head id and tail id does not have a vertex in common. Adding an edge could form a triangle
			if (triangleCheck) {
				if (commonVertices(headID, tailID))
					return false;//do not add an edge and return false. if input vertices have a vertex in common.
			}
			
			// randomly select edge colo
			BitSet randomEdgeColov1v2 = possEdgeColov1tailv2head.toArray(new BitSet[possEdgeColov1tailv2head.size()])[mRandom.nextInt(possEdgeColov1tailv2head.size())];
			
			// add the edge to mimic graph
			int edgeIdTemp;
			edgeIdTemp = mMimicGraph.addEdge(tailID, headID, randomEdgeColov1v2);
			
			// update the map of edge colors and tail, head IDs
			updateMappingOfEdgeColoHeadTailColo(randomEdgeColov1v2, headID, tailID);
			
			// Update or Add to the mapping of edge color and edge Id
			// Note: This generator does uses Real Edge IDs instead of fake IDs, as compared to previously developed generators. 
			IntSet setOfEdgeIds = mMapColourToEdgeIDsToUpdate.get(randomEdgeColov1v2);//mMapColourToEdgeIDs.get(randomEdgeColov1v2);
			if (setOfEdgeIds == null) {
				setOfEdgeIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
				mMapColourToEdgeIDsToUpdate.put(randomEdgeColov1v2, setOfEdgeIds); //mMapColourToEdgeIDs.put(randomEdgeColov1v2, setOfEdgeIds);
			}
			setOfEdgeIds.add(edgeIdTemp);
			
			return true;
		
		}else {
			return false;
		}
	}
	
	/**
	 * This method updates the global map for Edge color => tail ID => head IDs
	 * @param possEdgeColo
	 * @param headID
	 * @param tailID
	 */
	public void updateMappingOfEdgeColoHeadTailColo(BitSet possEdgeColo, int headID, int tailID) {
		// Get the map storing edge colors and corresponding tail and head ids
		Map<Integer, IntSet> mTailHead = mMapEdgeColoursToConnectedVertices.get(possEdgeColo);
		if (mTailHead == null) {
			mTailHead = new HashMap<Integer, IntSet>();
		}
								
		// initialize head ids for the map
		IntSet headIds =  mTailHead.get(tailID);
		if (headIds == null) {
			headIds = new DefaultIntSet(Constants.DEFAULT_SIZE);
		}
		headIds.add(headID);
									
		//update the map for tail id and head ids
		mTailHead.put(tailID, headIds);
					
		mMapEdgeColoursToConnectedVertices.put(possEdgeColo, mTailHead);
	}
	
	
	/**
	 * For the input set of edge colors between the given vertices, the function removes edge colors that already exist among input vertices. It returns the updated set.
	 * @param inputVertex1ID - input vertex id 1
	 * @param inputVertex2ID - input vertex id 2
	 * @param possEdgeColov1tailv2head - possible set of edge colors between input vertices
	 * @return - updated input set with not already existing edge colors
	 */
	private Set<BitSet> removeDuplicateEdgeColors(int inputVertex1ID, int inputVertex2ID, Set<BitSet> possEdgeColov1tailv2head) {
		// check existing edge colors between vertices and remove them from the possible edge colors set
		IntSet edgesIncidentVert1 = mMimicGraph.getEdgesIncidentTo(inputVertex1ID);
		IntSet edgesIncidentVert2 = mMimicGraph.getEdgesIncidentTo(inputVertex2ID);
		IntSet edgesIncidentExistingVertices = IntSetUtil.intersection(edgesIncidentVert1, edgesIncidentVert2);
					
		Set<BitSet> existingEdgeColours = new HashSet<BitSet>();
		for(int existingEdgeId : edgesIncidentExistingVertices) {
			existingEdgeColours.add(mMimicGraph.getEdgeColour(existingEdgeId));
		}
					
		// Difference between possible colors and existing colors
		SetView<BitSet> differenceSet = Sets.difference(possEdgeColov1tailv2head, existingEdgeColours);
		possEdgeColov1tailv2head = new HashSet<BitSet>();
		possEdgeColov1tailv2head.addAll(differenceSet);
		
		return possEdgeColov1tailv2head;
	}
	
	/**
	 * The method selects a random triangle by using Random object. It will return null, if no triangles are found in the input graphs.
	 * 
	 * @return - Triangle Colours
	 * 			 Set of colors for each vertex of a randomly selected triangle along with their occurrence count in input graphs.
	 */
	private TriColos getRandomTriangle() {
		TriColos randomTriangleColor = null;
		
		if (setAllTriangleColours.size() != 0) {
			randomTriangleColor = setAllTriangleColours.toArray(new TriColos[setAllTriangleColours.size()])[mRandom.nextInt(setAllTriangleColours.size())];
		}
		
		
		return randomTriangleColor;
	}
	
	private void removeTypeEdgesFromGraphs(ColouredGraph[] origGrphs) {
		
		for (ColouredGraph graph : origGrphs) {
			if (graph!= null) {
				
				// get rdf type edge
				BitSet rdfTypePropertyColour = graph.getRDFTypePropertyColour();
				
				// get all edges
				IntSet allEdges = graph.getEdges();
		
				//Iterate over all edges
				for(int edgeId:allEdges) {
					// remove if rdf type edge
					if (graph.getEdgeColour(edgeId).equals(rdfTypePropertyColour)) {
						graph.removeEdge(edgeId);
					}
				}
			}
		}
	}	
}
