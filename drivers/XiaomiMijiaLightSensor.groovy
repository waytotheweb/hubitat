/**
 *  Xiaomi Mijia Smart Light Sensor
 *
 */

import hubitat.zigbee.zcl.DataType

metadata {
	definition (name: "Xiaomi Mijia Smart Light Sensor", namespace: "waytotheweb", author: "Jonathan Michaelson", importUrl: "https://raw.githubusercontent.com/waytotheweb/hubitat/main/drivers/XiaomiMijiaLightSensor.groovy") {
		capability "IlluminanceMeasurement"
		capability "Battery"
		capability "Sensor"
		capability "Configuration"
		capability "Refresh"

		attribute "voltage", "number"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0400", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01", deviceJoinName: "Xiaomi Mijia Smart Home Light Sensor"

	}
	preferences {
		input name: "infoLogging", type: "bool", title: "Enable info message logging", description: ""
		input name: "debugLogging", type: "bool", title: "Enable debug message logging", description: ""
	}    
}

def parse(String description) {
	if (debugLogging) log.debug "Incoming data from device : $description"
	if (description?.startsWith("catchall:")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		if (debugLogging) log.debug "Desc Map : $descMap"
	}
	if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		if (descMap.cluster == "0001" && descMap.attrId == "0020") {
			def rawValue = Integer.parseInt(descMap.value,16)
			def batteryVolts = (rawValue / 10).setScale(2, BigDecimal.ROUND_HALF_UP)
			def minVolts = 20
			def maxVolts = 30
			def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
			def batteryValue = Math.min(100, pct)
			sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
			sendEvent("name": "voltage", "value": batteryVolts, "unit": "volts", "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName battery changed to $batteryValue%"
			if (infoLogging) log.info "$device.displayName voltage changed to $batteryVolts volts"
		}
		if (descMap.cluster == "0400" && descMap.attrId == "0000") {
			def rawLux = Integer.parseInt(descMap.value,16)
			def lux = rawLux > 0 ? Math.round(Math.pow(10,(rawLux/10000)) - 1) : 0
			sendEvent("name": "illuminance", "value": lux, "unit": "lux", "displayed": true, isStateChange: true)
			if (infoLogging) log.info "$device.displayName illuminance changed to $lux"
		}
	}
}

def refresh() {
	Integer zDelay = 1000
	List<String> cmd = []

	if (debugLogging) log.debug "refresh()"

	cmd += zigbee.readAttribute(0x0001, 0x0020)	// battery
	cmd += zigbee.readAttribute(0x0400, 0x0000)	// illuminance

	return cmd
}

def configure() {
	Integer zDelay = 100
	List<String> cmd = []

	if (debugLogging) log.debug "configure()"

	cmd = [
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0000 {${device.zigbeeId}} {}", "delay zDelay",	// basic
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}", "delay zDelay",	// battery
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0003 {${device.zigbeeId}} {}", "delay zDelay",	// identify
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0400 {${device.zigbeeId}} {}", "delay zDelay",	// luminance
		"send 0x${device.deviceNetworkId} 1 1"
	]

	cmd += zigbee.configureReporting(0x0400, 0x0000, 0x21, 10,   3600, 300)
	cmd += zigbee.configureReporting(0x0001, 0x0020, 0x20, 3600, 3600, 1)

	cmd += refresh()

	return cmd
}
