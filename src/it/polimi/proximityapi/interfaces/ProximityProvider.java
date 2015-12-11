/*
 * Copyright 2015 Luca Baggi, Marco Mezzanotte
 * 
 * This file is part of ADPF.
 *
 *  ADPF is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ADPF is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ADPF.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.polimi.proximityapi.interfaces;

import it.polimi.proximityapi.TechnologyManager;
import it.polimi.proximityapi.DAO.ProximityData;
import it.polimi.proximityapi.DAO.ProximityResult;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;

import android.location.Criteria;
import android.location.LocationManager;


/**
 * Questa interfaccia deve essere implementata dai componenti che forniscono
 * dati sulla proximity al {@link LocationManager}
 * @author marco
 *
 */
public interface ProximityProvider {
	
	
	/**
	 * Metodo chiamato per l'inizializzazione delle classi "della tecnologia" e di tutti gli oggetti
	 * necessari, tipo aspettare il binding del {@link BeaconConsumer} da parte del
	 * metodo {@link BeaconManager#bind(BeaconConsumer)} oppure creare e inizializzare un oggetto {@link Criteria}
	 * da parte del {@link LocationManager}
	 */
	public void init();
	

	/**
	 * Stoppa e dealloca tutte le risorse utilizzate dall'applicazione
	 */
	public void destroy();
	
	
	/**
	 * Metodo che permette di iniziare il sensing della proximity <br><br><br>
	 * <b>ESEMPIO</b><br>
	 * Nel caso del {@link LocationManager} questo metodo chiamera' {@link LocationManager}#requestLocationUpdates(...) <br>
	 * Nel caso del {@link BeaconManager} chiamerà {@link BeaconManager#startMonitoringBeaconsInRegion(org.altbeacon.beacon.Region)}
	 */
	public void start();
	
	/**
	 * Metodo che permette di "fermare" il sensing da parte della tecnologia che implementa l'interfaccia
	 */
	public void stop();
	
	/*
	 * 
	 * METODI PER LA GESTIONE DELLE "RICHIESTE DI PROXIMITY"
	 */
	
	/**
	 * Metodo che "notifica" la presenza di una nuova richiesta di monitoraggio della proximity
	 * fatta al {@link TechnologyManager}, in modo che il {@link ProximityProvider} possa reagire alla richiesta
	 * (ad es. nel caso BLE aggiungere la nuova BLERegion da monitorare e salvarsi la entityID del {@link ProximityData}
	 * per "costruire" correttamente la {@link ProximityResult} per il {@link TechnologyManager} <br> 
	 * (vedi {@link PassiveTechnologyListener} implmentata dal {@link TechnologyManager})
 	 *
	 */
	public void onNewProximityRequest(ProximityData data);
	
	/**
	 * Permette al {@link ProximityProvider} di fare il cleanup dei {@link ProximityData} di cui non è più necessario avere la proximity
	 * 
	 */
	public void onRemoveProximityRequest(ProximityData data);
	

}
