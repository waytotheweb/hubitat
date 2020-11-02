/**
 * Simple Virtual 3 setting switch Driver that works on
 * Amazon Alexa to force on/off
 *
 *
 * Version:  1.0
 *
 */
metadata {
    definition(name: "3 setting Switch", namespace: "waytotheweb", author: "Jonathan Michaelson") {
        capability "Switch"

	command "other"
    }
}

def on() {
    log.info "on()"
    sendEvent(name: "switch", value: "on")
}

def off() {
    log.info "off()"
    sendEvent(name: "switch", value: "off")
}

def other() {
    log.info "other()"
    sendEvent(name: "switch", value: "other")
}
