package com.orbotix.sample.spheroguide;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

public class Parser {
	public static final int NUMBER_OF_NODES = 100;
	static String nodeIDArray[];
	static String pathArray[];
	static Location2D locArray[];
	static float locXArray[];
	static float locYArray[];
	static String destinationNodeID="";
	static String currentNodeID="";
	static int nodeCount = 0;

	public static boolean getNextNodeLocation(Location2D position) {
		int destNodeIdx = 0;
		int pathIdx = 0;
		int pathLength = 0;
		String nextNodeID;
		int nextNodeIdx;
		
		
		//get destination node index based on ID
		while (destNodeIdx < nodeCount && !destinationNodeID.equals(nodeIDArray[destNodeIdx])) {
			destNodeIdx++;
		}
		System.out.printf("DestNodeID: %s\n", destinationNodeID);
		System.out.printf("DestIdx: %d\n", destNodeIdx);
		//get index in path string of next node on path
		if (destNodeIdx != nodeCount) {
			pathLength = pathArray[destNodeIdx].length();
			System.out.printf("PathLength: %d\n", pathLength);
			while (pathIdx < pathLength && !currentNodeID.equals(pathArray[destNodeIdx].substring(pathIdx, pathIdx+1))) {
				pathIdx++;
			}
			//get next node index on path
			pathIdx++;
			System.out.printf("PathIdx: %d\n", pathIdx);
			if (pathIdx == pathLength) { 
				return false;
			}
			//get next node ID from index to path string
			nextNodeID = pathArray[destNodeIdx].substring(pathIdx, pathIdx+1);
			System.out.printf("nextNodeID: %s\n", nextNodeID);
			nextNodeIdx = 0;
			//get next node index based on ID
			while (nextNodeIdx < nodeCount && !nextNodeID.equals(nodeIDArray[nextNodeIdx])) {
				nextNodeIdx++;
			}
			System.out.printf("nextNodeIdx: %d\n", nextNodeIdx);
			position.setX(locXArray[nextNodeIdx]);
			position.setY(locYArray[nextNodeIdx]);
			currentNodeID = nextNodeID;
			return true;
		}
		else {
			return false;
		}
	}
	
	public static void setLoc2d(Location2D pos) {
		pos.setX(4.0f);
		pos.setY(3.0f);
	}
	
	public static boolean setDestination (String nodeIDString) {
		//int nodeID = Integer.parseInt(nodeIDString);
		int nodeNum = 0;
		Location2D temp = new Location2D();
		while (nodeNum < nodeCount && !nodeIDString.equals(nodeIDArray[nodeNum])) {
			nodeNum++;
		}
		System.out.printf("nodeNum: %d\n", nodeNum);
		if (nodeNum != nodeCount) {
			destinationNodeID = nodeIDString;
			currentNodeID = pathArray[nodeNum].substring(0,1);
			System.out.printf("NodeAdded! destinatoinNodeID %s, currentNodeID %s\n",  destinationNodeID, currentNodeID);
			return true;
		}
		else {
			return false;
		}
		
	}
	
	public static boolean parseFile() {
		try {
			String filename = "sphero_location_marks_mesmael.csv";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
		StringTokenizer token = null;
			nodeIDArray = new String[NUMBER_OF_NODES];
			pathArray = new String[NUMBER_OF_NODES];
			locArray = new Location2D[NUMBER_OF_NODES];
			locXArray = new float[NUMBER_OF_NODES];
			locYArray = new float[NUMBER_OF_NODES];
			int lineNum = 0, tokenNum = 0, nodeNum = 0;
			String tempString="";
			float tempFloat=0;

			line = br.readLine();
			lineNum++;
			System.out.println(line);

			while ((line = br.readLine()) != null) {
				lineNum++;

				// break comma separated file line by line
				token = new StringTokenizer(line, ",");

				//Parse NodeID

				if (token.hasMoreTokens()) {
					//nodeIDArray[nodeNum] = Integer.parseInt(token.nextToken());
					tempString = token.nextToken();
					tempString = tempString.replace("\"", "");
					nodeIDArray[nodeNum] = tempString;
				}

				if (token.hasMoreTokens()) {
					pathArray[nodeNum] = token.nextToken();
					pathArray[nodeNum] = pathArray[nodeNum].replace("\"", "");
				}

				if (token.hasMoreTokens()) {
					tempString = token.nextToken();
					//System.out.println(tempString);
					tempString = tempString.replace("\"", "");
					//System.out.println(tempString);
					//tempFloat = Float.parseFloat(tempString);
					locXArray[nodeNum] = Float.parseFloat(tempString);
					//locArray[nodeNum].setX(Float.parseFloat(tempString));
					//locArray[nodeNum].setX((float)tempDouble);
					//System.out.printf("%f\n", locXArray[nodeNum]);
					//locArray[nodeNum].setX(tempFloat);
					//System.out.printf("%f\n", tempFloat);
					//System.out.printf("Line %d\n", lineNum);
					//System.out.println(token);
					if (token.hasMoreTokens()) {
						tempString = token.nextToken();
						tempString = tempString.replace("\"", "");
						locYArray[nodeNum]=Float.parseFloat(tempString);
						//System.out.printf("%f\n", locYArray[nodeNum]);
						//tempFloat = Float.parseFloat(tempString);
						//locArray[nodeNum].setY(tempFloat);
					}
				}

				//System.out.printf("Line %d\n", lineNum);
				//System.out.printf("Node: %d, Path: %s, X: %f, Y:%f\n", nodeIDArray[nodeNum], pathArray[nodeNum], locArray[nodeNum].getX(), locArray[nodeNum].getY());
				//System.out.printf("Node: %d, Path: %s, X: %f, Y:%f\n", nodeIDArray[nodeNum], pathArray[nodeNum], locXArray[nodeNum], locYArray[nodeNum]);
				System.out.printf("Node: %s, Path: %s, X: %f, Y:%f\n", nodeIDArray[nodeNum], pathArray[nodeNum], locXArray[nodeNum], locYArray[nodeNum]);
				nodeNum++;
				tokenNum = 0;
			}
			nodeCount = nodeNum-1;

			return true;
		} catch (Exception e) {
			System.err.println("Parse Error: " + e.getMessage());
			return false;
		}
	}
/*
	public static void main(String[] args) {
		//String filename = "C:\\Users\\DAS\\workspace\\SpheroMapParser\\SpheroMap.csv";
		String filename = "C:\\Users\\DAS\\workspace\\SpheroMapParser\\Sphero_Location_Marks_MEsmael.csv";
		Location2D loc = new Location2D();
		
		System.out.println("Initializing parse for file \"" + filename
				+ "\"...");

		if (Parser.parseFile(filename)) {
			System.out.println("Parse successful");
		} else {
			System.out.println("Failed to parse");
		}
		System.out.println("Parse done.");
		
		Parser.setDestination("J");
		if (Parser.getNextNodeLocation(loc))
			System.out.printf("%f, %f\n", loc.getX(), loc.getY());
		if (Parser.getNextNodeLocation(loc))
			System.out.printf("%f, %f\n", loc.getX(), loc.getY());
		if (Parser.getNextNodeLocation(loc))
			System.out.printf("%f, %f\n", loc.getX(), loc.getY());
		if (Parser.getNextNodeLocation(loc))
			System.out.printf("%f, %f\n", loc.getX(), loc.getY());
		//Parser.getNextNodeLocation(loc);
		//System.out.printf("%f, %f\n", loc.getX(), loc.getY());
		

	}
*/
}
