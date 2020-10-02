/*
Version 2020.10.02
Release Notes:
- Optimized to be used with Lutron Integration and Mirror Me

Revision History
Version 2020.08.16
Release Notes:
- Reduced whitespace
- Removed extra capabilities
- Removed moving attribute (windowShade comes with opening and closing attributes)
- Cleaned voltage attribute
- Cleaned parse function
Version 2020.06.08.01
Release Notes:
- Added "closing", "opening" and "partially open" states to the windowShade attribute
- Added "toggle" command
Version 2020.03.30 
- Initial Release
*/

metadata {
	definition (name: "Rollease Acmeda Shade", namespace: "arcautomate", author: "Younes Oughla", vid: "generic-shade") {
		capability "Refresh"
		capability "Switch"
		capability "SwitchLevel"
		command "stop"
		command "toggle"
		attribute "lastDirection", "enum"
		attribute "moving", "enum"
	}
	preferences {
		input ("motorAddress", "STRING", title: "Motor Address", description: "", defaultValue: "000", required: true, displayDuringSetup: true )
		input ("debug", "bool", title: "Enable debug logging", description: "", defaultValue: false, required: false, displayDuringSetup: false )
	}
}

def initialize() { logDebug "Motor Address: ${settings?.motorAddress}" }

def on() {
	open()
}

def off() {
	close()
}

def open() {
	logDebug "Opening Shade"
	sendCommand("m","000")
}

def close() {
	logDebug "Closing Shade"
	sendCommand("m","100")
}

def stop() {
	logDebug "Stopping Shade"
	sendCommand("s","")
}

def toggle() {
	logDebug "Toggling Shade"
    if (device.currentValue("moving") == "true") {
		stop()
    } else if (device.currentValue("lastDirection") == "true") {
		close()
	} else {
		open()
	}
}

def setLevel(level) {
	logDebug "Set Level: ${level}"
	levelString = 100-level
	levelString = "000${levelString}"
	levelString = levelString[-3..-1]
	sendCommand("m", levelString)
}

def sendCommand(String command, String commandData) {
	commandString = "!${motorAddress}${command}${commandData}"
	logDebug "Command Tx: ${commandString}"
	parent.sendTelnetCommand(commandString)
}

def parse(String msg) {
    logDebug "Command Rx: ${msg}"
	switch (msg.substring(4, 5)) {
   	    case "r":
			sendEvent(name: "moving", value: false)
			levelStr = msg.substring(5, 8)
			level = 100 - Integer.parseInt(levelStr)
	        if (level == 100) {
				sendEvent(name: "switch", value: true)
			} else if (level == 0) {
				sendEvent(name: "switch", value: false)
			} else {
				sendEvent(name: "switch", value: true)
			}
			sendEvent(name: "level", value: level)
		break;
		case "m":
			sendEvent(name: "moving", value: true)
			levelStr = msg.substring(5, 8)
			level = 100 - Integer.parseInt(levelStr)
			currentLevel = device.currentValue("level")
			if (level > currentLevel) {
				sendEvent(name: "switch", value: true)
				sendEvent(name: "lastDirection", value: true)
			} else if (level < currentLevel) {
				sendEvent(name: "lastDirection", value: false)
			}
		break;
		case "s":
			sendEvent(name: "moving", value: false)
		break;
	}
}

def refresh() {
	logDebug "Refreshing"
	sendCommand("r","?")
}

private def logDebug(message) {
	if (!debug) { return; }
	log.debug "${device.name} ${message}"  
}

private def logInfo(message) {
	log.info "${device.name} ${message}"
}

private def logWarning(message) {
	log.warn "${device.name} ${message}"
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}
