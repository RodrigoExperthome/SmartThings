/**
 *  Expert Alarm
 *
 *  Version 1.0.0 (8/12/2015)
 *  Inspired in SmartAlarm by statusbits.
 *
 *  The latest version of this file can be found on GitHub at:
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2015 Experthome.cl
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import groovy.json.JsonSlurper

definition(
    name: "Expert Alarm",
    namespace: "RodrigoExperthome",
    author: "rodrigo@experthome.cl",
    description: '''Sistema de alarma integrado a Experthome.cl. Permite monitoreo de casa, 
    y activación de sirenas, luces, luces, notificación sms y camaras foscam. Además, tiene integración tasker para
    keypad android (via botones/switch virtuales) y control remoto''',
    category: "Safety & Security",
    
    iconUrl: "http://statusbits.github.io/icons/SmartAlarm-128.png",
    iconX2Url: "http://statusbits.github.io/icons/SmartAlarm-256.png",
    //oauth: [displayName:"Smart Alarm", displayLink:"http://statusbits.github.io/smartalarm/"]
)

preferences {
    page name:"pageSetup" //Seteo general de la app
    page name:"pageSensores" //Sensores a seleccionar
    page name:"pageOpcionesSensor" //Tipo de armado para cada sensor
    page name:"pageOpcionesArmado" //Formas de armar/desarmar 
    page name:"pageOpcionesAlarma" //Acciones a realizar en caso de activación alarma
    page name:"pageEstado"
}

// Pagina de seteo
def pageSetup() {
    log.debug("pageSetup()")
    def pageProperties = [
        name:       "pageSetup"
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    return dynamicPage(pageProperties) {
        section("Opciones Alarma") {
            href "pageSensores", title:"Selecciona los sensores", description:"Toca para abrir"
            href "pageOpcionesArmado", title:"Opciones de armado", description:"Toca para abrir"
            href "pageOpcionesAlarma", title:"Opciones de alarma", description:"Toca para abrir"
        }
        section([title:"Opciones", mobileOnly:true]) {
            label title:"Asigna un nombre", required:false
        }
    }
}
// Selecciona los sensores, define puerta principal y delay.
def pageSensores() {
    LOG("pageSensores()")
    def resumen =
        "Seleccion de sensores a monitorear"
    
    def inputContact = [
        name:       "contacto",
        type:       "capability.contactSensor",
        title:      "Cuales sensores de puerta/ventana?",
        multiple:   true,
        required:   false
    ]
    def inputMotion = [
        name:       "movimiento",
        type:       "capability.motionSensor",
        title:      "Cuales sensores de movimiento?",
        multiple:   true,
        required:   false
    ]
    def pageProperties = [
        name:       "pageSensores",
        nextPage:   "pageOpcionesSensor",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Agrega/remueve sensores...") {
            paragraph resumen
            input inputContact
            input inputMotion
        }
    }
}

def pageOpcionesSensor() {
    LOG("pageOpcionesSensor()")
    def resumen = 
        "Cada sensor se puede configurar como Afuera o En Casa. " +
        "El armado En Casa considera que puede haber movimiento dentro de la  " +
        "casa sin generar una activacion de alarma. " +
        "Cuando la alarma se arma en modo Afuera, se activan los sensores Afuera y En Casa"
    def pageProperties = [
        name:       "pageOpcionesSensor",
        nextPage:   "pageSetup",
        uninstall:  false
    ]
    def tipoSensor = ["Afuera", "enCasa"]
    return dynamicPage(pageProperties) {
        section("Definir Opción Sensor") {
            paragraph resumen
        }
        if (settings.contacto) {
            def devices = settings.contacto.sort {it.displayName}
            devices.each() {
                def devId = it.id
                section("${it.displayName} (contacto)") {
                    input "type_${devId}", "enum", title:"Armado...", metadata:[values:tipoSensor], defaultValue:"Afuera"
                }
            }
        }

        if (settings.movimiento) {
            def devices = settings.movimiento.sort {it.displayName}
            devices.each() {
                def devId = it.id
                section("${it.displayName} (movimiento)") {
                    input "type_${devId}", "enum", title:"Armado...", metadata:[values:tipoSensor],defaultValue:"enCasa"
                }
            }
        }
        section("Definir Puerta Principal...") {
            input "inputPuerta","capability.contactSensor", title:"Puerta Principal", multiple:true, required: true
            input "inputDelay", "enum", title:"Retraso en Activacion (seg)", metadata:[values:["30","45","60"]], defaultValue:"30", required: true
        }
    }    
}        

/**
// Show "Arming/Disarming Options" page
def pageArmingOptions() {
    LOG("pageArmingOptions()")

    def helpArming =
        "Smart Alarm can be armed and disarmed by setting the home Mode. " +
        "There are two arming modes - Stay and Away. Interior zones are " +
        "not armed in Stay mode, allowing you to move freely inside your " +
        "home."

    def helpDelay =
        "Exit and entry delay allows you to exit the premises after arming " +
        "your alarm system and enter the premises while the alarm system " +
        "is armed without setting off an alarm. You can optionally disable " +
        "entry and exit delay when the alarm is armed in Stay mode."

    def inputAwayModes = [
        name:       "awayModes",
        type:       "mode",
        title:      "Arm 'Away' in these Modes",
        multiple:   true,
        required:   false
    ]

    def inputStayModes = [
        name:       "stayModes",
        type:       "mode",
        title:      "Arm 'Stay' in these Modes",
        multiple:   true,
        required:   false
    ]

    def inputDisarmModes = [
        name:       "disarmModes",
        type:       "mode",
        title:      "Disarm in these Modes",
        multiple:   true,
        required:   false
    ]

    def inputDelay = [
        name:       "delay",
        type:       "enum",
        metadata:   [values:["30","45","60","90"]],
        title:      "Delay (in seconds)",
        defaultValue: "30",
        required:   true
    ]

    def inputDelayStay = [
        name:       "stayDelayOff",
        type:       "bool",
        title:      "Disable delays in Stay mode",
        defaultValue: false,
        required:   true
    ]

    def pageProperties = [
        name:       "pageArmingOptions",
        //title:      "Arming/Disarming Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Arming/Disarming Options") {
            paragraph helpArming
        }

        section("Modes") {
            input inputAwayModes
            input inputStayModes
            input inputDisarmModes
        }

        section("Exit and Entry Delay") {
            paragraph helpDelay
            input inputDelay
            input inputDelayStay
        }
    }
}

// Show "Alarm Options" page
def pageAlarmOptions() {
    LOG("pageAlarmOptions()")

    def helpAlarm =
        "You can configure Smart Alarm to take several actions when an " +
        "alarm is set off, such as turning on sirens and light switches, " +
        "taking camera snapshots and executing a 'Hello, Home' action."

    def inputAlarms = [
        name:           "alarms",
        type:           "capability.alarm",
        title:          "Which sirens?",
        multiple:       true,
        required:       false
    ]

    def inputSirenMode = [
        name:           "sirenMode",
        type:           "enum",
        metadata:       [values:["Off","Siren","Strobe","Both"]],
        title:          "Choose siren mode",
        defaultValue:   "Both"
    ]

    def inputSwitches = [
        name:           "switches",
        type:           "capability.switch",
        title:          "Which switches?",
        multiple:       true,
        required:       false
    ]

    def inputCameras = [
        name:           "cameras",
        type:           "capability.imageCapture",
        title:          "Which cameras?",
        multiple:       true,
        required:       false
    ]

    def hhActions = getHelloHomeActions()
    def inputHelloHome = [
        name:           "helloHomeAction",
        type:           "enum",
        title:          "Which 'Hello, Home' action?",
        metadata:       [values: hhActions],
        required:       false
    ]

    def pageProperties = [
        name:       "pageAlarmOptions",
        //title:      "Alarm Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Alarm Options") {
            paragraph helpAlarm
        }
        section("Sirens") {
            input inputAlarms
            input inputSirenMode
        }
        section("Switches") {
            input inputSwitches
        }
        section("Cameras") {
            input inputCameras
        }
        section("'Hello, Home' Actions") {
            input inputHelloHome
        }
    }
}

// Show "Notification Options" page
def pageNotifications() {
    LOG("pageNotifications()")

    def helpAbout =
        "You can configure Smart Alarm to notify you when it is armed, " +
        "disarmed or when an alarm is set off. Notifications can be send " +
        "using either Push messages, SMS (text) messages and Pushbullet " +
        "messaging service. Smart Alarm can also notify you with sounds or " +
        "voice alerts using compatible audio devices, such as Sonos."

    def inputPushAlarm = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   true
    ]

    def inputPushStatus = [
        name:           "pushStatusMessage",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   true
    ]

    def inputPhone1 = [
        name:           "phone1",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone1Alarm = [
        name:           "smsAlarmPhone1",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone1Status = [
        name:           "smsStatusPhone1",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPhone2 = [
        name:           "phone2",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone2Alarm = [
        name:           "smsAlarmPhone2",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone2Status = [
        name:           "smsStatusPhone2",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPhone3 = [
        name:           "phone3",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone3Alarm = [
        name:           "smsAlarmPhone3",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone3Status = [
        name:           "smsStatusPhone3",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPhone4 = [
        name:           "phone4",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone4Alarm = [
        name:           "smsAlarmPhone4",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone4Status = [
        name:           "smsStatusPhone4",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPushbulletDevice = [
        name:           "pushbullet",
        type:           "device.pushbullet",
        title:          "Which Pushbullet devices?",
        multiple:       true,
        required:       false
    ]

    def inputPushbulletAlarm = [
        name:           "pushbulletAlarm",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   true
    ]

    def inputPushbulletStatus = [
        name:           "pushbulletStatus",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   true
    ]

    def inputAudioPlayers = [
        name:           "audioPlayer",
        type:           "capability.musicPlayer",
        title:          "Which audio players?",
        multiple:       true,
        required:       false
    ]

    def inputSpeechOnAlarm = [
        name:           "speechOnAlarm",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   true
    ]

    def inputSpeechOnStatus = [
        name:           "speechOnStatus",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   true
    ]

    def inputSpeechTextAlarm = [
        name:           "speechText",
        type:           "text",
        title:          "Alarm Phrase",
        required:       false
    ]

    def inputSpeechTextArmedAway = [
        name:           "speechTextArmedAway",
        type:           "text",
        title:          "Armed Away Phrase",
        required:       false
    ]

    def inputSpeechTextArmedStay = [
        name:           "speechTextArmedStay",
        type:           "text",
        title:          "Armed Stay Phrase",
        required:       false
    ]

    def inputSpeechTextDisarmed = [
        name:           "speechTextDisarmed",
        type:           "text",
        title:          "Disarmed Phrase",
        required:       false
    ]

    def pageProperties = [
        name:       "pageNotifications",
        //title:      "Notification Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Notification Options") {
            paragraph helpAbout
        }
        section("Push Notifications") {
            input inputPushAlarm
            input inputPushStatus
        }
        section("Text Message (SMS) #1") {
            input inputPhone1
            input inputPhone1Alarm
            input inputPhone1Status
        }
        section("Text Message (SMS) #2") {
            input inputPhone2
            input inputPhone2Alarm
            input inputPhone2Status
        }
        section("Text Message (SMS) #3") {
            input inputPhone3
            input inputPhone3Alarm
            input inputPhone3Status
        }
        section("Text Message (SMS) #4") {
            input inputPhone4
            input inputPhone4Alarm
            input inputPhone4Status
        }
        section("Pushbullet Notifications") {
            input inputPushbulletDevice
            input inputPushbulletAlarm
            input inputPushbulletStatus
        }
        section("Audio Notifications") {
            input inputAudioPlayers
            input inputSpeechOnAlarm
            input inputSpeechOnStatus
            input inputSpeechTextAlarm
            input inputSpeechTextArmedAway
            input inputSpeechTextArmedStay
            input inputSpeechTextDisarmed
        }
    }
}

// Show "Remote Control Options" page
def pageRemoteOptions() {
    LOG("pageRemoteOptions()")

    def helpRemote =
        "You can arm and disarm Smart Alarm using any compatible remote " +
        "control, for example Aeon Labs Minimote."

    def inputRemotes = [
        name:       "remotes",
        type:       "capability.button",
        title:      "Which remote controls?",
        multiple:   true,
        required:   false
    ]

    def inputArmAwayButton = [
        name:       "buttonArmAway",
        type:       "number",
        title:      "Which button?",
        required:   false
    ]

    def inputArmAwayHold = [
        name:       "holdArmAway",
        type:       "bool",
        title:      "Hold to activate",
        defaultValue: false,
        required:   true
    ]

    def inputArmStayButton = [
        name:       "buttonArmStay",
        type:       "number",
        title:      "Which button?",
        required:    false
    ]

    def inputArmStayHold = [
        name:       "holdArmStay",
        type:       "bool",
        title:      "Hold to activate",
        defaultValue: false,
        required:   true
    ]

    def inputDisarmButton = [
        name:       "buttonDisarm",
        type:       "number",
        title:      "Which button?",
        required:   false
    ]

    def inputDisarmHold = [
        name:       "holdDisarm",
        type:       "bool",
        title:      "Hold to activate",
        defaultValue: false,
        required:   true
    ]

    def inputPanicButton = [
        name:       "buttonPanic",
        type:       "number",
        title:      "Which button?",
        required:   false
    ]

    def inputPanicHold = [
        name:       "holdPanic",
        type:       "bool",
        title:      "Hold to activate",
        defaultValue: false,
        required:   true
    ]

    def pageProperties = [
        name:       "pageRemoteOptions",
        //title:      "Remote Control Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Remote Control Options") {
            paragraph helpRemote
            input inputRemotes
        }

        section("Arm Away Button") {
            input inputArmAwayButton
            input inputArmAwayHold
        }

        section("Arm Stay Button") {
            input inputArmStayButton
            input inputArmStayHold
        }

        section("Disarm Button") {
            input inputDisarmButton
            input inputDisarmHold
        }

        section("Panic Button") {
            input inputPanicButton
            input inputPanicHold
        }
    }
}

*/
def installed() {
    LOG("installed()")
    //initialize()
    state.installed = true
}

def updated() {
    LOG("updated()")

    unschedule()
    unsubscribe()
    //initialize()
}

/**
private def setupInit() {
    LOG("setupInit()")

    if (state.installed == null) {
        state.installed = false
        state.armed = false
        state.zones = []
        state.alarms = []
        state.history = []
    } else {
        def version = state.version as String
        if (version == null || version.startsWith('1')) {
            return false
        }
    }

    state.version = getVersion()
    return true
}

private def initialize() {
    log.info "Smart Alarm. Version ${getVersion()}. ${textCopyright()}"
    LOG("settings: ${settings}")

    clearAlarm()
    state.delay = settings.delay?.toInteger() ?: 30
    state.offSwitches = []
    state.history = []

    if (settings.awayModes?.contains(location.mode)) {
        state.armed = true
        state.stay = false
    } else if (settings.stayModes?.contains(location.mode)) {
        state.armed = true
        state.stay = true
    } else {
        state.armed = false
        state.stay = false
    }

    initZones()
    initButtons()
    initRestApi()
    subscribe(location, onLocation)

    STATE()
}

private def clearAlarm() {
    LOG("clearAlarm()")

    state.alarms = []
    settings.alarms*.off()

    // Turn off only those switches that we've turned on
    def switchesOff = state.offSwitches
    if (switchesOff) {
        LOG("switchesOff: ${switchesOff}")
        settings.switches.each() {
            if (switchesOff.contains(it.id)) {
                it.off()
            }
        }
        state.offSwitches = []
    }
}

private def initZones() {
    LOG("initZones()")

    state.zones = []

    state.zones << [
        deviceId:   null,
        sensorType: "panic",
        zoneType:   "alert",
        delay:      false
    ]

    if (settings.z_contact) {
        settings.z_contact.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "contact",
                zoneType:   settings["type_${it.id}"] ?: "exterior",
                delay:      settings["delay_${it.id}"]
            ]
        }
        subscribe(settings.z_contact, "contact.open", onContact)
    }

    if (settings.z_motion) {
        settings.z_motion.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "motion",
                zoneType:   settings["type_${it.id}"] ?: "interior",
                delay:      settings["delay_${it.id}"]
            ]
        }
        subscribe(settings.z_motion, "motion.active", onMotion)
    }

    if (settings.z_movement) {
        settings.z_movement.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "acceleration",
                zoneType:   settings["type_${it.id}"] ?: "interior",
                delay:      settings["delay_${it.id}"]
            ]
        }
        subscribe(settings.z_movement, "acceleration.active", onMovement)
    }

    if (settings.z_smoke) {
        settings.z_smoke.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "smoke",
                zoneType:   settings["type_${it.id}"] ?: "alert",
                delay:      settings["delay_${it.id}"]
            ]
        }
        subscribe(settings.z_smoke, "smoke.detected", onSmoke)
        subscribe(settings.z_smoke, "smoke.tested", onSmoke)
        subscribe(settings.z_smoke, "carbonMonoxide.detected", onSmoke)
        subscribe(settings.z_smoke, "carbonMonoxide.tested", onSmoke)
    }

    if (settings.z_water) {
        settings.z_water.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "water",
                zoneType:   settings["type_${it.id}"] ?: "alert",
                delay:      settings["delay_${it.id}"]
            ]
        }
        subscribe(settings.z_water, "water.wet", onWater)
    }

    state.zones.each() {
        def zoneType = it.zoneType

        if (zoneType == "alert") {
            it.armed = true
        } else if (zoneType == "exterior") {
            it.armed = state.armed
        } else if (zoneType == "interior") {
            it.armed = state.armed && !state.stay
        } else {
            it.armed = false
        }
    }
}

private def initButtons() {
    LOG("initButtons()")

    state.buttonActions = []
    if (settings.remotes) {
        if (settings.buttonArmAway) {
            def button = settings.buttonArmAway.toInteger()
            def event = settings.holdArmAway ? "held" : "pushed"
            state.buttonActions << [button:button, event:event, action:"armAway"]
        }

        if (settings.buttonArmStay) {
            def button = settings.buttonArmStay.toInteger()
            def event = settings.holdArmStay ? "held" : "pushed"
            state.buttonActions << [button:button, event:event, action:"armStay"]
        }

        if (settings.buttonDisarm) {
            def button = settings.buttonDisarm.toInteger()
            def event = settings.holdDisarm ? "held" : "pushed"
            state.buttonActions << [button:button, event:event, action:"disarm"]
        }

        if (settings.buttonPanic) {
            def button = settings.buttonPanic.toInteger()
            def event = settings.holdPanic ? "held" : "pushed"
            state.buttonActions << [button:button, event:event, action:"panic"]
        }

        if (state.buttonActions) {
            subscribe(settings.remotes, "button", onButtonEvent)
        }
    }
}





def onContact(evt)  { onZoneEvent(evt, "contact") }
def onMotion(evt)   { onZoneEvent(evt, "motion") }
def onMovement(evt) { onZoneEvent(evt, "acceleration") }
def onSmoke(evt)    { onZoneEvent(evt, "smoke") }
def onWater(evt)    { onZoneEvent(evt, "water") }

private def onZoneEvent(evt, sensorType) {
    LOG("onZoneEvent(${evt.displayName}, ${sensorType})")

    def zone = getZoneForDevice(evt.deviceId, sensorType)
    if (!zone) {
        log.warn "Cannot find zone for device ${evt.deviceId}"
        return
    }

    if (zone.armed) {
        state.alarms << evt.displayName
        if (zone.zoneType == "alert" || !zone.delay || (state.stay && settings.stayDelayOff)) {
            activateAlarm()
        } else {
            myRunIn(state.delay, activateAlarm)
        }
    }
}

def onLocation(evt) {
    LOG("onLocation(${evt.value})")

    String mode = evt.value
    if (settings.awayModes?.contains(mode)) {
        armAway()
    } else if (settings.stayModes?.contains(mode)) {
        armStay()
    } else if (settings.disarmModes?.contains(mode)) {
        disarm()
    }
}

def onButtonEvent(evt) {
    LOG("onButtonEvent(${evt.displayName})")

    if (!state.buttonActions || !evt.data) {
        return
    }

    def slurper = new JsonSlurper()
    def data = slurper.parseText(evt.data)
    def button = data.buttonNumber?.toInteger()
    if (button) {
        LOG("Button '${button}' was ${evt.value}.")
        def item = state.buttonActions.find {
            it.button == button && it.event == evt.value
        }

        if (item) {
            LOG("Executing '${item.action}' button action")
            "${item.action}"()
        }
    }
}

def armAway() {
    LOG("armAway()")

    if (!atomicState.armed || atomicState.stay) {
        armPanel(false)
    }
}

def armStay() {
    LOG("armStay()")

    if (!atomicState.armed || !atomicState.stay) {
        armPanel(true)
    }
}

def disarm() {
    LOG("disarm()")

    if (atomicState.armed) {
        state.armed = false
        state.zones.each() {
            if (it.zoneType != "alert") {
                it.armed = false
            }
        }

        reset()
    }
}

def panic() {
    LOG("panic()")

    state.alarms << "Panic"
    activateAlarm()
}

def reset() {
    LOG("reset()")

    unschedule()
    clearAlarm()

    // Send notification
    def msg = "${location.name} is "
    if (state.armed) {
        msg += "ARMED "
        msg += state.stay ? "STAY" : "AWAY"
    } else {
        msg += "DISARMED."
    }

    notify(msg)
    notifyVoice()
}

def exitDelayExpired() {
    LOG("exitDelayExpired()")

    def armed = atomicState.armed
    def stay = atomicState.stay
    if (!armed) {
        log.warn "exitDelayExpired: unexpected state!"
        STATE()
        return
    }

    state.zones.each() {
        def zoneType = it.zoneType
        if (zoneType == "exterior" || (zoneType == "interior" && !stay)) {
            it.armed = true
        }
    }

    def msg = "${location.name}: all "
    if (stay) {
        msg += "exterior "
    }
    msg += "zones are armed."

    notify(msg)
}

private def armPanel(stay) {
    LOG("armPanel(${stay})")

    unschedule()
    clearAlarm()

    state.armed = true
    state.stay = stay

    def armDelay = false
    state.zones.each() {
        def zoneType = it.zoneType
        if (zoneType == "exterior") {
            if (it.delay) {
                it.armed = false
                armDelay = true
            } else {
                it.armed = true
            }
        } else if (zoneType == "interior") {
            if (stay) {
                it.armed = false
            } else if (it.delay) {
                it.armed = false
                armDelay = true
            } else {
                it.armed = true
            }
        }
    }

    def delay = armDelay && !(stay && settings.stayDelayOff) ? atomicState.delay : 0
    if (delay) {
        myRunIn(delay, exitDelayExpired)
    }

    def mode = stay ? "STAY" : "AWAY"
    def msg = "${location.name} "
    if (delay) {
        msg += "will arm ${mode} in ${state.delay} seconds."
    } else {
        msg += "is ARMED ${mode}."
    }

    notify(msg)
    notifyVoice()
}

// .../armaway REST API endpoint
def apiArmAway() {
    LOG("apiArmAway()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    if (settings.pincode && settings.armWithPin && (params.pincode != settings.pincode.toString())) {
        log.error "Invalid PIN code '${params.pincode}'"
        return httpError(403, "Access denied")
    }

    armAway()
    return apiStatus()
}

// .../armstay REST API endpoint
def apiArmStay() {
    LOG("apiArmStay()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    if (settings.pincode && settings.armWithPin && (params.pincode != settings.pincode.toString())) {
        log.error "Invalid PIN code '${params.pincode}'"
        return httpError(403, "Access denied")
    }

    armStay()
    return apiStatus()
}

// .../disarm REST API endpoint
def apiDisarm() {
    LOG("apiDisarm()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    if (settings.pincode && (params.pincode != settings.pincode.toString())) {
        log.error "Invalid PIN code '${params.pincode}'"
        return httpError(403, "Access denied")
    }

    disarm()
    return apiStatus()
}

// .../panic REST API endpoint
def apiPanic() {
    LOG("apiPanic()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    panic()
    return apiStatus()
}

// .../status REST API endpoint
def apiStatus() {
    LOG("apiStatus()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    def status = [
        status: state.armed ? (state.stay ? "armed stay" : "armed away") : "disarmed",
        alarms: state.alarms
    ]

    return status
}

def activateAlarm() {
    LOG("activateAlarm()")

    if (state.alarms.size() == 0) {
        log.warn "activateAlarm: false alarm"
        return
    }

    switch (settings.sirenMode) {
    case "Siren":
        settings.alarms*.siren()
        break

    case "Strobe":
        settings.alarms*.strobe()
        break
        
    case "Both":
        settings.alarms*.both()
        break
    }

    // Only turn on those switches that are currently off
    def switchesOn = settings.switches?.findAll { it?.currentSwitch == "off" }
    LOG("switchesOn: ${switchesOn}")
    if (switchesOn) {
        switchesOn*.on()
        state.offSwitches = switchesOn.collect { it.id }
    }

    settings.cameras*.take()

    if (settings.helloHomeAction) {
        log.info "Executing HelloHome action '${settings.helloHomeAction}'"
        location.helloHome.execute(settings.helloHomeAction)
    }

    def msg = "Alarm at ${location.name}!"
    state.alarms.each() {
        msg += "\n${it}"
    }

    notify(msg)
    notifyVoice()

    myRunIn(180, reset)
}

private def notify(msg) {
    LOG("notify(${msg})")

    log.info msg

    if (state.alarms.size()) {
        // Alarm notification
        if (settings.pushMessage) {
            mySendPush(msg)
        } else {
            sendNotificationEvent(msg)
        }

        if (settings.smsAlarmPhone1 && settings.phone1) {
            sendSms(phone1, msg)
        }

        if (settings.smsAlarmPhone2 && settings.phone2) {
            sendSms(phone2, msg)
        }

        if (settings.smsAlarmPhone3 && settings.phone3) {
            sendSms(phone3, msg)
        }

        if (settings.smsAlarmPhone4 && settings.phone4) {
            sendSms(phone4, msg)
        }

        if (settings.pushbulletAlarm && settings.pushbullet) {
            settings.pushbullet*.push(location.name, msg)
        }   
    } else {
        // Status change notification
        if (settings.pushStatusMessage) {
            mySendPush(msg)
        } else {
            sendNotificationEvent(msg)
        }

        if (settings.smsStatusPhone1 && settings.phone1) {
            sendSms(phone1, msg)
        }

        if (settings.smsStatusPhone2 && settings.phone2) {
            sendSms(phone2, msg)
        }

        if (settings.smsStatusPhone3 && settings.phone3) {
            sendSms(phone3, msg)
        }

        if (settings.smsStatusPhone4 && settings.phone4) {
            sendSms(phone4, msg)
        }

        if (settings.pushbulletStatus && settings.pushbullet) {
            settings.pushbullet*.push(location.name, msg)
        }
    }
}

private def notifyVoice() {
    LOG("notifyVoice()")

    if (!settings.audioPlayer) {
        return
    }

    def phrase = null
    if (state.alarms.size()) {
        // Alarm notification
        if (settings.speechOnAlarm) {
            phrase = settings.speechText ?: getStatusPhrase()
        }
    } else {
        // Status change notification
        if (settings.speechOnStatus) {
            if (state.armed) {
                if (state.stay) {
                    phrase = settings.speechTextArmedStay ?: getStatusPhrase()
                } else {
                    phrase = settings.speechTextArmedAway ?: getStatusPhrase()
                }
            } else {
                phrase = settings.speechTextDisarmed ?: getStatusPhrase()
            }
        }
    }

    if (phrase) {
        settings.audioPlayer*.playText(phrase)
    }
}

private def history(String event, String description = "") {
    LOG("history(${event}, ${description})")

    def history = atomicState.history
    history << [time: now(), event: event, description: description]
    if (history.size() > 10) {
        history = history.sort{it.time}
        history = history[1..-1]
    }

    LOG("history: ${history}")
    state.history = history
}

private def getStatusPhrase() {
    LOG("getStatusPhrase()")

    def phrase = ""
    if (state.alarms.size()) {
        phrase = "Alarm at ${location.name}!"
        state.alarms.each() {
            phrase += " ${it}."
        }
    } else {
        phrase = "${location.name} security is "
        if (state.armed) {
            def mode = state.stay ? "stay" : "away"
            phrase += "armed in ${mode} mode."
        } else {
            phrase += "disarmed."
        }
    }

    return phrase
}

private def getHelloHomeActions() {
    def actions = location.helloHome?.getPhrases().collect() { it.label }
    return actions.sort()
}

private def getAlarmStatus() {
    def alarmStatus

    if (atomicState.armed) {
        alarmStatus = "ARMED "
        alarmStatus += atomicState.stay ? "STAY" : "AWAY"
    } else {
        alarmStatus = "DISARMED"
    }

    return alarmStatus
}

private def getZoneStatus(device, sensorType) {

    def zone = getZoneForDevice(device.id, sensorType)
    if (!zone) {
        return null
    }

    def str = "${device.displayName}: ${zone.zoneType}, "
    str += zone.armed ? "armed, " : "disarmed, "
    str += device.currentValue(sensorType)

    return str
}

private def getZoneForDevice(id, sensorType) {
    return state.zones.find() { it.deviceId == id && it.sensorType == sensorType }
}

private def isZoneReady(device, sensorType) {
    def ready

    switch (sensorType) {
    case "contact":
        ready = "closed".equals(device.currentValue("contact"))
        break

    case "motion":
        ready = "inactive".equals(device.currentValue("motion"))
        break

    case "acceleration":
        ready = "inactive".equals(device.currentValue("acceleration"))
        break

    case "smoke":
        ready = "clear".equals(device.currentValue("smoke"))
        break

    case "water":
        ready = "dry".equals(device.currentValue("water"))
        break

    default:
        ready = false
    }

    return ready
}

private def getDeviceById(id, sensorType) {
    switch (sensorType) {
    case "contact":
        return settings.z_contact?.find() { it.id == id }

    case "motion":
        return settings.z_motion?.find() { it.id == id }

    case "acceleration":
        return settings.z_movement?.find() { it.id == id }

    case "smoke":
        return settings.z_smoke?.find() { it.id == id }

    case "water":
        return settings.z_water?.find() { it.id == id }
    }

    return null
}

private def getNumZones() {
    def numZones = 0

    numZones += settings.z_contact?.size() ?: 0
    numZones += settings.z_motion?.size() ?: 0
    numZones += settings.z_movement?.size() ?: 0
    numZones += settings.z_smoke?.size() ?: 0
    numZones += settings.z_water?.size() ?: 0

    return numZones
}

private def myRunIn(delay_s, func) {
    if (delay_s > 0) {
        def date = new Date(now() + (delay_s * 1000))
        runOnce(date, func)
        LOG("scheduled '${func}' to run at ${date}")
    }
}

private def mySendPush(msg) {
    // sendPush can throw an exception
    try {
        sendPush(msg)
    } catch (e) {
        log.error e
    }
}

private def getVersion() {
    return "2.4.3"
}

private def textCopyright() {
    def text = "Copyright © 2014 Statusbits.com"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def LOG(message) {
    //log.trace message
}

private def STATE() {
    //log.trace "state: ${state}"
}
