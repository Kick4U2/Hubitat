/* 

Version 2020.08.16
Release Notes:
- Reduced whitespace
- Removed extra capabilities
- Removed moving attribute (windowShade comes with opening and closing attributes)
- Cleaned voltage attribute
- Cleaned parse function


Revision History

Version 2020.06.08.01
Release Notes:
- Added "closing", "opening" and "partially open" states to the windowShade attribute
- Added "toggle" command

Version 2020.03.30 
- Initial Release

*/

metadata {
	definition (name: "Rollease Acmeda Shade", namespace: "arcautomate", author: "Younes Oughla", vid: "generic-shade") {
		capability "Actuator"
		capability "Initialize"
		capability "Refresh"
		capability "VoltageMeasurement"
		capability "WindowShade"
		command  "stop"
		command  "toggle"
		attribute "lastDirection", "enum"
		attribute "position", "int"
		attribute "voltage","int"
	}
	preferences {
		input ("motorAddress", "STRING", title: "Motor Address", description: "", defaultValue: "000", required: true, displayDuringSetup: true )
		input ("debug", "bool", title: "Enable debug logging", description: "", defaultValue: false, required: false, displayDuringSetup: false )
	}
}

def initialize() { logDebug "Motor Address: ${settings?.motorAddress}" }

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
	currentStatus = device.currentValue("windowShade")
	switch (currentStatus) {
		case "opening":
		case "closing":
			stop()
			break;
		case "open":
			close()
			break;
		case "closed":
			open()
			break;
		case "partially open":
        		if (device.currentValue("lastDirection") == "true") {
				close()
			} else {
				open()
			}
			break;
		default:
			open()
	}
}

def setPosition(position) {
	logDebug "Set Position: ${position}"
	positionString = 100-position
	positionString = "000${positionString}"
	positionString = positionString[-3..-1]
	sendCommand("m",positionString)
}

def sendCommand(String command, String commandData) {
	commandString = "!${motorAddress}${command}${commandData}"
	logDebug "Send Command: ${commandString}"
	parent.sendTelnetCommand(commandString)
}

def parse(String msg) {
	switch (msg.substring(4, 5)) {
		case "r":
			positionStr = msg.substring(5, 8)
			position = 100 - Integer.parseInt(positionStr)
			positionUpdated(position)
			break;
		case "m":
			positionStr = msg.substring(5, 8)
			targetPosition = 100 - Integer.parseInt(positionStr)
			currentPosition = device.currentValue("position")
			if (targetPosition > currentPosition) {
				sendEvent(name: "windowShade", value: "opening")
                sendEvent(name: "lastDirection", value: true)
			}
			if (targetPosition < currentPosition) {
				sendEvent(name: "windowShade", value: "closing")
                sendEvent(name: "lastDirection", value: false)
			}
			break;
		case "p":
			if (msg.substring(5, 7) == "Vc") {
				voltageStr = msg.substring(7, 12)
				voltageStr = Integer.parseInt(voltageStr) / 100
				sendEvent(name: "voltage", value: voltageStr)
			}
			break;
	}
}

def positionUpdated(position) {
	logDebug "Position Updated: ${position}"
	if (position == 100) {
		sendEvent(name: "windowShade", value: "open")
	} else if (position == 0) {
		sendEvent(name: "windowShade", value: "closed")
	} else {
		sendEvent(name: "windowShade", value: "partially open")
	}
	sendEvent(name: "position", value: position)
}

def requestStatus() {
	sendCommand("r","?")
}

def refresh() {
	logDebug "Refreshing"
	requestStatus()
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
