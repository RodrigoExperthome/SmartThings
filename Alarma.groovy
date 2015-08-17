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
    name: "Expert Alarm",
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
    def alarmStatus = "La alarma esta Activada Afuera"
    //def alarmStatus = "Alarm is ${getAlarmStatus()}"
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
    def resumenSwitch =    
        "Solo para ser usados por switch virtuales"    
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
    def inputSwitchAfuera = [
        name:       "switchAfuera",
        type:       "capability.switch",
        title:      "Afuera?",
        multiple:   true,
        required:   false
    ]
    def inputSwitchEnCasa = [
        name:       "switchEnCasa",
        type:       "capability.switch",
        title:      "En Casa?",
        multiple:   true,
        required:   false
    ]
    def inputSwitchDesactivar = [
        name:       "switchDesactivar",
        type:       "capability.switch",
        title:      "Desactiva?",
        multiple:   true,
        required:   false
    ]
    def inputSwitchPanico = [
        name:       "switchPanico",
        type:       "capability.switch",
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
        section("Switch Virtual") {
           paragraph resumenSwitch
           input inputSwitchAfuera
           input inputSwitchEnCasa
           input inputSwitchDesactivar
           input inputSwitchPanico
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
    def inputModoSirena = [
        name:           "modoSirena",
        type:           "enum",
        metadata:       [values:["Off","Siren","Strobe","Both"]],
        title:          "Que tipo de modo sirena",
        defaultValue:   "Both"
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
        title:          "Which cameras?",
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
    def pageProperties = [
        name:       "pageOpcionesAlarma",
        nextPage:   "pageStatus",
        uninstall:  true
    ]

    return dynamicPage(pageProperties) {
        section("Opciones de Alarma") {
            paragraph resumen
        }
        section("Sirenas") {
            input inputSirena
            input inputModoSirena
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
    //Estado alarma es Desarmado al instalar la SmartApp
    
    //Estado de activacion de alarma
    state.alarma = false
    state.desconectada = false
    
    //Mapeo sensores en state.sensor() y me suscribo a eventos de movimiento y apertura
    sensores()
    //Suscripcion a botones
    controlRemoto()
    //Suscripcion a switch virtual
    switchVirtual()
    
    
    
    
}

//mapeo sensores y suscripcion
private def sensores() {
    log.debug("sensores()")
    state.sensor = []
    state.sensor << [
        deviceId:   null,
        tipoArmado:   "Casa",
    ]
    if (settings.contacto) {
        settings.contacto.each() {
            state.sensor << [
                idSensor:   it.id,
                tipoArmado: settings["type_${it.id}"] ?: "Afuera",
            ]
        }
        subscribe(settings.contacto, "contact.open", onAccion)
    }

    if (settings.movimiento) {
        settings.movimiento.each() {
            state.zones << [
                idSensor:   it.id,
                tipoArmado:   settings["type_${it.id}"] ?: "Casa",
            ]
        }
        subscribe(settings.movimiento, "motion.active", onAccion)
    }
}
// Control remoto por default define botones (1) Afuera, (2) Casa, (3) Desactivar, (4) Panico
private def controlRemoto() {
    log.debug("initButtons()")
    if (state.buttonActions) {
        subscribe(settings.remotes, "button", onButtonEvent)
    }
}

private def switchVirtual() {
    log.debug("switchVirtual()")
    
    if (settings.switchAfuera) {
        suscribe(settings.switchAfuera,"switch",armadoAfuera)
    }
    if (settings.switchEnCasa) {
        suscribe(settings.switchEnCasa,"switch",armadoCasa)
    }
    if (settings.switchDesactivar) {
        suscribe(settings.switchDesactivar,"switch",armadoDesarmado)
    }
    if (settings.switchPanico) {
        suscribe(settings.switchPanico,"switch",armadoPanico)
    }
}


    
private def alarmaOk () {
}
private def armadoAfuera () {
}
private def armadoCasa () {
}
private def armadoDesarmado () {
}
private def armadoPanico () {
}
private def activarAlarma () {
}
private def desactivarAlarma () {
}
