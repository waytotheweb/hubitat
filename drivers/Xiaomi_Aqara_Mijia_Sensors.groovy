/**
 *  Xiaomi Aqara Mijia Sensors:
 *
 *  Xiaomi Aqara Contact Sensor			: MCCGQ11LM
 *  Xiaomi Aqara Motion Sensor                  : RTCGQ11LM
 *  Xiaomi Aqara Temperature Sensor		: WSDCGQ11LM
 *  Xiaomi Aqara Vibration Sensor		: DJT11LM
 *  Xiaomi Aqara Wireless Single Remote Switch	: WXKG03LM
 *  Xiaomi Mijia Human Body Sensor		: RTCGQ01LM
 *  Xiaomi Mijia Light Sensor			: GZCGQ01LM
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
 *  Changelog:
 *
 *  v0.06 - Added battery level detection for older Xiaomi sensors
 *          Fixed lux calculation for RTCGQ11LM
 *  v0.05 - Added workaround for Xiaomi data structure oddities (ignoring them for now as they're not used)
 *          Added fingerprint for RTCGQ01LM
 *  v0.04 - Fixed temperature calculation for negative temps
 *  v0.03 - Fix for spurious voltage calculation from device data
 *  v0.02 - Added state and schedule cleanup to configure command if you move from an old driver
 *  v0.01 - Initial public release
 */

import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

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
		capability "ContactSensor"

		capability "PushableButton"
		capability "HoldableButton"
		capability "DoubleTapableButton"

		attribute "voltage", "number"
		attribute "tilt", "string"

		fingerprint profileId: "0104", inClusters: "0000,0400,0003,0001", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01", deviceJoinName: "Xiaomi Mijia Light Sensor"
		fingerprint profileId: "0104", inClusters: "0000,0003,FFFF,0402,0403,0405", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.weather", deviceJoinName: "Xiaomi Aqara Temperature Sensor"
		fingerprint profileId: "0104", inClusters: "0000,FFFF,0406,0400,0500,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion.aq2", deviceJoinName: "Xiaomi Aqara Motion Sensor"
		fingerprint profileId: "0104", inClusters: "0000,FFFF,0406,0400,0500,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion", deviceJoinName: "Xiaomi Aqara Motion Sensor"
		fingerprint profileId: "0104", inClusters: "0003,0012", outClusters: "0004,0003,0005,0012", manufacturer: "LUMI", model: "lumi.vibration.aq1", deviceJoinName: "Xiaomi Aqara Vibration Sensor"
		fingerprint profileId: "0104", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2", deviceJoinName: "Xiaomi Aqara Contact Sensor"
		fingerprint profileId: "0104", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.remote.b186acn01", deviceJoinName: "Xiaomi Aqara Wireless Single Remote Switch"
		fingerprint profileId: "0104", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.sensor_86sw1", deviceJoinName: "Xiaomi Aqara Wireless Single Remote Switch"

	}
	preferences {
		input name: "infoLogging", type: "bool", title: "Enable info message logging", description: ""
		input name: "debugLogging", type: "bool", title: "Enable debug message logging", description: ""
	}    
}

def parse(String description) {
	if (debugLogging) log.debug "Incoming data from device : $description"

	if (description?.startsWith("read attr -")) {
		def mydescMap = description.split(', ').collectEntries {
			entry -> def pair = entry.split(': ')
			[(pair.first()): pair.last()]
		}
		if (mydescMap.attrId == "FF01" || mydescMap.attrId == "FF02") {
			if (debugLogging) log.debug "Processing Xiaomi data (cluster:$mydescMap.cluster, attrId:$mydescMap.attrId)"
			if (mydescMap.cluster == "0000") {
				def MsgLength = mydescMap.value.size()
				if (MsgLength > 20){
					def batteryVoltage = ""
					if (mydescMap.attrId == "FF01" && mydescMap.value[4..5] == "21"){
						batteryVoltage = mydescMap.value[8..9] + mydescMap.value[6..7]
					}
					else if (mydescMap.attrId == "FF02" && mydescMap.value[8..9] == "21"){
						batteryVoltage = mydescMap.value[12..13] + mydescMap.value[10..11]
					}
					if (batteryVoltage != ""){
						batteryEvent(Integer.parseInt(batteryVoltage, 16) / 100)
					}
				}
			}
		} else {
			def descMap = zigbee.parseDescriptionAsMap(description)

			if (debugLogging) log.debug "Processing Xigbee data (cluster:$descMap.cluster, attrId:$descMap.attrId)"

			if (descMap.cluster == "0001" && descMap.attrId == "0020") {
				batteryEvent(Integer.parseInt(descMap.value,16))
			}
			else if (descMap.cluster == "0400" && descMap.attrId == "0000") {
				def rawEncoding = Integer.parseInt(descMap.encoding, 16)
				def rawLux = Integer.parseInt(descMap.value,16)
				def lux = rawLux > 0 ? Math.round(Math.pow(10,(rawLux/10000)) - 1) : 0
				if (getDeviceDataByName('model') == "lumi.sensor_motion.aq2") lux = rawLux
				sendEvent("name": "illuminance", "value": lux, "unit": "lux", "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName illuminance changed to $lux"
			}
			else if (descMap.cluster == "0402" && descMap.attrId == "0000") {
				def rawValue = hexStrToSignedInt(descMap.value) / 100
				def Scale = location.temperatureScale
				if (Scale == "F") rawValue = (rawValue * 1.8) + 32
				sendEvent("name": "temperature", "value": rawValue, "unit": "\u00B0"+Scale, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName temperature changed to $rawValue\u00B0"+Scale
			}
			else if (descMap.cluster == "0403" && descMap.attrId == "0000") {
				def rawValue = Integer.parseInt(descMap.value,16)
				sendEvent("name": "pressure", "value": rawValue, "unit": "kPa", "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName pressure changed to $rawValue"
			}
			else if (descMap.cluster == "0405" && descMap.attrId == "0000") {
				def rawValue = Integer.parseInt(descMap.value,16)/100
				sendEvent("name": "humidity", "value": rawValue, "unit": "%", "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName humidity changed to $rawValue"
			}
			else if (descMap.cluster == "0406" && descMap.attrId == "0000") {
				def rawValue = Integer.parseInt(descMap.value,16)
				def status = "inactive"
				if (rawValue == 1) status = "active"
				sendEvent("name": "motion", "value": status, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName motion changed to $status"
				unschedule()
				runIn(65, resetMotion)
			}
			else if (descMap.cluster == "0101" && descMap.attrId == "0508") {
				def status = "active"
				sendEvent("name": "acceleration", "value": status, "displayed": true, isStateChange: true)
				sendEvent("name": "motion", "value": "active", "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName acceleration changed to $status"
				unschedule()
				runIn(65, resetVibration)
			}
			else if (descMap.cluster == "0101" && descMap.attrId == "0055") {
				def status = "active"
				sendEvent("name": "tilt", "value": status, "displayed": true, isStateChange: true)
				sendEvent("name": "motion", "value": "active", "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName tilt changed to $status"
				unschedule()
				runIn(65, resetVibration)
			}
			else if (descMap.cluster == "0006" && descMap.attrId == "0000") {
				def rawValue = Integer.parseInt(descMap.value,16)
				def contact = "closed"
				if (rawValue == 1) contact = "open"
				sendEvent("name": "contact", "value": contact, "displayed": true, isStateChange: true)
				if (infoLogging) log.info "$device.displayName contact changed to $contact"
			}
			else if (descMap.cluster == "0012" && descMap.attrId == "0055") {
				def button = Integer.parseInt(descMap.endpoint,16) 
				def action = Integer.parseInt(descMap.value,16)
				if (debugLogging) log.debug "Button:$button, Action:$action"

				if (action == 0) {
					sendEvent("name": "held", "value":  button, "displayed": true, isStateChange: true)
					if (infoLogging) log.info "Button $button was held"
				}
				else if (action == 1) {
					sendEvent("name": "pushed", "value":  button, "displayed": true, isStateChange: true)
					if (infoLogging) log.info "Button $button was pushed $action time(s)"
				}
				else if (action == 2) {
					sendEvent("name": "doubleTapped", "value":  button, "displayed": true, isStateChange: true)
					if (infoLogging) log.info "Button $button was double tapped"
				}
				else if (action == 255) {
					sendEvent("name": "released", "value":  button, "displayed": true, isStateChange: true)
					if (infoLogging) log.info "Button $button was released"
				}
				runIn(4, resetButton)
			}
		}
	}
}

def batteryEvent(rawValue) {
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

	return
}

def resetMotion() {
	if (device.currentState('motion')?.value == "active"){
		sendEvent("name": "motion", "value": "inactive", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName motion changed to inactive"
	}

	return
}

def resetButton() {
	unschedule()
	if (debugLogging) log.debug "reset()"

	return
}

def resetVibration() {
	if (device.currentState('acceleration')?.value == "active"){
		sendEvent("name": "acceleration", "value": "inactive", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName acceleration changed to inactive"
	}
	if (device.currentState('tilt')?.value != "inactive"){
		sendEvent("name": "tilt", "value": "inactive", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName tilt changed to inactive"
	}
	if (device.currentState('motion')?.value != "inactive"){
		sendEvent("name": "motion", "value": "inactive", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName motion changed to inactive"
	}

	return
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
	cmd += zigbee.readAttribute(0x0400, 0x0000)	// illuminance

	return cmd
}

def configure() {
	Integer zDelay = 100
	List<String> cmd = []

	if (debugLogging) log.debug "configure()"

	unschedule()
	state.clear()

	cmd = [
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0000 {${device.zigbeeId}} {}",
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}",
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0003 {${device.zigbeeId}} {}",
		"zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0400 {${device.zigbeeId}} {}",
	]

	cmd += zigbee.configureReporting(0x0400, 0x0000, 0x21, 10,   3600, 300)
	cmd += zigbee.configureReporting(0x0001, 0x0020, 0x20, 3600, 3600, 1)

	return cmd
}
