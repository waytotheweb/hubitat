/* 
Parent driver fo deCONZ_rest_api 
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
*/

metadata {
    definition (name: "deCONZ_rest_api_Child_Light_Sensor", namespace: "jorge.martinez", author: "Jonathan Michaelson", importUrl: "https:") {
	capability "IlluminanceMeasurement"
        capability "Battery"
	capability "Sensor"
	attribute "lastUpdated", "String"
        attribute "ID", "String"
        command "SETdeCONZname" , ["string"]
        command "GETdeCONZname"
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}
def SETdeCONZname(name){
    if (name==null) name = device.getLabel()
    parent.PutRequest("sensors/${getDataValue("ID")}","{\"name\": \"${name}\"}")
}
def GETdeCONZname(){
    parent.updateCildLabel(getDataValue("ID"),true)
}
def updateBattery (bat){
    if (logEnable) log.debug "new batt:${bat}"
    sendEvent(name: "battery", value: bat)
}
def updateLightLevel (data){
    if (logEnable) log.debug "illuminance change :${data}"
    if (data)  sendEvent(name: "illuminance", value: data)
}
def updateLastUpdated (date){
    if (logEnable) log.debug "Last Update:${date}"
    sendEvent(name: "lastUpdated", value: date)
}
