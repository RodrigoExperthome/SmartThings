/**
 *  Autor: Expert Home
 *  Version 2.0
 *  To analyze if app is deprecated by Rule Machine.....
 */
definition(
    name: "Expert Lights",
    namespace: "ExpertHome",
    author: "rodrigo@experthome.cl",
    description: "Controlador de luces",
    category: "Convenience",
    iconUrl: "http://experthome.cl/wp-content/uploads/2015/08/Lightning_20.png",
    iconX2Url: "http://experthome.cl/wp-content/uploads/2015/08/Lightning_20.png"
)

preferences {
    page name: "pageSetup"
}

def pageSetup() {
    def inputLights = [
        name:       "luces",
        type:       "capability.switch",
        title:      "Luces...",
        multiple:   true,
        required:   false
    ]
    def inputMotion = [
        name:       "movimiento",
        type:       "capability.motionSensor",
        title:      "Sensores de movimiento...",
        multiple:   true,
        required:   false
    ]
    def inputContact = [
        name:       "contacto",
        type:       "capability.contactSensor",
        title:      "Sensores de contacto...",
        multiple:   true,
        required:   false
    ]
    def inputPresence = [
        name:       "presencia",
        type:       "capability.presenceSensor",
        title:      "Sensores de presencia...",
        multiple:   true,
        required:   false
    ]
    def inputMode = [
        name:       "modo",
        type:       "mode",
        title:      "Solo durante los siguientes modos...",
        multiple:   true,
        required:   false
    ]
    def inputDelay = [
        name:       "delay",
        type:       "number",
        title:      "Retrasar apagado (minutos)...",
        multiple:   false,
        required:   true
    ]
    def inputEstado = [
        name:       "estadoLuz",
        type:       "bool",
        title:      "Override estado luz?",
        multiple:   false,
        required:   true,
        defaultValue: true
    ]
   def pageProperties = [
        name:       "pageSetup",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    return dynamicPage(pageProperties) {
		section([title:"Nombra el escenario", mobileOnly:true]) {
        	label title:"Nombre", required:false
    	}
		section("Use los siguientes...") { 	
        	input inputMotion
			input inputContact
			input inputPresence
    	}
    	section("Para controlar...") {   
       		input inputLights         
    	}
		section("Ajustes generales") {
        	input inputDelay
       		input inputMode
       		//input inputEstado
        }      
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
	state.lucesOff = []
	state.processNumber = 0
	state.killedProcess = false
	state.timerStart = false
	
	if(movimiento) {
		subscribe(settings.movimiento, "motion", onMovimiento)
	}
	if(contacto) {
		subscribe(settings.contacto, "contact", onContacto)
	}
	if(presencia) {
		subscribe(settings.presencia, "presence.present", onPresencia)
	}
}

def onMovimiento (evt) { onHandler (evt, "movimiento") }
def onContacto (evt) { onHandler (evt, "contacto") }
def onPresencia (evt) { onHandler (evt, "presencia") }
//Revisar codigo para escenarios que existan puertas abiertas antes de la activacion (estaba abierta en modo dia)
//dado que processNumber seria negativo
private def onHandler (evt, sensorType) {
	if (!modo || modo.contains(location.mode))  {
		if (inputOk(evt.device, sensorType)) {
			log.debug ("${sensorType} detectado en ${evt.device}")
			if (state.timerStart) {
            	log.debug("Delay cancelado - evento detectado durante periodo de delay")
            	unschedule(apagarLuz)
            	state.timerStart = false
            	state.processNumber = state.processNumber + 1
            	log.debug("Proceso nro ${state.processNumber} en cola de activacion")
        	} else {
        		if (state.processNumber == 0) {
					log.debug("Primer proceso de activacion")
					state.lucesOff = settings.luces.findAll {it?.latestValue("switch").contains("off")}
            		state.lucesOff*.on()
            		log.debug("Las luces apagadas al iniciar activacion son: ${state.lucesOff}")        
            		state.processNumber = state.processNumber + 1
                	log.debug("Proceso nro ${state.processNumber} en cola de activacion")
					//def offLuces = settings.luces.findAll {it?.latestValue("switch").contains("off")}
            		//log.debug("Las luces apagadas al iniciar activacion son : '${offLuces}'")             	
            		//state.lucesOff = offLuces.collect{it.id}
                	//offLuces*.on()
                	
                	if (sensorType == "presence") {
                		log.debug("Proceso de presencia con delay obligado")   
                    	runIn(delay * 60, "apagarLuz")
                		state.timerStart = true		
                	}
				} else {
					state.processNumber = state.processNumber + 1
					log.debug("Proceso nro ${state.processNumber} en cola de activacion")
				}
        	}
		} else {
			if (state.processNumber == 1) {
				if (settings.delay) {
            		log.debug("Apagando luces en ${delay} minutos ")
                	runIn(delay * 60, "apagarLuz")
                	state.timerStart = true
            	}		
			}
			if (state.processNumber == 0) {
				//Problema con iniciar aplicacion de la rutina con evento negativo, ie,
				//cambio de modo, y ventana abierta.
				log.debug("Proceso Negativo - Seguir depurando programación")
			} else {
				state.processNumber = state.processNumber - 1	
			}
			log.debug("Proceso nro ${state.processNumber} en cola de activacion")
		}
	
	}
}
// Metodo para revisar si evento es apertura/movimiento o cierre/no movimiento
def inputOk (device, sensorType) {
	def result
	switch (sensorType) {
		case "contacto":
			result = "open".equals(device.currentValue("contact"))
			break
		case "movimiento":
			result = "active".equals(device.currentValue("motion"))
			break
		case "presencia":
			result = true
			break
		default:
        	result = false	
	}	
}

private def apagarLuz() {
	log.debug ("Luces a apagar son: ${state.lucesOff}")
    state.lucesOff*.off()
   	state.lucesOff = []
    state.timerStart = false
}

