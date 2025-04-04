/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.UpdateListener;


/**
 * Message location report. Reports the location (coordinates) of messages.
 * The messages that are reported and the reporting interval can be configured.
 */
public class MessageLocationReport extends Report implements UpdateListener {
	/** Reporting granularity -setting id ({@value}). 
	 * Defines the interval how often (seconds) a new snapshot of message 
	 * locations is created */
	public static final String GRANULARITY = "granularity";
	/** Reported messages -setting id ({@value}). 
	 * Defines the IDs of the messages that are reported 
	 * (comma separated list)*/
	public static final String REPORTED_MESSAGES = "messages";
	/** value of the granularity setting */
	protected final int granularity;
	/** time of last update*/
	protected double lastUpdate; 
	/** Identifiers of the message which are reported */
	protected HashSet<String> reportedMessages;
	
	/**
	 * Constructor. Reads the settings and initializes the report module.
	 */
	public MessageLocationReport() {
		Settings settings = getSettings();
		this.lastUpdate = 0;	
		this.granularity = settings.getInt(GRANULARITY);
		
		this.reportedMessages = new HashSet<String>();
        Collections.addAll(this.reportedMessages, settings.getCsvSetting(REPORTED_MESSAGES));
		
		init();
	}

	/**
	 * Creates a new snapshot of the message locations if "granularity" 
	 * seconds have passed since the last snapshot. 
	 * @param hosts All the hosts in the world
	 */
	public void updated(List<DTNHost> hosts) {
		double simTime = getSimTime();
		/* creates a snapshot once every granularity seconds */
		if (simTime - lastUpdate >= granularity) {
			createSnapshot(hosts);
			this.lastUpdate = simTime - simTime % granularity;
		}
	}
	
	/**
	 * Creates a snapshot of message locations 
	 * @param hosts The list of hosts in the world
	 */
	private void createSnapshot(List<DTNHost> hosts) {
		boolean isFirstMessage;
		String reportLine;
		
		write ("[" + (int)getSimTime() + "]"); /* write sim time stamp */
		
		for (DTNHost host : hosts) {
			isFirstMessage = true;
			reportLine = "";
			for (Message m : host.getMessageCollection()) {
				if (this.reportedMessages.contains(m.getId())) {
					if (isFirstMessage) {
						reportLine = host.getLocation().toString();
						isFirstMessage = false;
					}		
					reportLine += " " + m.getId();
				}
			}
			if (reportLine.length() > 0) {
				write(reportLine); /* write coordinate and message IDs */
			}
		}
	}
	 
}
