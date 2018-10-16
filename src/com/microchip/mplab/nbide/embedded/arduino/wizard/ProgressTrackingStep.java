/*
 * Copyright (c) 2017 Microchip Technology Inc. and its subsidiaries (Microchip). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.microchip.mplab.nbide.embedded.arduino.wizard;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;


public class ProgressTrackingStep implements WizardDescriptor.Panel<WizardDescriptor> {

    private final Set<ChangeListener> listeners = new HashSet<>();
    private final ImportWorker importWorker;
    private WizardDescriptor wizardDescriptor;
    private ProgressTrackingPanel view;
    private volatile boolean isDone;

    public ProgressTrackingStep(ImportWorker importWorker) {
        this.importWorker = importWorker;
    }
    
    @Override
    public Component getComponent() {
        if (view == null) {            
            view = new ProgressTrackingPanel();
        }
        return view;
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings(WizardDescriptor wizardDescriptor) {        
        this.wizardDescriptor = wizardDescriptor;
        this.wizardDescriptor.setOptions( new Object[] { WizardDescriptor.FINISH_OPTION } );        
        
        importWorker.addPropertyChangeListener( (PropertyChangeEvent evt) -> {
            if ( "state".equals( evt.getPropertyName() ) && evt.getNewValue() == SwingWorker.StateValue.DONE ) {
                if ( importWorker.hasFailed() ) {                    
                    onImportFailed( importWorker.getException() );
                } else {
                    onImportSuccess();
                }
            }
        });
        importWorker.execute();
    }

    @Override
    public void storeSettings(WizardDescriptor wizardDescriptor) {
        // Ignore
    }

    @Override
    public boolean isValid() {
        return isDone;
    }

    @Override
    public final void addChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);// NOI18N
        }
    }

    
    //**********************************************
    //************** PRIVATE METHODS ***************
    //**********************************************
    private void onImportSuccess() {
        view.onImportSuccess();
        isDone = true;
        fireChangeEvent();
        Timer timer = new Timer(2000, (evt) -> {
            wizardDescriptor.doFinishClick();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void onImportFailed( Exception ex ) {
        view.onImportFailed(ex);
        isDone = true;
        fireChangeEvent();
        Exceptions.printStackTrace(ex);
    }
    
    private void fireChangeEvent() {
        Iterator<ChangeListener> it;
        synchronized (listeners) {
            it = new HashSet<>(listeners).iterator();
        }
        ChangeEvent ev = new ChangeEvent(this);
        while (it.hasNext()) {
            it.next().stateChanged(ev);
        }
    }
    
}
