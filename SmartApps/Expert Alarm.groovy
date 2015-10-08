/**
 *  Expert Alarm
 *
 *  Version 2.0.0 (06/Oct/2015)
 *  Considera cambios a menus (nueva UI) e integración con rutinas
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
    description:    "Sistema de alarma integrado a experthome.cl",
    category:       "Safety & Security",
    iconUrl:        "http://experthome.cl/wp-content/uploads/2015/08/Security_14.png",
    iconX2Url:      "http://experthome.cl/wp-content/uploads/2015/08/Security_14.png",
)

preferences {
    page name:"pageInicio"
    page name:"pageArmedAway" 
    page name:"pageArmedStay"
    page name:"pageOpcionesAlarma"
    page name:"pageOpcionesActivacion"
}

def pageInicio() {
    def alarmStatus = "La Alarma está ${statusAlarma()}"
    def pageProperties = [
        name:       "pageInicio",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    return dynamicPage(pageProperties) {
        section("Estado Alarma") {
            paragraph alarmStatus
        }
        section("Opciones") {
            href "pageArmedAway", title:"Armado Afuera", description:"Toca para abrir"
            href "pageArmedStay", title:"Armado Casa", description:"Toca para abrir"
            href "pageOpcionesAlarma", title:"Alerta y Notificaciones", description:"Toca para abrir"
            href "pageOpcionesActivacion", title:"Activación Alarma", description:"Toca para abrir"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}
def pageArmedAway() {
    def resumenArmedAway = 
        "Selección de sensores a usar en Armado Afuera."
    def inputContactAway = [
        name:       "contactoArmedAway",
        type:       "capability.contactSensor",
        title:      "Sensores de puerta/ventana",
        multiple:   true,
        required:   false
    ]
    def inputMotionAway = [
        name:       "movimientoArmedAway",
        type:       "capability.motionSensor",
        title:      "Sensores de movimiento",
        multiple:   true,
        required:   false
    ]
    def pageProperties = [
        name:       "pageArmedAway",
        nextPage:   "pageInicio",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section() {
            paragraph resumenArmedAway
            input inputContactAway
            input inputMotionAway
        }
    }
}
def pageArmedStay() {
    log.debug("pageArmedStay()")
    def resumenArmedStay = 
        "Selección de sensores a usar en Armado Casa."
    def inputContactStay = [
        name:       "contactoArmedStay",
        type:       "capability.contactSensor",
        title:      "Sensores de puerta/ventana",
        multiple:   true,
        required:   false
    ]
    def inputMotionStay = [
        name:       "movimientoArmedStay",
        type:       "capability.motionSensor",
        title:      "Sensores de movimiento",
        multiple:   true,
        required:   false
    ]
    def pageProperties = [
        name:       "pageArmedStay",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section() {
            paragraph resumenArmedStay
            input inputContactStay
            input inputMotionStay
        }
    }
}
def pageOpcionesAlarma() {
    log.debug("pageOpcionesAlarma")
    def inputSirena = [
        name:           "sirena",
        type:           "capability.alarm",
        title:          "Sirenas?",
        multiple:       true,
        required:       false
    ]
    def inputLuces = [
        name:           "luces",
        type:           "capability.switch",
        title:          "Luces?",
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
    def inputPuertaPrincipal = [
        name:       "puertaPrincipal",
        type:       "capability.contactSensor",
        title:      "Puerta Principal",
        multiple:   false,
        required:   false
    ]
    def inputDelay = [
        name:           "delayPuertaPrincipal", 
        type:           "enum", 
        title:          "Retraso en Activacion (seg)", 
        metadata:       [values:["30","45","60"]], 
        defaultValue:   "30",
        required:       false
    ]    
    def pageProperties = [
        name:       "pageOpcionesAlarma",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Alerta con...") {
            input inputSirena
            input inputLuces
            input inputCamaras
        }
        section("Mensajes") {
            input inputPush
            input inputPhone1
            input inputPhone2
        }
        section ("Delay Puerta Principal") {
            input inputPuertaPrincipal
            input inputDelay
        }
    }
}
def pageOpcionesActivacion() {
    log.debug("pageOpcionesActivacion")
    def inputModoAfuera = [
        name:       "modosAfuera",
        type:       "mode",
        title:      "Que modo?",
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
        title:      "En Casa?",
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
    def pageProperties = [
        name:       "pageOpcionesActivacion",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section() {
        }
        section("Activar Armado Afuera") {
            input inputModoAfuera
        }
        section("Controles Remotos") {
            paragraph "(1) Armado Afuera, (2) Armado Casa, (3) Desarmado, y (4) Pánico"
            input inputRemotes
        }
        section("Botones") {
            input inputMomentaryAfuera
            input inputMomentaryCasa
            input inputMomentaryDesactivar
            input inputMomentaryPanico
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
    //Mapeo y revision de la alarma
    state.alarmaOn = false
    state.offSwitches = []
    //Mapeo sensores y suscripcion a eventos
    //log.debug("${statusAlarma()}")
    //sensores()
    //controlRemoto()
    //switchSimulado()
}

/**
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
                tipoArmado: settings["type_${it.id}"] ?: "Afuera",
            ]
        }
        subscribe(settings.contacto, "contact.open", onContacto)
        state.sensorContacto.each() {
            log.debug ("${it.idSensor} / ${it.tipoArmado}")    
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
                tipoArmado: settings["type_${it.id}"] ?: "Casa",
            ]
        }
        subscribe(settings.movimiento, "motion.active", onMovimiento)
        state.sensorMovimiento.each() {
            log.debug ("${it.idSensor} / ${it.tipoArmado}")    
        }
    }
}
// Control remoto por default define botones (1) Afuera, (2) Casa, (3) Desactivar, (4) Panico
private def controlRemoto() {
    if (settings.remoto) {
        subscribe(settings.remoto, "button", onControlRemoto)
    }
}
//Nombre Switch Simulado debe ser mismo que funciones definidas
//Tiene que estar en ingles para Alexa (Away, Home & Panic). No se permite desactivacion por Alexa.
//Alexa podria funcionar con botones simulados como antes.
//Problema es mantener actualizado a Tasker, especialmente en armados fallidos.
private def switchSimulado() {
    log.debug("switchSimulado()")
    if (settings.switchAfuera) {
        subscribe(settings.switchAfuera,"switch.on",onSwitchSimulado)
    }
    if (settings.switchCasa) {
        subscribe(settings.switchCasa,"switch.on",onSwitchSimulado)
    }
    if (settings.switchDesactivar) {
        subscribe(settings.switchDesactivar,"switch.on",onSwitchSimulado)
    }
    if (settings.switchPanico) {
        subscribe(settings.switchPanico,"switch.on",onSwitchSimulado)
    }
}
//Cuando ocurre un evento de contact.open, reviso 
//que tipo de armado tiene el sensor, y lo comparo con el
//estado de la alarma.
//** Falta implementar un delay (inputDelay) para la puerta principal (inputPuerta)
def onContacto(evt) {
    log.debug("Evento ${evt.displayName} / ${evt.deviceId}")
    def contactoOk = state.sensorContacto.find() {it.idSensor == evt.deviceId}
    if (!contactoOk) {
        log.warn ("No se encuentra el dispositivo de contacto ${evt.deviceId}")
        return
    }
    //Pensar en usar atomicState....
    if((contactoOk.tipoArmado = "Afuera" && state.afuera) || (contactoOk.tipoArmado = "Casa" && state.afuera)
    || (contactoOk.tipoArmado = "Afuera" && state.casa)) {
        log.debug("Activando Alarma ${evt.displayName}")
        activarAlarma(evt.displayName)    
    }
}
//Cuando ocurre un evento de motion.presence, reviso 
//que tipo de armado tiene el sensor, y lo comparo con el
//estado de la alarma.
//No existe delay en este caso, dado que siempre tiene que sonar la alarma
def onMovimiento(evt) {
    log.debug("Evento ${evt.displayName} / ${evt.deviceId}")
    def movimientoOk = state.sensorMovimiento.find() {it.idSensor == evt.deviceId}
    if (!movimientoOk) {
        log.warn ("No se encuentra el dispositivo de movimiento ${evt.deviceId}")
        return
    }
    //Pensar en usar atomicState....
    if((movimientoOk.tipoArmado == "Afuera" && state.afuera) || (movimientoOk.tipoArmado == "Casa" && state.afuera)
    || (movimientoOk.tipoArmado == "Afuera" && state.casa)) {
        log.debug("Activando Alarma ${evt.displayName}")
        activarAlarma(evt.displayName)    
    }
}
//Cuando se aprieta un boton del control remoto, 
//se ejecuta un cambio en el estado de la alarma.
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
//Nombre Switch Simulado debe ser mismo que funciones definidas
def onSwitchSimulado(evt) {
    "${evt.displayName}"()
}

private def away() {
    log.debug("Preparando Armado Afuera")
    if (revisarContactos() && !atomicState.afuera && !atomicState.alarmaOn){
        armadoAlarma(true)
    } else {
        
        log.debug("Armado no exitoso")
        
    }
}
private def home() {
    log.debug("Preparando armadoCasa")
    if (revisarContactos() && !atomicState.casa && !atomicState.alarmaOn){
        armadoAlarma(false)
    } 
}
private def disarm() {
    log.debug("Preparando desarmado")
    if (!atomicState.desarmado){
        desactivarAlarma()
    } 
}
private def panic() {
    log.debug("panico")
    log.debug("Alarma ya esta sonando!!!! ${state.alarmaOn}")
    if (!atomicState.panico && !state.alarmaOn){
        activarPanico()
         //mensaje push avisando que es boton de panico!!!
    } 
}
//Falta push y SMS. Pensar como integrar en Tasker.
private def activarAlarma(nombreDispositivo) {
    log.debug("BEE DO BEE DO BEE DO")
    state.alarmaOn = true
    settings.sirena*.strobe()
    settings.camaras*.take()
    def lucesOn = settings.luces?.findAll {it?.latestValue("switch").contains("off")}
    log.debug("lucesOn: ${lucesOn}")
    if (lucesOn) {
        lucesOn*.on()
        state.offLuces = lucesOn.collect {it.id}
    }
    //Implementar mensaje tipo push y SMS. Pensar como realizarlo en Tasker.
    def msg = "Alarma en ${location.name}! - ${nombreDispositivo}"
    log.debug("${msg}")
}

private def activarPanico() {
    log.debug("PANICO!!!!")
    state.alarmaOn = true
    settings.sirena*.strobe()
    settings.camaras*.take()
    def lucesOn = settings.luces?.findAll {it?.latestValue("switch").contains("off")}
    log.debug("lucesOn: ${lucesOn}")
    if (lucesOn) {
        lucesOn*.on()
        state.offLuces = lucesOn.collect {it.id}
    }
    //Implementar mensaje tipo push y SMS. Pensar como realizarlo en Tasker.
    def msg = "Alarma en ${location.name}! - PANICO"
    log.debug("${msg}")
}

private def desactivarAlarma() {
    log.debug("Desarmado exitoso")
    state.afuera = false
    state.casa = false
    state.desarmado = true
    state.panico = false
    
    state.alarmaOn = false
    settings.sirena*.off()
    def lucesOff = state.offLuces
    if (lucesOff) {
        log.debug("lucesOff: ${lucesOff}")
        settings.luces?.each() {
            if (lucesOff.contains(it.id)) {
                it.off()
            }
        }
        state.offLuces = []
    }
    log.debug("Afuera: ${state.afuera}/Casa: ${state.casa}/Desarmada: ${state.desarmado}/Panico: ${state.panico}")
    //Implementar mensaje tipo push y SMS
}
//Armado Afuera = true y Armado Casa = false
private def armadoAlarma(tipo){
    state.desarmado = false
    state.panico = false
    if (tipo){
        state.afuera = true
        state.casa = false
    } else {
        state.afuera = false
        state.casa = true
    }
    //Implementar mensaje tipo push. Pensar como realizarlo en Tasker.
    if (tipo){
        log.debug("Alarma esta armada Afuera")
    } else {
        log.debug("Alarma esta armada Casa")
    }
    log.debug("Afuera: ${state.afuera}/Casa: ${state.casa}/Desarmada: ${state.desarmado}/Panico: ${state.panico}")
}
//Falta mandar un msg explicando razon de porque no se pudo armar la alarma
private def revisarContactos(){
    def puertaAbierta = settings.contacto.findAll {it?.latestValue("contact").contains("open")}
    if (puertaAbierta.size() > 0) {
        puertAbierta.each() {
            log.debug("${it.displayName} esta abierto, no se puede continuar con proceso armado")    
        }
        return false
        //implementar mensaje tipo push. Pensar como realizarlo en Tasker.
    }
    return true
}
*/

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
