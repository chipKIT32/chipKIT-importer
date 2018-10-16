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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jdesktop.swingx.JXBusyLabel;
import org.openide.util.NbBundle;

class ProgressTrackingPanel extends JPanel {

    ProgressTrackingPanel() {
        setName( NbBundle.getMessage( ProgressTrackingPanel.class, "ProgressTrackingPanel.title" ) );
        setLayout( new GridBagLayout() );
        add( createImportInProgressPane(), new GridBagConstraints() );
        setPreferredSize( new Dimension(662, 432) );
    }
    
    void onImportSuccess() {
        showContents( createImportSuccessfulPane() );
    }
    
    void onImportFailed( Exception cause ) {
        showContents( createImportFailedPane( cause ) );
    }
    
    private void showContents( JComponent c ) {
        removeAll();
        add(c, new GridBagConstraints());
        revalidate();
        repaint();
    }
    
    private JComponent createImportInProgressPane() {
        JLabel infoLabel = new JLabel( NbBundle.getMessage( ProgressTrackingPanel.class, "ProgressTrackingPanel.importInProgressMessage" ) );
        infoLabel.setAlignmentX(0.5f);

        JXBusyLabel busyLabel = new JXBusyLabel( new Dimension(48, 48) );
        busyLabel.setAlignmentX(0.5f);
        busyLabel.setBusy(true);

        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.PAGE_AXIS));
        p1.add(infoLabel);
        p1.add(Box.createVerticalStrut(20));
        p1.add(busyLabel);
        
        return p1;
    }
    
    private JComponent createImportSuccessfulPane() {
        JLabel mainMessageLabel = new JLabel( NbBundle.getMessage( 
            ProgressTrackingPanel.class, "ProgressTrackingPanel.importSuccessfulMessage"
        ));
        mainMessageLabel.setAlignmentX(0.5f);
        
        JLabel wizardWillCloseMessageLabel = new JLabel( NbBundle.getMessage( 
            ProgressTrackingPanel.class, "ProgressTrackingPanel.wizardWillCloseMessage" 
        ) );
        wizardWillCloseMessageLabel.setHorizontalAlignment( JLabel.CENTER );
        wizardWillCloseMessageLabel.setAlignmentX(0.5f);
        
        JPanel p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.PAGE_AXIS));
        p0.add(mainMessageLabel);
        p0.add(Box.createVerticalStrut(10));
        p0.add(wizardWillCloseMessageLabel);
        
        return p0;
    }
    
    private JComponent createImportFailedPane( Exception cause ) {
        JLabel infoLabel = new JLabel( NbBundle.getMessage( ProgressTrackingPanel.class, "ProgressTrackingPanel.importFailedMessage" ));
        infoLabel.setHorizontalAlignment(JLabel.CENTER );
        infoLabel.setBackground(Color.red);
        infoLabel.setOpaque(true);
        infoLabel.setBorder( BorderFactory.createLineBorder(Color.red, 3) );
        
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter( arrayOutputStream );
        cause.printStackTrace( printWriter );
        printWriter.flush();
        String stackTraceText = arrayOutputStream.toString();
        
        JTextArea stackTraceTextArea = new JTextArea( stackTraceText );
        stackTraceTextArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane( stackTraceTextArea );        
        
        JButton copyToClipboardButton = new JButton( NbBundle.getMessage( ProgressTrackingPanel.class, "ProgressTrackingPanel.copyToClipboard" ));
        copyToClipboardButton.addActionListener( (a) -> {
            Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            defaultToolkit.getSystemClipboard().setContents( new StringSelection(stackTraceText), null );
        });
        
        JPanel p1 = new JPanel( new FlowLayout(FlowLayout.TRAILING) );
        p1.add( copyToClipboardButton );
        
        JPanel p2 = new JPanel( new BorderLayout(0, 10) );
        p2.add( infoLabel, BorderLayout.NORTH );
        p2.add( scrollPane, BorderLayout.CENTER );        
        p2.add( p1, BorderLayout.SOUTH );
        p2.setSize( new Dimension(600,400) );
        p2.setMinimumSize( p2.getSize() );
        p2.setPreferredSize( p2.getSize() );
        
        return p2;
    }
    
}