/**
 *  Expert Alarm
 *
 *  Version 1.0.0 (8/12/2015)
 *  Inspired in SmartAlarm by statusbits, but recoded to simplify logics, and enable 
 *  better integration with Android Tasker Keypad
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
    name: "Alarma ExpertHome",
    namespace: "Experthome",
    author: "rodrigo@experthome.cl",
    description: "Sistema de alarma integrado a Experthome.cl. Permite monitoreo de casa, " +
                 "y activación de sirenas, luces, luces, notificación sms y camaras foscam. " +
                 "Incluye integración tasker para keypad android (via botones/switch virtuales) y control remoto.",
    category: "Safety & Security",
    
    iconUrl: "http://statusbits.github.io/icons/SmartAlarm-128.png",
    iconX2Url: "http://statusbits.github.io/icons/SmartAlarm-256.png",
)

preferences {
    page name:"pageStatus"
    page name:"pageSensores" 
    page name:"pageOpcionesSensor" 
    page name:"pageOpcionesActivacion"
    page name:"pageOpcionesAlarma" 
    
}

def pageStatus() {
    // def alarmStatus = "La alarma esta Activada Afuera"
    def alarmStatus = "La Alarma está ${statusAlarma()}"
    def pageProperties = [
        name:       "pageStatus",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    return dynamicPage(pageProperties) {
        section("Estado") {
            paragraph alarmStatus
        }
        section("Setup Menu") {
            href "pageSensores", title:"Selecciona sensores", description:"Toca para abrir"
            href "pageOpcionesSensor", title:"Configura activacion", description:"Toca para abrir"
            href "pageOpcionesActivacion", title:"Opciones de activacion", description:"Toca para abrir"
            href "pageOpcionesAlarma", title:"Opciones de alarma", description:"Toca para abrir"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def pageSensores() {
    log.debug("pageSensores()")
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
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Agrega/remueve sensores...") {
            input inputContact
            input inputMotion
        }
    }
}
def pageOpcionesSensor() {
    log.debug("pageOpcionesSensor()")
    def resumen = 
        "Cada sensor se puede configurar como Afuera o Casa." +
        "El armado Casa considera que puede haber movimiento dentro de la " +
        "casa sin generar una activacion de alarma." +
        "Cuando la alarma se arma en modo Afuera, se activan los sensores Afuera y Casa."
    def pageProperties = [
        name:       "pageOpcionesSensor",
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    def tipoSensor = ["Afuera", "Casa"]
    return dynamicPage(pageProperties) {
        section("Sensores") {
            paragraph resumen
        }
        if (settings.contacto) {
            section("Sensores Contacto") {
                def devices = settings.contacto.sort {it.displayName}
                devices.each() {
                    def devId = it.id
                    def displayName = it.displayName
                    input "type_${devId}", "enum", title:displayName, metadata:[values:tipoSensor], defaultValue:"Afuera"
                }
            }
        }
        if (settings.movimiento) {
            section("Sensores Movimiento") {
                def devices = settings.movimiento.sort {it.displayName}
                devices.each() {
                    def devId = it.id
                    def displayName = it.displayName
                        input "type_${devId}", "enum", title:"${it.displayName}", metadata:[values:tipoSensor],defaultValue:"Casa"
                }
            }
        }
        section("Definir Puerta Principal...") {
            input "inputPuerta","capability.contactSensor", title:"Puerta Principal", multiple:true, required: false
            input "inputDelay", "enum", title:"Retraso en Activacion (seg)", metadata:[values:["30","45","60"]], defaultValue:"30", required: true
        }
    }    
}        
def pageOpcionesActivacion() {
    log.debug("pageOpcionesActivacion()")
    def resumen =
        "Expert Alarm se puede instalar via:" +
        "(i) android keypad (tasker), (ii) control remoto y," +
        "(iii) cambio de modo (solo para activacion Afuera)."
    def resumenRemotos =    
        "Control remoto por default define botones " +
        "(1) Afuera, (2) Casa, (3) Desactivar, (4) Panico"
    def resumenBoton =    
        "Solo para ser usados por botones simulados" + 
        "definidos en ST"
    def resumenAudio =    
        "Opciones de audio"        
    def inputModoAfuera = [
        name:       "modosAfuera",
        type:       "mode",
        title:      "Activa Afuera en estos modos...",
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
    def inputBotonAfuera = [
        name:       "botonAfuera",
        type:       "capability.button",
        title:      "Afuera?",
        multiple:   true,
        required:   false
    ]
    def inputBotonCasa = [
        name:       "botonCasa",
        type:       "capability.button",
        title:      "En Casa?",
        multiple:   true,
        required:   false
    ]
    def inputBotonDesactivar = [
        name:       "botonDesactivar",
        type:       "capability.button",
        title:      "Desactiva?",
        multiple:   true,
        required:   false
    ]
    def inputBotonPanico = [
        name:       "botonPanico",
        type:       "capability.button",
        title:      "Panico?",
        multiple:   true,
        required:   false
    ]
    def pageProperties = [
        name:       "pageOpcionesActivacion",
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Opciones de Armado") {
            paragraph resumen
        }
        section("Modos") {
            input inputModoAfuera
        }
        section("Controles Remotos") {
           paragraph resumenRemotos
           input inputRemotes
        }
        section("Botones Simulados para Keypad") {
           paragraph resumenBoton
           input inputBotonAfuera
           input inputBotonCasa
           input inputBotonDesactivar
           input inputBotonPanico
        }
    }
}
def pageOpcionesAlarma() {
    log.debug("pageOpcionesAlarma()")
    def resumen =
        "Acciones a realizar despues de una activación." +
        "Incluye sirenas, luces, fotos, y notificaciones. " +
        "Notificaciones push (activacion & cambio estado) " +
        "se envian de manera automatica."
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
    def inputPushbulletDevice = [
        name:           "pushbullet",
        type:           "device.pushbullet",
        title:          "Que cuenta pushbullet?",
        multiple:       true,
        required:       false
    ]
    def inputAudioPlayers = [
        name:           "audioPlayer",
        type:           "capability.musicPlayer",
        title:          "Que parlantes?",
        multiple:       true,
        required:       false
    ]
    def inputSpeechTextArmedAway = [
        name:           "speechTextArmedAway",
        type:           "text",
        title:          "Frase Armado Afuera",
        required:       false
    ]
    def inputSpeechTextArmedStay = [
        name:           "speechTextArmedStay",
        type:           "text",
        title:          "Frase Armado En Casa",
        required:       false
    ]
    def inputSpeechTextDisarmed = [
        name:           "speechTextDisarmed",
        type:           "text",
        title:          "Frase Desarmado",
        required:       false
    ]
    def pageProperties = [
        name:       "pageOpcionesAlarma",
        nextPage:   "pageStatus",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Opciones de Alarma") {
            paragraph resumen
        }
        section("Sirenas") {
            input inputSirena
        }
        section("Luces a prender") {
            input inputLuces
        }
        section("Camaras") {
            input inputCamaras
        }
        section("Mensajes de Texto") {
            input inputPhone1
            input inputPhone2
        }
        section("Pushbullet") {
            input inputPushbulletDevice
        }
        section("Audio") {
            input inputAudioPlayers
            input inputSpeechTextArmedAway
            input inputSpeechTextArmedStay
            input inputSpeechTextDisarmed
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
    //Estado de activacion de alarma
    state.afuera = false
    state.casa = false
    state.panico = false
    state.desarmado = true
    //Mapeo de la alarma
    state.alarma = []
    //Mapeo sensores y suscripcion a eventos
    sensores()
    controlRemoto()
    botonSimulado()
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

private def botonSimulado() {
    log.debug("botonSimulado()")
    if (settings.botonAfuera) {
        subscribe(settings.botonAfuera,"button",onActivacion)
    }
    if (settings.botonCasa) {
        subscribe(settings.botonCasa,"button",onActivacion)
    }
    if (settings.botonDesactivar) {
        subscribe(settings.botonDesactivar,"button",onActivacion)
    }
    if (settings.botonPanico) {
        subscribe(settings.botonPanico,"button",onActivacion)
    }
}
//Cuando ocurre un evento de contact.open, reviso 
//que tipo de armado tiene el sensor, y lo comparo con el
//estado de la alarma.
def onContacto(evt) {
    log.debug("Evento ${evt.displayName} / ${evt.deviceId}")
    def contactoOk = state.sensorContacto.find() {it.idSensor == evt.deviceId}
    if (!contactoOk) {
        log.warn ("Cannot find zone for device ${evt.deviceId}")
        return
    }
    if((contactoOk.tipoArmado = "Afuera" && state.afuera) || (contactoOk.tipoArmado = "Casa" && state.afuera)
    || (contactoOk.tipoArmado = "Casa" && state.casa)) {
        log.debug("Activando Alarma ${evt.displayName}")
        activarAlarma()    
    }
}

//Cuando ocurre un evento de motion.presence, reviso 
//que tipo de armado tiene el sensor, y lo comparo con el
//estado de la alarma.
def onMovimiento(evt) {
    log.debug("Evento ${evt.displayName} / ${evt.deviceId}")
    def movimientoOk = state.sensorMovimiento.find() { it.idSensor == evt.deviceId }
    if (!movimientoOk) {
        log.warn ("Cannot find zone for device ${evt.deviceId}")
        return
    }
    if((movimientoOk.tipoArmado == "Afuera" && state.afuera) || (movimientoOk.tipoArmado == "Casa" && state.afuera)
    || (movimientoOk.tipoArmado == "Casa" && state.casa)) {
        log.debug("Activando Alarma ${evt.displayName}")
        activarAlarma()    
    }
}

//Cuando se aprieta un boton del control remoto, 
//ejecutando un cambio? del estado de la alarma.
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
        if (button == 1) {
            armadoAfuera()
        } else if (button==2) {
            armadoCasa()
        } else if (button==3) {
            desarmado()
        } else if (button==4) {
            panico()
        }
        
    }
}

def onActivacion(evt) {
    log.debug("OnACTIVACION")
}    


private def armadoAfuera() {
    log.debug("armadoAfuera")
    atomicState.afuera = true
    atomicState.casa = false
    atomicState.panico = false
    atomicState.desarmado = false
}
private def armadoCasa() {
    log.debug("armadoCasa")
    atomicState.afuera = false
    atomicState.casa = true
    atomicState.panico = false
    atomicState.desarmado = false
}
private def desarmado() {
    atomicState.afuera = false
    atomicState.casa = false
    atomicState.panico = false
    atomicState.desarmado = true    
    log.debug("desarmado")
}
private def panico() {
    atomicState.afuera = false
    atomicState.casa = false
    atomicState.panico = false
    atomicState.desarmado = true    
    log.debug("panico")
}
private def activarAlarma() {
    log.debug("activarAlarma")
}
private def desactivarAlarma() {
}

private def statusAlarma(){
    def statusAlarmaAhora
    if(state.afuera) {
        statusAlarmaAhora = "Armada Afuera"
    }
    if(state.casa) {
        statusAlarmaAhora = "Armada En Casa"
    }
    if(state.desarmado) {
        statusAlarmaAhora = "Desarmada"
    }
    if(state.panico) {
        statusAlarmaAhora = "Panico"
    }
    def alarmaDesinstalada = state.afuera||state.casa||state.desarmado||state.panico
    if (alarmaDesintalada==null) {
        statusAlarmaAhora = "No instalada"
    }
    return statusAlarmaAhora
}
