/*
* Virtual Doorbell with Temperature
*
*/

import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

metadata {
	definition (name: "Doorbell Driver", namespace: "waytotheweb", author: "Jonathan Michaelson", importUrl: "") {
		capability "ContactSensor"
		capability "Configuration"
		capability "Temperature Measurement"
		capability "PresenceSensor"

		command "open"
		command "close"
		command "setTemperature", ["integer"]
	}

	preferences {
		input name: "presenceDetect", type: "bool", title: "Enable Presence Detection", description: "This will keep track of the devices presence and will change state if no data received within the Presence Timeout. If it does lose presence try pushing the reset button on the device if available.", defaultValue: true
		input name: "presenceHours", type: "enum", title: "Presence Timeout", description: "The number of hours before a device is considered 'not present'.<br>Note: Some of these devices only update their battery every 6 hours.", defaultValue: "12", options: ["1","2","3","4","6","12","24"]
		input name: "infoLogging", type: "bool", title: "Enable info message logging", description: "", defaultValue: true
		input name: "debugLogging", type: "bool", title: "Enable debug message logging", description: "", defaultValue: false
	}
}

def present() {
	sendEvent("name": "presence", "value":  "present", isStateChange: true)
	if (infoLogging) log.info "$device.displayName contact changed to present [virtual]"
}

def notPresent() {
	sendEvent("name": "presence", "value":  "not present", isStateChange: true)
	if (infoLogging) log.info "$device.displayName contact changed to not present [virtual]"
}

def presenceTracker() {
	sendEvent("name": "presence", "value":  "not present")
	if (infoLogging) log.info "$device.displayName not present"
	presenceStart()
}

def presenceStart() {
	if (presenceHours == null || presenceHours == "") presenceHours = "12"
	def scheduleHours = presenceHours.toInteger() * 60 * 60
	if (scheduleHours < 1 || scheduleHours > 86400) scheduleHours = 43200
	if (infoLogging) log.info "$device.displayName presense check in ${presenceHours} hours"
	runIn(scheduleHours, "presenceTracker")
}

def updated() {
	unschedule(presenceTracker)
	if (presenceDetect != false) presenceStart()
}

def setTemperature(temperature) {
	if (debugLogging)  log.debug "Temperature: ${temperature}"
	processTemperature(temperature)
}

def open() {
	sendEvent(name: "contact", value: "open")
	if (presenceDetect != false) {
		unschedule(presenceTracker)
		if (device.currentValue("presence") != "present"){
			sendEvent("name": "presence", "value":  "present")
			if (debugLogging) log.debug "$device.displayName present"
		}
		presenceStart()
	}
}

def close() {
	sendEvent(name: "contact", value: "closed")
	if (presenceDetect != false) {
		unschedule(presenceTracker)
		if (device.currentValue("presence") != "present"){
			sendEvent("name": "presence", "value":  "present")
			if (debugLogging) log.debug "$device.displayName present"
		}
		presenceStart()
	}
}

def processTemperature(rawValue) {
	rawValue = Integer.parseInt(rawValue) / 100
	def Scale = location.temperatureScale
	if (Scale == "F") rawValue = (rawValue * 1.8) + 32
	if (temperatureOffset == null) temperatureOffset = "0"
	def offsetrawValue = (rawValue  + Float.valueOf(temperatureOffset))
	rawValue = offsetrawValue
	if (rawValue > 200 || rawValue < -200){
		if (infoLogging) log.info "$device.displayName Ignored temperature value: $rawValue\u00B0"+Scale
	} else {
		sendEvent("name": "temperature", "value": rawValue, "unit": "\u00B0"+Scale)
		if (infoLogging) log.info "$device.displayName temperature changed to $rawValue\u00B0"+Scale
	}
	if (presenceDetect != false) {
		unschedule(presenceTracker)
		if (device.currentValue("presence") != "present"){
			sendEvent("name": "presence", "value":  "present")
			if (debugLogging) log.debug "$device.displayName present"
		}
		presenceStart()
	}
}

def configure() {
	if (debugLogging) log.debug "configure()"

	unschedule()
	state.clear()

	return
}
