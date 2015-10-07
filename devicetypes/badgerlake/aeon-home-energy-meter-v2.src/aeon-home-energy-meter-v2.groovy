/**
 *  Aeon Home Energy Meter V2
 *
 *  Copyright 2015 Lance J Nelson
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
 */
metadata {
	definition (
		name: 		"Aeon Home Energy Meter V2", 
		namespace: 	"badgerlake",
		author: 	"Lance J Nelson"
	) 
	{
    	capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
        capability "Refresh"
        capability "Polling"
        
        attribute "energy", "string"
        attribute "power", "string"
        attribute "volts", "string"
        attribute "amps", "string"
        
		command "reset"
        command "configure"
        command "refresh"
        command "poll"

		fingerprint deviceId: "0x3101", inClusters: "0x70,0x32,0x60,0x85,0x56,0x72,0x86"
	}

	// simulator metadata
	simulator {
	}

	// tile definitions
	tiles(scale: 2) {
    
    	valueTile("amps", "device.amps", width: 6, height: 2){
            	state ("amps", label: '${currentValue} Amps', 
                	backgroundColors:
                    [
						[value: 0, color: "#2133b6"],
                        [value: 0.1, color: "#44b621"],
                        [value: 100, color: "#44b621"],
						[value: 150, color: "#FF8C00"],
                        [value: 200, color: "#FF2600"]
                    ]
                )
    	}
    
    // Watts row
		valueTile("power", "device.power", width: 2, height: 1, decoration: "flat") {
			state ("power", label:'${currentValue} Watts')
		}

	// Power row
		valueTile("energy", "device.energy", width: 2, height: 1, decoration: "flat") {
			state("energy", label: '${currentValue} kWh')
		}
        
    // Volts row
        valueTile("volts", "device.volts", width: 2, height: 1, decoration: "flat") {
        	state("volts", label: '${currentValue} Volts',
                foregroundColors:
                [
                	[value: 1, color: "#FF2600"],
                    [value: 110, color: "#00F600"],
                    [value: 120, color: "#00F600"]
                ]
            )
        }
        
    // Controls row
		standardTile("reset", "command.reset", inactiveLabel: false) {
			state "default", label:'reset', action:"reset", icon: "st.Health & Wellness.health7"
		}
		standardTile("refresh", "command.refresh", inactiveLabel: false) {
			state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
		}
		standardTile("configure", "command.configure", inactiveLabel: false) {
			state "configure", label:'', action: "configure", icon:"st.secondary.configure"
		}

		main "amps"
		details([
			"amps",
            "power","volts","energy",
			"reset","refresh","configure"
		])
	}
}

def installed() {
	reset()
	configure()
	refresh()
}

def updated() {
	configure()
	refresh()
}

def parse(String description) {
    def result = null
    def cmd = zwave.parse(description)
    if (cmd) {
        result = createEvent(zwaveEvent(cmd))
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

// Devices that support the Security command class can send messages in an
// encrypted form; they arrive wrapped in a SecurityMessageEncapsulation
// command and must be unencapsulated
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
        def encapsulatedCommand = cmd.encapsulatedCommand([0x98: 1, 0x20: 1])

        // can specify command class versions here like in zwave.parse
        if (encapsulatedCommand) {
                return zwaveEvent(encapsulatedCommand)
        }
}
/*
private secure(physicalgraph.zwave.Command cmd) {
	return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}
*/

        
def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd, Short source) {        
		def map = null

        if (cmd.meterType == 1) {
                if (cmd.scale == 0) {
                		if (source == 1) { state.energyClamp1 = cmd.scaledMeterValue; }
                        else if (source == 2) {state.energyClamp2 = cmd.scaledMeterValue; }
                        map = [name: "energy", value: (state.energyClamp1 ?: 0) + (state.energyClamp2 ?: 0), unit: "kWh"]
                } else if (cmd.scale == 1) {
                        if (source == 1) { state.energyClamp1 = cmd.scaledMeterValue; }
                        else if (source == 2) {state.energyClamp2 = cmd.scaledMeterValue; }
                        map = [name: "energy", value: (state.energyClamp1 ?: 0) + (state.energyClamp2 ?: 0), unit: "kVAh"]
                } else if (cmd.scale == 2) {
                        if (source == 1) { state.powerClamp1 = cmd.scaledMeterValue; }
                        else if (source == 2) {state.powerClamp2 = cmd.scaledMeterValue; }
                        map = [name: "power", value: (state.powerClamp1 ?: 0) + (state.powerClamp2 ?: 0), unit: "W"]
                } else if (cmd.scale == 4) {
                        if (source == 1) { state.voltsClamp1 = cmd.scaledMeterValue; }
                        else if (source == 2) {state.voltsClamp2 = cmd.scaledMeterValue; }
                        map = [name: "volts", value: (state.voltsClamp1 ?: 0) + (state.voltsClamp2 ?: 0), unit: "V"]
                } else if (cmd.scale == 5) {
                        if (source == 1) { state.ampsClamp1 = cmd.scaledMeterValue; }
                        else if (source == 2) {state.ampsClamp2 = cmd.scaledMeterValue; }
                        map = [name: "amps", value: Math.round(((state.ampsClamp1 ?: 0) + (state.ampsClamp2 ?: 0)) / 2 * 100) / 100, unit: "A"]
                } else {
                        map = [name: "unk", value: cmd.scaledMeterValue, unit: "unk"]
                }
        }
        if (map) {
                if (cmd.previousMeterValue && cmd.previousMeterValue != cmd.meterValue) {
                        map.descriptionText = "${device.displayName} ${map.name} is ${map.value} ${map.unit}, previous: ${cmd.scaledPreviousMeterValue}"
                }
                createEvent(map)
        } else {
                null
        }
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
log.debug "V3 ********* meterType is ${cmd.meterType} and scale is ${cmd.scale} and full cmd is ${cmd} ********"
        def map = null

        if (cmd.meterType == 1) {
                if (cmd.scale == 0) {
                        map = [name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
                } else if (cmd.scale == 1) {
                        map = [name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
                } else if (cmd.scale == 2) {
                        map = [name: "power", value: cmd.scaledMeterValue, unit: "W"]
                } else if (cmd.scale == 4) {
                        map = [name: "volts", value: cmd.scaledMeterValue, unit: "V"]
                } else if (cmd.scale == 5) {
                        map = [name: "amps", value: cmd.scaledMeterValue, unit: "A"]
                } else {
                        map = [name: "unk", value: cmd.scaledMeterValue, unit: "unk"]
                }
        }
        if (map) {
                if (cmd.previousMeterValue && cmd.previousMeterValue != cmd.meterValue) {
                        map.descriptionText = "${device.displayName} ${map.name} is ${map.value} ${map.unit}, previous: ${cmd.scaledPreviousMeterValue}"
                }
                createEvent(map)
        } else {
                null
        }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
        // def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1]) // can specify command class versions here like in zwave.parse
		def encapsulatedCommand = cmd.encapsulatedCommand()

        log.debug ("Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}")

        if (encapsulatedCommand) {
                return zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint)
        }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiInstanceCmdEncap cmd) {
        def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1])  // can specify command class versions here like in zwave.parse
        
        log.debug ("Command from instance ${cmd.instance}: ${encapsulatedCommand}")

        if (encapsulatedCommand) {
                return zwaveEvent(encapsulatedCommand)
        }
}
def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
    //log.debug "Unhandled event ${cmd}"
	[:]
}

def refresh() {
	log.debug "refresh()"
    
	delayBetween([
        zwave.meterV2.meterGet(scale: 0).format(),
        zwave.meterV2.meterGet(scale: 2).format(),
		zwave.meterV2.meterGet(scale: 4).format(),
		zwave.meterV2.meterGet(scale: 5).format()
	])
}

def poll() {
	log.debug "poll()"
	refresh()
}

def reset() {
	log.debug "reset()"

	state.energy = -1
	state.power = -1
	state.amps = -1
	state.volts = -1

    def dateString = new Date().format("M/d/YY", location.timeZone)
    def timeString = new Date().format("h:mm a", location.timeZone)    
	state.lastResetTime = "Since\n"+dateString+"\n"+timeString

	def cmd = delayBetween( [
		zwave.meterV2.meterReset().format(),			// Reset all values
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format(),
        zwave.meterV2.meterGet(scale: 4).format(),
		zwave.meterV2.meterGet(scale: 5).format()
	], 1000)
    cmd
    
    configure()
}

def configure() {
	log.debug "configure()"
    
	def cmd = delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 0).format(),			// Disable (=0) selective reporting
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: (256+512+2048+4096+65536+131072+524288+1048576)).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 10).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 0).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 100).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 3000).format()
	], 2000)
	log.debug cmd

	cmd
}

private secure(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}