/**
 *  Expert Alarm
 *
 *  Version 1.0.0 (8/12/2015)
 *  Inspired in SmartAlarm by statusbits, but recoded to simplify logics, and enable 
 *  better integration with ExpertHome alarm system solution (includes Android Tasker Keypad)
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
    page name:"pageSetup" //Seteo general de la app
    page name:"pageSensores" //Sensores a seleccionar
    page name:"pageOpcionesSensor" //Tipo de armado para cada sensor
    page name:"pageOpcionesActivacion" //Formas de armar/desarmar 
    page name:"pageOpcionesAlarma" //Acciones a realizar en caso de activación alarma
    //page name:"pageEstado"
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
        section("Opciones Alarma...") {
            href "pageSensores", title:"Selecciona los sensores", description:"Toca para abrir..."
            href "pageOpcionesSensor", title:"Selecciona tipo de armado para los sensores", description:"Toca para abrir..."
            href "pageOpcionesActivacion", title:"Formas de activar/desactivar la alarma", description:"Toca para abrir..."
            href "pageOpcionesAlarma", title:"Acciones a realizar en caso a de alarma ", description:"Toca para abrir..."
        }
        section([title:"Opciones", mobileOnly:true]) {
            label title:"Asigna un nombre", required:false
        }
    }
}
def pageSensores() {
    log.debug("pageSensores()")
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
    log.debug("pageOpcionesSensor()")
    def resumen = 
        "Cada sensor se puede configurar como Afuera o En Casa. " +
        "El armado En Casa considera que puede haber movimiento dentro de la  " +
        "casa sin generar una activacion de alarma. " +
        "Cuando la alarma se arma en modo Afuera, se activan los sensores Afuera y En Casa."
    def pageProperties = [
        name:       "pageOpcionesSensor",
        nextPage:   "pageSetup",
        uninstall:  false
    ]
    def tipoSensor = ["Afuera", "enCasa"]
    return dynamicPage(pageProperties) {
        section("Opcion Sensor") {
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
def pageOpcionesActivacion() {
    log.debug("pageOpcionesActivacion()")
    def resumen =
        "Expert Alarm se puede instalar via keypad-switch virtuales (android + tasker)," +
        "control remoto y cambio de modo (solo para activacion Afuera)." +
        "Control remoto por default define botones [1:Afuera, 2:enCasa, 3:Desactivar, 4:Panico]"
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
        nextPage:   "pageSetup",
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
           input inputRemotes
        }
        section("Switch Virtual") {
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
        "Acciones a realizar despues de una activación " +
        "Incluye sirenas, luces, fotos, y notificaciones. " +
        "Notificaciones push (activacion & cambio estado) se envian de manera automatica."
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
        nextPage:   "pageSetup",
        uninstall:  false
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
    state.installed = true
}

def updated() {
    log.debug("updated()")
    unschedule()
    unsubscribe()
    initialize()
}

private def initialize() {

    
}
    
