/*
 * @(#)CentralityDetection.java
 * 
 * Copyright 2017 by Elisabeth Kusuma, University of Sanata Dharma.
 * 
 */

package routing.community;

/**
 *  
 * 
 * @author Elisabeth Kusuma Adi P., University of Sanata Dharma
 */

public interface CentralityDetection {
	
	double getCentrality(double[][] matrixEgoNetwork);
	CentralityDetection replicate();
}
