/**
 * Simple Virtual Window/Curtain/Roller/Shutter/Shade Driver that works on
 * Amazon Alexa
 *
 */
metadata {
    definition(name: "Virtual Window Blinds", namespace: "waytotheweb", author: "Jonathan Michaelson") {
        capability "Window Shade"
    }
}

def close() {
    log.info "close()"
    sendEvent(name: "windowShade", value: "closed")
    sendEvent(name: "position", value: "0")
}

def open() {
    log.info "open()"
    sendEvent(name: "windowShade", value: "open")
    sendEvent(name: "position", value: "100")
}

def setPosition(data){
    log.info "setPosition()"
    if (data == 100) {
        open()
    } else if (data == 0) {
        close()
    } else {
        sendEvent(name: "windowShade", value: "open")
        sendEvent(name: "position", value: data)
    }
}
