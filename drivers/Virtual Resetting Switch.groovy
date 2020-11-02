/**
 * Simple Virtual Resetting Switch Driver that works on
 * Amazon Alexa to force on/off
 *
 *
 * Version:  1.0
 *
 */
metadata {
    definition(name: "Virtual Resetting Switch", namespace: "waytotheweb", author: "Jonathan Michaelson") {
        capability "Switch"

	command "reset"
    }
}

def on() {
    log.info "on()"
    sendEvent(name: "switch", value: "on")
    runIn(2, "reset")
}

def off() {
    log.info "off()"
    sendEvent(name: "switch", value: "off")
    runIn(2, "reset")
}

def reset() {
    log.info "reset()"
    sendEvent(name: "switch", value: "reset")
}
