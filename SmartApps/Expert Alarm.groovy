/**
 *  Expert Alarm
 *
 *  Version 1.5.0 (06/Oct/2015)
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
    name:           "Expert Alarm",
    namespace:      "ExpertHome",
    author:         "rodrigo@experthome.cl",
    description:    "Sistema de alarma integrado a Experthome.cl",
    category:       "Safety & Security",
    iconUrl:        "http://experthome.cl/wp-content/uploads/2015/08/Security_14.png",
    iconX2Url:      "http://experthome.cl/wp-content/uploads/2015/08/Security_14.png",
)

preferences {
    page name:"pageStatus"
    page name:"pageSensores" 
    page name:"pageOpcionesSensor" 
    page name:"pageOpcionesActivacion"
    page name:"pageOpcionesAlarma" 
}

def pageStatus() {
    def alarmStatus = "La Alarma está ${statusAlarma()}"
    def pageProperties = [
        name:       "pageStatus",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    return dynamicPage(pageProperties) {
        section("Estado Alarma") {
            paragraph alarmStatus
        }
        section("Menu") {
            href "pageSensores", title:"Sensores", description:"Toca para abrir"
            href "pageOpcionesSensor", title:"Armado Afuera/Casa", description:"Toca para abrir"
            href "pageOpcionesActivacion", title:"Activacion Alarma", description:"Toca para abrir"
            href "pageOpcionesAlarma", title:"Alerta & Notificaciones", description:"Toca para abrir"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def pageSensores() {
    //log.debug("pageSensores()")
    def inputContact = [
        name:       "contacto",
        type:       "capability.contactSensor",
        title:      "Sensores de puerta/ventana",
        multiple:   true,
        required:   false
    ]
    def inputMotion = [
        name:       "movimiento",
        type:       "capability.motionSensor",
        title:      "Sensores de movimiento",
        multiple:   true,
        required:   false
    ]
    def pageProperties = [
        name:       "pageSensores",
        nextPage:   "pageOpcionesSensor",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Agrega/remueve sensores a ser monitoreados") {
            input inputContact
            input inputMotion
        }
    }
}
def pageOpcionesSensor() {
    //log.debug("pageOpcionesSensor()")
    def resumen = 
        "Cada sensor se puede configurar como Afuera o Casa."
        
    def pageProperties = [
        name:       "pageOpcionesSensor",
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    def tipoSensor = ["Afuera", "Casa"]
    return dynamicPage(pageProperties) {
        section("Afuera/Casa") {
            paragraph resumen
        }
        if (settings.contacto) {
            section("Contacto") {
                def devices = settings.contacto.sort {it.displayName}
                devices.each() {
                    def devId = it.id
                    def displayName = it.displayName
                    input "type_${devId}", "enum", title:displayName, metadata:[values:tipoSensor], defaultValue:"Casa"
                }
            }
        }
        if (settings.movimiento) {
            section("Movimiento") {
                def devices = settings.movimiento.sort {it.displayName}
                devices.each() {
                    def devId = it.id
                    def displayName = it.displayName
                        input "type_${devId}", "enum", title:"${it.displayName}", metadata:[values:tipoSensor],defaultValue:"Afuera"
                }
            }
        }
        section("Puerta Principal") {
            input "puertaPrincipal","capability.contactSensor", title:"Selecciona", multiple:true, required: false
            input "delayPuerta", "enum", title:"Retraso en Activacion (seg)", metadata:[values:["30","45","60"]], defaultValue:"30", required: true
        }
    }    
}        
def pageOpcionesActivacion() {
    //log.debug("pageOpcionesActivacion()")
    def resumenRemotos =    
        "Botones: (1) Afuera, (2) Casa, (3) Desactivar, (4) Panico"
    
    def inputModoAfuera = [
        name:       "modosAfuera",
        type:       "mode",
        title:      "Armado Afuera en estos modos...",
        multiple:   true,
        required:   false
    ]
    def inputRemotes = [
        name:       "remoto",
        type:       "capability.button",
        title:      "Que control remoto?",
        multiple:   true,
        required:   false
    ]
    def inputMomentaryAfuera = [
        name:       "momentaryAfuera",
        type:       "capability.momentary",
        title:      "Afuera?",
        multiple:   false,
        required:   false
    ]
    def inputMomentaryCasa = [
        name:       "momentaryCasa",
        type:       "capability.momentary",
        title:      "Casa?",
        multiple:   false,
        required:   false
    ]
    def inputMomentaryDesactivar = [
        name:       "momentaryDesactivar",
        type:       "capability.momentary",
        title:      "Desarmado?",
        multiple:   false,
        required:   false
    ]
    def inputMomentaryPanico = [
        name:       "momentaryPanico",
        type:       "capability.momentary",
        title:      "Panico?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchAfuera = [
        name:       "switchAfuera",
        type:       "capability.switch",
        title:      "Afuera?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchCasa = [
        name:       "switchCasa",
        type:       "capability.switch",
        title:      "Casa?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchDesactivar = [
        name:       "switchDesactivar",
        type:       "capability.switch",
        title:      "Desarmado?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchPanico = [
        name:       "switchPanico",
        type:       "capability.switch",
        title:      "Panico?",
        multiple:   false,
        required:   false
    ]
    def pageProperties = [
        name:       "pageOpcionesActivacion",
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Opciones de Activación Alarma") {
        }
        section("Modos") {
            input inputModoAfuera
        }
        section("Controles Remotos") {
           paragraph resumenRemotos
           input inputRemotes
        }
        section("Botonera") {
           input inputMomentaryAfuera
           input inputMomentaryCasa
           input inputMomentaryDesactivar
           input inputMomentaryPanico
        }
        section("Switch para Status"){
           input inputSwitchAfuera
           input inputSwitchCasa
           input inputSwitchDesactivar
           input inputSwitchPanico
        }
        
    }
}
def pageOpcionesAlarma() {
    //log.debug("pageOpcionesAlarma()")
    def inputSirena = [
        name:           "sirena",
        type:           "capability.alarm",
        title:          "Que sirenas?",
        multiple:       true,
        required:       false
    ]
    def inputLuces = [
        name:           "luces",
        type:           "capability.switch",
        title:          "Que luces?",
        multiple:       true,
        required:       false
    ]
    def inputCamaras = [
        name:           "camaras",
        type:           "capability.imageCapture",
        title:          "Que camaras?",
        multiple:       true,
        required:       false
    ]
    def inputPush = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Mensaje Push?",
        defaultValue:   true
    ]
    def inputPhone1 = [
        name:           "phone1",
        type:           "phone",
        title:          "Envia a este numero",
        required:       false
    ]
    def inputPhone2 = [
        name:           "phone2",
        type:           "phone",
        title:          "Envia a este numero",
        required:       false
    ]
    
    def pageProperties = [
        name:       "pageOpcionesAlarma",
        nextPage:   "pageStatus",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Alerta") {
            input inputSirena
            input inputLuces
            input inputCamaras
        }
        section("Notificaciones") {
            input inputPush
            input inputPhone1
            input inputPhone2
        }
    }
}

def installed() {
    log.debug("installed()")
    initialize()
}

def updated() {
    log.debug("updated()")
    unschedule()
    unsubscribe()
    initialize()
}

private def initialize() {
    //Estado inicial de alarma
    state.afuera = false
    state.casa = false
    state.panico = false
    state.desarmado = true
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    //Mapeo y revision de la alarma
    state.alarmaOn = false
    state.alarmaDelay = false
    state.offSwitches = []
    //Mapeo sensores y suscripcion a eventos
    sensores()
    controlRemoto()
    momentarySwitch()
}

//mapeo sensores y suscripcion
private def sensores() {
    log.debug("sensores()")
    state.sensorContacto = []
    state.sensorContacto << [
        idSensor:   null,
        tipoArmado: "Afuera",
    ]
    if (settings.contacto) {
        settings.contacto.each() {
            state.sensorContacto << [
                idSensor:   it.id,
                tipoArmado: settings["type_${it.id}"] ?: "Casa",
            ]
        }
        subscribe(settings.contacto, "contact.open", onContacto)
        state.sensorContacto.each() {
            log.debug ("Instalacion Exitosa Sensor Contacto ${it.idSensor} / ${it.tipoArmado}")    
        }
    }
    state.sensorMovimiento = []
    state.sensorMovimiento << [
        idSensor:   null,
        tipoArmado: "Casa",
    ]
    if (settings.movimiento) {
        settings.movimiento.each() {
            state.sensorMovimiento << [
                idSensor:   it.id,
                tipoArmado: settings["type_${it.id}"] ?: "Afuera",
            ]
        }
        subscribe(settings.movimiento, "motion.active", onMovimiento)
        state.sensorMovimiento.each() {
            log.debug ("Instalacion Exitosa Sensor Movimiento ${it.idSensor} / ${it.tipoArmado}")    
        }
    }
}
// Control remoto por default define botones (1) Afuera, (2) Casa, (3) Desactivar, (4) Panico
private def controlRemoto() {
    if (settings.remoto) {
        subscribe(settings.remoto, "button", onControlRemoto)
    }
}
//Nombre boton momentario debe ser mismo que funciones definidas
//Tiene que estar en ingles para Alexa (Away, Home & Panic). No se permite desactivacion por Alexa.
private def momentarySwitch() {
    log.debug("switchSimulado()")
    if (settings.momentaryAfuera) {
        subscribe(settings.momentaryAfuera,"switch.on",onMomentary)
    }
    if (settings.momentaryCasa) {
        subscribe(settings.momentaryCasa,"switch.on",onMomentary)
    }
    if (settings.momentaryDesactivar) {
        subscribe(settings.momentaryDesactivar,"switch.on",onMomentary)
    }
    if (settings.momentaryPanico) {
        subscribe(settings.momentaryPanico,"switch.on",onMomentary)
    }
}

def onContacto(evt) {
    log.debug("Evento ${evt.displayName}")
    def contactoOk = state.sensorContacto.find() {it.idSensor == evt.deviceId}
    if (!contactoOk) {
        log.warn ("No se encuentra el dispositivo de contacto ${evt.deviceId}")
        return
    }
    if((contactoOk.tipoArmado == "Afuera" && state.afuera) || (contactoOk.tipoArmado == "Casa" && state.afuera)
    || (contactoOk.tipoArmado == "Casa" && state.casa)) {
        if (contactoOk.idSensor == settings.puertaPrincipal.id) {
            log.debug("Se detecto apertura de puerta principal ${settings.puertaPrincipal.displayName}... Proceso en ${settings.dealyPuerta}")
            //no esta haciendo el delay
            runIn(settings.delayPuerta, activarAlarma(evt.displayName))
        } else {
            activarAlarma(evt.displayName)    
        }
    }
}

def onMovimiento(evt) {
    log.debug("Evento ${evt.displayName} / ${evt.deviceId}")
    def movimientoOk = state.sensorMovimiento.find() {it.idSensor == evt.deviceId}
    if (!movimientoOk) {
        log.warn ("No se encuentra el dispositivo de movimiento ${evt.deviceId}")
        return
    }
    if((movimientoOk.tipoArmado == "Afuera" && state.afuera) || (movimientoOk.tipoArmado == "Casa" && state.afuera)
    || (movimientoOk.tipoArmado == "Casa" && state.casa)) {
        activarAlarma(evt.displayName)    
    }
}

def onControlRemoto(evt) {
    log.debug("onControlRemoto")
    if (!evt.data) {
        return
    }
    def slurper = new JsonSlurper()
    def data = slurper.parseText(evt.data)
    def button = data.buttonNumber?.toInteger()
    if (button) {
        log.debug("Boton ${button} fue ${evt.value}")
        //Nombre en ingles para integracion con Alexa
        if (button == 1) {
            away()
        } else if (button==2) {
            home()
        } else if (button==3) {
            disarm()
        } else if (button==4) {
            panic()
        }
        
    }
}
//Nombre Switch Momentario debe ser mismo que funciones definidas
// away, home, disarm & panic (en ingles para uso con Amazon Echo)
def onMomentary(evt) {
    "${evt.displayName}"()
}

private def away() {
    log.debug("Preparando Armado Afuera")
    if (revisarContacto() && !atomicState.afuera && !atomicState.alarmaOn && !state.alarmaDelay){
        //Siempre se arma con delay
        runIn(settings.delayPuerta, armadoAlarma(true))
        state.alarmaDelay = true
    } 
}
private def home() {
    log.debug("Preparando Armado Casa")
    if (revisarContacto() && !atomicState.casa && !atomicState.alarmaOn && !state.alarmaDelay){
        armadoAlarma(false)
    } 
}
private def disarm() {
    log.debug("Preparando Desarmado")
    if (!atomicState.desarmado){
        desactivarAlarma()
    } 
}
private def panic() {
    log.debug("Activando Panico")
    if (!atomicState.panico && !state.alarmaOn){
        activarPanico()
    } 
}

private def activarAlarma(nombreDispositivo) {
    state.alarmaOn = true
    settings.sirena*.strobe()
    settings.camaras*.take()
    def lucesOn = settings.luces?.findAll {it?.latestValue("switch").contains("off")}
    if (lucesOn) {
        lucesOn*.on()
        state.offLuces = lucesOn.collect {it.id}
    }
    def msg = "Alarma en ${location.name}! - ${nombreDispositivo}"
    log.debug(msg)
    mySendPush(msg)
}

private def activarPanico() {
    state.afuera = false
    state.casa = false
    state.desarmado = false
    state.panico = true
    state.alarmaOn = true
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    settings.sirena*.strobe()
    settings.camaras*.take()
    def lucesOn = settings.luces?.findAll {it?.latestValue("switch").contains("off")}
    if (lucesOn) {
        lucesOn*.on()
        state.offLuces = lucesOn.collect {it.id}
    }
    def msg = "Boton de Panico en ${location.name}!"
    mySendPush(msg)
    log.debug(msg)
}
private def desactivarAlarma() {
    unschedule()
    state.afuera = false
    state.casa = false
    state.desarmado = true
    state.panico = false
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    state.alarmaOn = false
    state.alarmaDelay = false
    settings.sirena*.off()
    def lucesOff = state.offLuces
    if (lucesOff) {
        settings.luces?.each() {
            if (lucesOff.contains(it.id)) {
                it.off()
            }
        }
        state.offLuces = []
    }
    def msg = "Desactivando Alarma en ${location.name}!"
    mySendPush(msg)
    log.debug(msg)
}
//Armado Afuera = true y Armado Casa = false
private def armadoAlarma(tipo){
    state.desarmado = false
    state.panico = false
    if (tipo){
        state.afuera = true
        state.casa = false
        state.alarmaDelay = false
        mySendPush("Armado Afuera en ${location.name}")
        log.debug("Armado Afuera en ${location.name}")
    } else {
        state.afuera = false
        state.casa = true
        mySendPush("Armado Casa en ${location.name}")
        log.debug("Armado Casa en ${location.name}")
    }
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
}

private def revisarContacto(){
    def algoAbierto = settings.contacto.findAll {it?.latestValue("contact").contains("open")}
    if (algoAbierto.size() > 0) {
        algoAbierto.each() {
            log.debug("${it.displayName} esta abierto, no se puede continuar con proceso armado")
            mySendPush("${it.displayName} esta abierto, no se puede continuar con proceso armado")
            //sendNotificationEvent("${it.displayName} esta abierto, no se puede continuar con proceso armado")
        }
        return false
    }
    return true
}

private def statusAlarma(){
    def statusAlarmaAhora = "No Instalada"
    if(state.afuera) {
        statusAlarmaAhora = "Armada Afuera"
    }
    if(state.casa) {
        statusAlarmaAhora = "Armada Casa"
    }
    if(state.desarmado) {
        statusAlarmaAhora = "Desarmada"
    }
    if(state.panico) {
        statusAlarmaAhora = "Panico"
    }
    return statusAlarmaAhora
}

private def mySendPush(msg) {
    // sendPush puede arrojar un error
    try {
        sendPush(msg)
    } catch (e) {
        log.error e
    }
}
//Via switch voy analizando estado de alarma.
//Solo se usan para revisar estado
//Proceso ineficiente
private def statusAlarma(afueraBool, casaBool, panicoBool, desarmadoBool) {
    if (afueraBool){
        settings.switchAfuera.on()
        settings.switchCasa.off()
        settings.switchPanico.off()
        settings.switchDesactivar.off()
    }
    if (casaBool){
        settings.switchAfuera.off()
        settings.switchCasa.on()
        settings.switchPanico.off()
        settings.switchDesactivar.off()
    }
    if (panicoBool){
        settings.switchAfuera.off()
        settings.switchCasa.off()
        settings.switchPanico.on()
        settings.switchDesactivar.off()
    }
    if (desarmadoBool){
        settings.switchAfuera.off()
        settings.switchCasa.off()
        settings.switchPanico.off()
        settings.switchDesactivar.on()
    }
}
