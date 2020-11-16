/**
 *  Zigbee Xiaomi Mijia Smart Light Sensor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Current Contributer   : Jonathan Michaelson
 *	Original Contributors : John Constantelos (https://github.com/jsconstantelos/SmartThings/blob/master/devicetypes/jsconstantelos/my-xiaomi-mijia-smart-light-sensor.src/my-xiaomi-mijia-smart-light-sensor.groovy)
 *
 *  Updates:
 *  -------
 *  11-15-2020 : Initial commit, converted from ST
 */

import hubitat.zigbee.zcl.DataType

metadata {
	definition (name: "Zigbee Xiaomi Mijia Smart Light Sensor", namespace: "waytotheweb", author: "Jonathan Michaelson", importUrl: "https://raw.githubusercontent.com/waytotheweb/hubitat/main/drivers/XiaomiMijiaLightSensor.groovy") {
		capability "IlluminanceMeasurement"
		capability "Battery"
		capability "Sensor"
		capability "Configuration"
		capability "Refresh"

		fingerprint profileId: "0104", inClusters: "0000,0400,0003,0001", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01", deviceJoinName: "Xiaomi Mijia Smart Home Light Sensor"

	}
	preferences {
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
			def minVolts = 20
			def maxVolts = 30
			def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
			def batteryValue = Math.min(100, pct)
			sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
		}
		if (descMap.cluster == "0400" && descMap.attrId == "0000") {
			def rawLux = Integer.parseInt(descMap.value,16)
			def lux = Math.round(Math.pow(10,(rawLux/10000))+ 1)
			sendEvent("name": "illuminance", "value": lux, "unit": "lux", "displayed": true, isStateChange: true)
		}
	}
}

def refresh() {
	Integer zDelay = 100

	if (debugLogging) log.debug "refresh()"

	def cmd = [
		"he rattr 0x${device.deviceNetworkId} 1 0x0001 0","delay zDelay",
		"he rattr 0x${device.deviceNetworkId} 1 0x0400 0","delay zDelay"
	]

	return cmd
}

def configure() {
	Integer zDelay = 100

	if (debugLogging) log.debug "configure()"

	def cmd = [
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x0000 {${device.zigbeeId}} {}", "delay zDelay",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x0001 {${device.zigbeeId}} {}", "delay zDelay",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x0003 {${device.zigbeeId}} {}", "delay zDelay",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x0400 {${device.zigbeeId}} {}", "delay zDelay",
	]

	cmd += zigbee.configureReporting(0x0400, 0x0000, 0x21, 5, 60, 300)
	cmd += zigbee.configureReporting(0x0001, 0x0020, 0x20, 60, 60, 1)

	cmd += refresh()

	return cmd
}
