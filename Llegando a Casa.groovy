/**
 *  Llegando v1.0
 *
 *  Author: Expert Home
 *  Date: 2015-07-15
 *	Based on Lightning Director v2.9.4
 */
definition(
    name: "Llegando a Casa",
    namespace: "ExpertHome",
    author: "Expert Home",
    description: "Permite controlar luces y llaves al momento de llegar a casa",
    category: "Convenience",
    //Colocar iconos ExpertHome disponibles
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
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
    def inputDimmersA = [
        name:       "A_dimmers",
        type:       "capability.switchLevel",
        title:      "Dimmer...",
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
    def inputLevelA = [
        name:       "A_level",
        type:       "enum",
        options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]],
        title:      "Para los dimmer, usa la siguiente intensidad...",
        multiple:   false,
        required:   false
    ]
    def inputTurnOffA = [
        name:       "A_turnOff",
        type:       "number",
        title:      "Retrasar apagado (minutos)...",
        multiple:   false,
        required:   true
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
            input inputPresenceA
     	}
        section("Para controlar los siguientes...") {   
            input inputDimmersA
            input inputLightsA
			//input inputLockA
      	}
		section("Ajustes generales") {
            input inputLevelA
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
	if(A_presence) {
		subscribe(settings.A_presence, "presence.present", onEventA)
	}
}

def onEventA(evt) {
	if (!A_mode || A_mode.contains(location.mode))  {
    	def A_levelOn = A_level as Integer           
        if (state.A_timerStart){
        	log.debug("Cancelando proceso de delay, dado que llego '${evt.displayName}'")
            unschedule(apagarLuz)
            state.A_timerStart = false
            state.killedProcess = true
        } else {
            if (state.killedProcess) {
            } else {
            	log.debug("Esta llegando '${evt.displayName}'")
                def offLuces = settings.A_switches.findAll {it?.latestValue("switch").contains(off)}
                log.debug("Las luces apagadas al iniciar la app son : '${offLuces}'")             	
            	state.lucesOff = offLuces.collect{it.id}
                settings.A_dimmers?.setLevel(A_levelOn)
                offLuces*.on()
        	}
    	}
      	
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
		
	} else {
    	log.debug("Detectado pero no se activa debido a restricciones de modo")
    }
}

//Procedimientos comunes

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
