/**
 *  Xiaomi Aqara Mijia Sensors
 *
 */

import hubitat.zigbee.zcl.DataType

metadata {
	definition (name: "Xiaomi Aqara Mijia Sensors", namespace: "waytotheweb", author: "Jonathan Michaelson", importUrl: "https://raw.githubusercontent.com/waytotheweb/hubitat/main/drivers/Xiaomi_Aqara_Mijia_Sensors.groovy") {
		capability "Battery"
		capability "Sensor"
		capability "Refresh"
		capability "Configuration"

		capability "IlluminanceMeasurement"
		capability "RelativeHumidityMeasurement"
		capability "TemperatureMeasurement"
		capability "PressureMeasurement"
		capability "AccelerationSensor"
		capability "MotionSensor"
		capability "WaterSensor"
		capability "ContactSensor"

		capability "PushableButton"
		capability "HoldableButton"
		capability "DoubleTapableButton"

		attribute "voltage", "number"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0400", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01", deviceJoinName: "Xiaomi Mijia Light Sensor"
		fingerprint profileId: "0104", inClusters: "0000, 0003, FFFF, 0402, 0403, 0405", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.weather", deviceJoinName: "Xiaomi Aqara Temperature Sensor"
		fingerprint profileId: "0104", inClusters: "0000,FFFF,0406,0400,0500,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion.aq2", deviceJoinName: "Xiaomi Aqara Motion Sensor"
		fingerprint profileId: "0104", inClusters: "0000,FFFF,0101,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.vibration.aq1", deviceJoinName: "Xiaomi Aqara Vibration Sensor"
		fingerprint profileId: "0104", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2", deviceJoinName: "Xiaomi Aqara Contact Sensor"
		fingerprint profileId: "0104", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.remote.b186acn01", deviceJoinName: "Xiaomi Aqara Wireless Single Remote Switch"
		fingerprint profileId: "0104", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_86sw1", deviceJoinName: "Xiaomi Aqara Wireless Single Remote Switch"

	}
	preferences {
		input name: "infoLogging", type: "bool", title: "Enable info message logging", description: ""
		input name: "debugLogging", type: "bool", title: "Enable debug message logging", description: ""
	}    
}

def parse(String description) {
	if (debugLogging) log.debug "Incoming data from device : $description"
	if (description.indexOf('attrId: FF01, encoding: 42') >= 0) return

	if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		if (debugLogging) log.debug "cluster:$descMap.cluster, attrId:$descMap.attrId"

		if (descMap.cluster == "0001" && descMap.attrId == "0020") {
			def rawValue = Integer.parseInt(descMap.value,16)
			def batteryVolts = (rawValue / 10).setScale(2, BigDecimal.ROUND_HALF_UP)
			def minVolts = 20
			def maxVolts = 30
			def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
			def batteryValue = Math.min(100, pct)
			if (batteryValue > 0){
				sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
				sendEvent("name": "voltage", "value": batteryVolts, "unit": "volts", "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName battery changed to $batteryValue%"
				if (infoLogging) log.info "$device.displayName voltage changed to $batteryVolts volts"
			}
		}
		if (descMap.cluster == "0400" && descMap.attrId == "0000") {
			def rawEncoding = Integer.parseInt(descMap.encoding, 16)
			def rawLux = Integer.parseInt(descMap.value,16)
			if (getDeviceDataByName('model') == "lumi.sensor_motion.aq2") {
				def rawHex = reverseHexString(descMap.value)
				rawLux = Integer.parseInt(rawHex,16)
			}
			def lux = rawLux > 0 ? Math.round(Math.pow(10,(rawLux/10000)) - 1) : 0
			sendEvent("name": "illuminance", "value": lux, "unit": "lux", "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName illuminance changed to $lux"
		}
		if (descMap.cluster == "0403" && descMap.attrId == "0000") {
			def rawValue = Integer.parseInt(descMap.value,16)
			sendEvent("name": "pressure", "value": rawValue, "unit": "kPa", "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName pressure changed to $rawValue"
		}
		if (descMap.cluster == "0402" && descMap.attrId == "0000") {
			def rawValue = Integer.parseInt(descMap.value,16)/100
			def Scale = location.temperatureScale
			if (Scale == "F") rawValue = (rawValue * 1.8) + 32
			sendEvent("name": "temperature", "value": rawValue, "unit": "&deg;"+Scale, "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName temperature changed to $rawValue&deg;"+Scale
		}
		if (descMap.cluster == "0405" && descMap.attrId == "0000") {
			def rawValue = Integer.parseInt(descMap.value,16)/100
			sendEvent("name": "humidity", "value": rawValue, "unit": "%", "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName humidity changed to $rawValue"
		}
		if (descMap.cluster == "0406" && descMap.attrId == "0000") {
			def rawValue = Integer.parseInt(descMap.value,16)
			def status = "inactive"
			if (rawValue == 1) status = "active"
			sendEvent("name": "motion", "value": status,, "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName motion changed to $status"
			unschedule()
			runIn(65, resetMotion)
		}
		if (descMap.cluster == "0101" && (descMap.attrId == "0055" || descMap.attrId == "0508")) {
			def status = "active"
			sendEvent("name": "acceleration", "value": status,, "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName acceleration changed to $status"
			unschedule()
			runIn(65, resetVibration)
		}
		if (descMap.cluster == "0006" && descMap.attrId == "0000") {
			def rawValue = Integer.parseInt(descMap.value,16)
			def contact = "closed"
			if (rawValue == 1) contact = "open"
			sendEvent("name": "contact", "value": contact, "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName contact changed to $contact"
		}
		if (descMap.cluster == "0012" && descMap.attrId == "0055") {
			def button = Integer.parseInt(descMap.endpoint,16) 
			def action = Integer.parseInt(descMap.value,16)
			if (debugLogging) log.debug "Button:$button, Action:$action"

			if (action == 0) {
				sendEvent("name": "held", "value":  button, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "Button $button was held"
			}
			if (action == 1) {
				sendEvent("name": "pushed", "value":  button, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "Button $button was pushed $action time(s)"
			}
			if (action == 2) {
				sendEvent("name": "doubleTapped", "value":  button, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "Button $button was double tapped"
			}
			if (action == 255) {
				sendEvent("name": "released", "value":  button, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "Button $button was released"
			}
		}
	}
}

def resetMotion() {
	if (device.currentState('motion')?.value == "active"){
		sendEvent("name": "motion", "value": "inactive", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName motion changed to inactive"
	}
}

def resetVibration() {
	if (device.currentState('acceleration')?.value == "active"){
		sendEvent("name": "acceleration", "value": "inactive", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName acceleration changed to inactive"
	}
}

def reverseHexString(hexString) {
	def reversed = ""
	for (int i = hexString.length(); i > 0; i -= 2) {
		reversed += hexString.substring(i - 2, i )
	}
	return reversed
}

def refresh() {
	List<String> cmd = []

	if (debugLogging) log.debug "refresh()"
	if (device.currentState('motion')?.value == "active"){
		unschedule()
		resetMotion()
	}
	if (device.currentState('acceleration')?.value == "active"){
		unschedule()
		resetVibration()
	}

	cmd += zigbee.onOffRefresh()
	cmd += zigbee.onOffConfig()
	cmd += zigbee.batteryConfig()

	cmd += zigbee.readAttribute(0x0001, 0x0020)	// battery
	cmd += zigbee.readAttribute(0x0000, 0x0004)
	cmd += zigbee.readAttribute(0x0000, 0x0005)

	return cmd
}

def configure() {
	Integer zDelay = 100
	List<String> cmd = []

	if (debugLogging) log.debug "configure()"

	cmd = [
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0000 {${device.zigbeeId}} {}",
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}",
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0003 {${device.zigbeeId}} {}",
	]

	return cmd
}
