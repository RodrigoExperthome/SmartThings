/**
 *  Author: Expert Home
 *  Date: 2015-07-15
 */
definition(
    name: "Expert Lights",
    namespace: "ExpertHome",
    author: "Expert Home",
    description: "Controlador de luces",
    category: "Convenience",
    iconUrl: "http://experthome.cl/wp-content/uploads/2015/08/Lightning_20.png",
    iconX2Url: "http://experthome.cl/wp-content/uploads/2015/08/Lightning_20.png"
)

preferences {
    page name: "pageSetupScenarioA"
}

// Definir escenario A
def pageSetupScenarioA() {
    def inputLightsA = [
        name:       "A_switches",
        type:       "capability.switch",
        title:      "Luces...",
        multiple:   true,
        required:   false
    ]
    def inputMotionA = [
        name:       "A_motion",
        type:       "capability.motionSensor",
        title:      "Sensores de movimiento...",
        multiple:   true,
        required:   false
    ]
    def inputContactA = [
        name:       "A_contact",
        type:       "capability.contactSensor",
        title:      "Sensores de contacto...",
        multiple:   true,
        required:   false
    ]
    def inputPresenceA = [
        name:       "A_presence",
        type:       "capability.presenceSensor",
        title:      "Sensores de presencia...",
        multiple:   true,
        required:   false
    ]
    def inputModeA = [
        name:       "A_mode",
        type:       "mode",
        title:      "Solo durante los siguientes modos...",
        multiple:   true,
        required:   false
    ]
    def inputTurnOffA = [
        name:       "A_turnOff",
        type:       "number",
        title:      "Retrasar apagado (minutos)...",
        multiple:   false,
        required:   false
    ]
   def pageProperties = [
        name:       "pageSetupScenarioA",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    return dynamicPage(pageProperties) {
	section([title:"Nombra el escenario", mobileOnly:true]) {
        	label title:"Nombre", required:false
        }
	section("Use los siguientes...") { 	
        	input inputMotionA
		input inputContactA
		input inputPresenceA
     	}
        section("Para controlar...") {   
        	input inputLightsA         
      	}
	section("Ajustes generales") {
        	input inputTurnOffA
       		input inputModeA     
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
	if(A_motion) {
		subscribe(settings.A_motion, "motion", onEventA)
	}
	if(A_contact) {
		subscribe(settings.A_contact, "contact", onEventA)
	}
	if(A_presence) {
		subscribe(settings.A_presence, "presence", onEventA)
	}
}

def onEventA(evt) {
	if (!A_mode || A_mode.contains(location.mode))  {
    	def A_levelOn = A_level as Integer
		if (getInputOk(A_motion, A_contact,A_presence)) {
            log.debug("Movimiento, Contacto o Presencia detectada")           
            if (state.A_timerStart){
            	log.debug("Cancelando proceso de delay, dado que aparecio nuevo evento")
            	unschedule(apagarLuz)
            	state.A_timerStart = false
                state.killedProcess = true
        	} else {
            	if (state.killedProcess) {
                	  
                } else {
                	def offLuces = settings.A_switches.findAll {it?.latestValue("switch").contains("off")}
                    log.debug("Las luces apagadas al iniciar la app son : '${offLuces}'")             	
            		state.lucesOff = offLuces.collect{it.id}
                    settings.A_dimmers?.setLevel(A_levelOn)
                	offLuces*.on()
                }
                
                
            }
      	} else {
        	if (settings.A_turnOff) {
            	log.debug("Activando proceso de delay '${A_turnOff}' minutos ")
            	if (state.killedProcess) {
                	log.debug("Proceso original no terminado... luces a apagar no cambian ")  
                }
                runIn(A_turnOff * 60, "apagarLuz")
                state.A_timerStart = true
                
            } else {
            	log.debug("Apagando luz inmediatamente (no hay delay)")
            	apagarLuz()
         	}
		}
	} else {
    	log.debug("Detectado pero no se activa debido a restricciones de modo")
    }
}

//Procedimientos comunes
private getInputOk(motion, contact, presence) {
	def motionDetected = false
	def contactDetected = false
	def presenceDetected = false
	def result = false

	if (motion) {
		if (motion.latestValue("motion").contains("active")) {
			motionDetected = true
            log.debug("Motion valor = ${motion.latestValue("motion")}")
		}
	}
	if (contact) {
		if (contact.latestValue("contact").contains("open")) {
			contactDetected = true
            log.debug("Contact valor = ${contact.latestValue("contact")}")
		}
	}
	if (presence) {
		if (presence.latestValue("presence").contains("present")) {
			presenceDetected = true
            log.debug("Presence valor = ${presence.latestValue("presence")}")
		}
	}
	result = motionDetected || contactDetected || presenceDetected
	result
}

private apagarLuz() {
	def lucesOn_Off = state.lucesOff
    settings.A_switches.each() {
    	if (lucesOn_Off.contains (it.id)){
        	it.off()
        }
    
    }
    settings.A_dimmers?.setLevel(0)
    state.A_timerStart = false
    state.killedProcess = false
}

