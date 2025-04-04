/*
 * @(#)CommunityDetectionReport.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package report;

import java.util.*;

import core.*;
import routing.*;
import routing.community.CommunityDetectionEngine;

/**
 * <p>
 * Reports the local communities at each node whenever the done() method is
 * called. Only those nodes whose router is a DecisionEngineRouter and whose
 * RoutingDecisionEngine implements the
 * routing.community.CommunityDetectionEngine are reported. In this way, the
 * report is able to output the result of any of the community detection
 * algorithms.</p>
 *
 * @author PJ Dillon, University of Pittsburgh
 */
public class CommunityDetectionReport extends Report {

    public CommunityDetectionReport() {
        init();
    }

    @Override
    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();
        List<Set<DTNHost>> communities = new LinkedList<Set<DTNHost>>();
        Map<Integer, List<Set<DTNHost>>> nodeCommunities = new HashMap<>(); //added

        for (DTNHost h : nodes) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            if (!(de instanceof CommunityDetectionEngine cd)) {
                continue;
            }

            boolean alreadyHaveCommunity = false;
            Set<DTNHost> nodeComm = cd.getLocalCommunity();

            // Test to see if another node already reported this community
            for (Set<DTNHost> c : communities) {
                if (c.containsAll(nodeComm) && nodeComm.containsAll(c)) {
                    alreadyHaveCommunity = true;
                    break;
                }
            }

            if (!alreadyHaveCommunity && nodeComm.size() > 0) {
                communities.add(nodeComm);
            }

        }

//         print each community and its size out to the file
        for (Set<DTNHost> c : communities) {
            String print = "";
            for (DTNHost dTNHost : c) {
                print = print + "\t" + dTNHost;
            }
            write(print);
        }

//        for (Map.Entry<Integer, List<Set<DTNHost>>> entry : nodeCommunities.entrySet()) {
//            Integer key = entry.getKey();
//            String print = key + "";
//            for (Set<DTNHost> community : communities) {
//                print += "\t" + community;
//            }
//            write(print);
//        }
        super.done();
    }

}
