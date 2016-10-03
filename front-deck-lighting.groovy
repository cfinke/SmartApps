/**
 *  Front Deck Lighting
 *
 *  Copyright 2016 Christopher Finke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Front Deck Lighting",
	namespace: "cfinke",
	author: "Christopher Finke",
	description: "Turn on the front deck lights when there is motion or an opened door in the evening. Turn it X minutes off after all motion stops and the door shuts.  Do nothing if the switch was turned on manually (until it's turned off manually).",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Turn on when motion detected:") {
		input "themotion", "capability.motionSensor", required: true, title: "Where?"
	}
	section("Turn on when door opens:") {
		input "thedoor", "capability.contactSensor", required: true, title: "Door?"
	}
	section("Turn off when there's been no activity for") {
		input "minutes", "number", required: true, title: "Minutes?"
	}
	section("Turn on this light") {
		input "theswitch", "capability.switch", required: true
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(themotion, "motion.active", motionDetectedHandler)
	subscribe(themotion, "motion.inactive", motionStoppedHandler)
	subscribe(thedoor, "contact.closed", doorClosedHandler)
	subscribe(thedoor, "contact.open", doorOpenHandler)
	subscribe(theswitch, "switch.on", switchOnHandler)
	subscribe(theswitch, "switch.off", switchOffHandler)
}

def motionDetectedHandler(evt) {
	log.debug "motionDetectedHandler called: $evt"
	log.debug "turning theswitch on"
	state.lastActivity = now()
	state.nextSwitchWasAuto = true
	theswitch.on()
}

def motionStoppedHandler(evt) {
	log.debug "motionStoppedHandler called: $evt"
	state.lastActivity = now()
	runIn(60 * minutes, checkMotion)
}

def doorOpenHandler(evt) {
	log.debug "doorOpenHandler called: $evt"
	log.debug "turning theswitch on"
	
	state.lastActivity = now()
	
	if ( ! state.manualMode ) {
		state.nextSwitchWasAuto = true
		theswitch.on()
	}
}

def doorClosedHandler(evt) {
	log.debug "doorClosedHandler called: $evt"
	state.lastActivity = now()
	
	runIn(60 * minutes, checkMotion)
}

def switchOnHandler(evt) {
	log.debug "switchOnHander called: $evt"
	
	if ( ! state.nextSwitchWasAuto ) {
		log.debug "Entering manual mode."
		state.manualMode = true
	}
	
	state.nextSwitchWasAuto = false
}

def switchOffHandler(evt) {
	log.debug "switchOnHander called: $evt"
	state.manualMode = false
}

def checkMotion() {
	log.debug "In checkMotion scheduled method"

	if ( state.manualMode ) {
		log.debug "In manual mode. Do nothing."
		return
	}

	def motionState = themotion.currentState("motion")

	if (motionState.value == "inactive") {
		// get the time elapsed between now and when the motion reported inactive
		def elapsed = now() - state.lastActivity

		// elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
		def threshold = 1000 * 60 * minutes

		if (elapsed >= threshold) {
			log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
			theswitch.off()
		} else {
			log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
		}
	} else {
		// Motion active; just log it and do nothing
		log.debug "Motion is active, do nothing and wait for inactive"
	}
}