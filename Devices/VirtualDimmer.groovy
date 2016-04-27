/**
 *  Copyright 2015 SmartThings
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
 *  Z-Wave RGBW Light
 *
 *  Author: SmartThings
 *  Date: 2015-7-12
 *
 *  Changes
 *
 *  Version
 *  0.1.	26 Apr 2016	Correctly represent level = 0, in the UI
 * 
 */


metadata {
        definition (name: "Virtual Dimmer", namespace: "experthome", author: "rodrigo@experthome.cl") {
        capability "Switch"
        capability "Refresh"
        capability "Switch Level"
    }

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles (scale: 2) {
    	multiAttributeTile(name:"lValue", type: "generic", width: 6, height: 4) {
        	tileAttribute ("device.level", key: "PRIMARY_CONTROL") {
				attributeState "levelValue", label:'${currentValue}', unit:"", backgroundColor: "#53a7c0"
			}
        }
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Kids.kid10", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Kids.kid10", backgroundColor: "#79b821", nextState: "off"
		}
		standardTile("refresh", "device.switch", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}        
        controlTile("levelSliderControl", "device.level", "slider",height: 2, width: 6, inactiveLabel: false, backgroundColor:"#ffe71e") {
            state "level", action:"switch level.setLevel"
        }
        main(["lValue"])
		details(["lValue","levelSliderControl","button","refresh"])
	}
}

def parse(String description) {
}

def on() {
	sendEvent(name: "switch", value: "on")
    log.info "Dimmer On"
}

def off() {
	sendEvent(name: "switch", value: "off")
    log.info "Dimmer Off"
}

def setLevel(val){
    log.info "setLevel $val"
    
    if (val < 0) val = 0
    else if( val > 100) val = 100
    
    if(val == 0){
    off()
    sendEvent(name: "level", value: val)
    } else{
 	on()
 	sendEvent(name: "level", value: val)
    }
}

def refresh() {
    log.info "refresh"
}
