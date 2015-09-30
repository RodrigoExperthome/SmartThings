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
	if(movimiento) {
		subscribe(settings.movimiento, "motion", onEvent)
	}
	if(contacto) {
		subscribe(settings.contacto, "contact", onEvent)
	}
	if(presencia) {
		subscribe(settings.presencia, "presence.present", onEvent)
	}
}

def onEvent(evt) {
	if (!modo || modo.contains(location.mode))  {
    	if (getInputOk(movimiento, contacto, presencia)) {
        	log.debug("Movimiento, Contacto o Presencia detectada")           
            if (state.timerStart){
            	log.debug("Cancelando proceso de delay, dado que aparecio nuevo evento")
            	unschedule(apagarLuz)
            	state.timerStart = false
                state.killedProcess = true
        	} else {
            	if (state.killedProcess) {
            	// No hace nada. Implementar en negativo???	  
            	} else {
                def offLuces = settings.luces.findAll {it?.latestValue("switch").contains("off")}
                    log.debug("Las luces apagadas al iniciar la app son : '${offLuces}'")             	
            	    state.lucesOff = offLuces.collect{it.id}
                    offLuces*.on()
                    //Eventos de presencia no tienen cierre, por tanto deben tener delay.
                    if (getPresence(presencia)) {
                    	log.debug("Proceso de presencia con delay obligado")   
                    	runIn(delay * 60, "apagarLuz")
                		state.timerStart = true	
                    }
                }
            }
      	} else {
        	if (settings.delay) {
            	log.debug("Activando proceso de delay '${delay}' minutos ")
            	if (state.killedProcess) {
                	log.debug("Proceso original no terminado... luces a apagar no cambian ")  
                }
                runIn(delay * 60, "apagarLuz")
                state.timerStart = true
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

private getPresence(presence) {
	def presenceDetected = false
	def result = false
	if (presence) {
		if (presence.latestValue("presence").contains("present")) {
			presenceDetected = true
            log.debug("Presence valor = ${presence.latestValue("presence")}")
		}
	}
	result = presenceDetected
	result
}

private apagarLuz() {
	def lucesOn_Off = state.lucesOff
	log.debug ("Luces a apagar son: '${lucesOn_Off}'")
    	settings.luces.each() {
    	if (lucesOn_Off.contains (it.id)){
        	it.off()
        }
    }
    state.timerStart = false
    state.killedProcess = false
}

