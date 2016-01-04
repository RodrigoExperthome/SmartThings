/**
 * ExpertHome Aeon Multi 6 device type 
 * 
 * Copyright 2015 SmartThings
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
 * Change Log
 *
 *  v2.3: (i)   Include ExpertHome UI 
 *        (ii)  Reporting Interval - 900 seg.
 *        (iii) Battery Operated Icon
 */
 
 metadata {
	definition (name: "Aeon Multisensor 6 v2.3", namespace: "ExpertHome", author: "rodrigo@experthome.cl") {
		capability "Motion Sensor"
		capability "Acceleration Sensor"
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Illuminance Measurement"
		capability "Ultraviolet Index"
		capability "Configuration"
		capability "Sensor"
		capability "Battery"

		attribute "tamper", "enum", ["detected", "clear"]
		attribute "batteryStatus", "string"
		attribute "powerSupply", "enum", ["USB Cable", "Battery"]
		attribute "needUpdate", "string"
        
		fingerprint deviceId: "0x2101", inClusters: "0x5E,0x86,0x72,0x59,0x85,0x73,0x71,0x84,0x80,0x30,0x31,0x70,0x7A", outClusters: "0x5A"
	}
    simulator {
    //To Do
    }
    
    tiles(scale: 2) {
		multiAttributeTile(name:"main", type: "generic", width: 6, height: 4){
			tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
				attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
			}
			tileAttribute("device.illuminance", key: "SECONDARY_CONTROL") {
            	attributeState "luminosity",label:'${currentValue} ${unit}', unit:"lux", backgroundColors:[
                	[value: 0, color: "#000000"],
                    [value: 1, color: "#060053"],
                    [value: 3, color: "#3E3900"],
                    [value: 12, color: "#8E8400"],
					[value: 24, color: "#C5C08B"],
					[value: 36, color: "#DAD7B6"],
					[value: 128, color: "#F3F2E9"],
                    [value: 1000, color: "#FFFFFF"]
				]
			}
		}
		valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
			state "temperature", label:'${currentValue}°',
			backgroundColors:[
				[value: 0, color: "#153591"],
				[value: 7, color: "#1e9cbb"],
				[value: 15, color: "#90d2a7"],
				[value: 23, color: "#44b621"],
				[value: 28, color: "#f1d801"],
				[value: 33, color: "#d04e00"],
				[value: 37, color: "#bc2323"]
			]
		}
		valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
			state "humidity", label:'${currentValue}% humidty', unit:""
		}
		standardTile("acceleration", "device.acceleration", width: 2, height: 2) {
			state("active", label:'tamper', icon:"st.motion.acceleration.active", backgroundColor:"#ff0000")
			state("inactive", label:'clear', icon:"st.motion.acceleration.inactive", backgroundColor:"#00ff00")
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		//Not implemented and copied from Fibaro device type
		standardTile("synced", "device.needUpdate", inactiveLabel: false, width: 2, height: 2) {
            state "No" , label:'Synced', action:"configuration.configure", icon:"st.secondary.refresh-icon", backgroundColor:"#99CC33"
            state "Yes", label:'Pending', action:"configuration.configure", icon:"st.secondary.refresh-icon", backgroundColor:"#CCCC33"
        }
        standardTile("configure","device.configure", decoration: "flat", width: 2, height: 2) {
			state "configure", label:'config', action:"configure", icon:"st.secondary.tools"
		}
		valueTile("powerSupply", "device.powerSupply", height: 2, width: 2, decoration: "flat") {
			state "powerSupply", label:'${currentValue} powered', backgroundColor:"#ffffff"
		}

		main(["main"])
		details(["main","temperature", "humidity", "acceleration", "battery", "powerSupply", "configure"])
	}
 }
 
 def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "${device.displayName} is now ${device.latestValue("powerSupply")}"
	if (device.latestValue("powerSupply") == "USB Cable") {  //case1: USB powered
		response(configure())
	} else if (device.latestValue("powerSupply") == "Battery") {  //case2: battery powered
		// setConfigured("false") is used by WakeUpNotification
		setConfigured("false") //wait until the next time device wakeup to send configure command after user change preference
	} else { //case3: power source is not identified, ask user to properly pair the sensor again
		log.warn "power source is not identified, check it sensor is powered by USB, if so > configure()"
		def request = []
		request << zwave.configurationV1.configurationGet(parameterNumber: 101)
		response(commands(request))
	}
}
