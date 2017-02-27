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

package com.microchip.mplab.nbide.embedded.serialmonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.TooManyListenersException;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

public class SerialMonitorDisplayPane extends JPanel {

    
    private static final Charset MESSAGE_CHARSET = Charset.forName("US-ASCII");
    
    @StaticResource
    private static final String CONFIG_ICON = "com/microchip/mplab/nbide/embedded/serialmonitor/config.png";
    @StaticResource
    private static final String RECONNECT_ICON = "com/microchip/mplab/nbide/embedded/serialmonitor/reconnect.png";
    @StaticResource
    private static final String CLEAR_ICON = "com/microchip/mplab/nbide/embedded/serialmonitor/clear.png";
    
    private SerialPortCommunicator communicator;
    private ActionListener configureActionHandler;
    private JTextPane textPane;    
    private StyledDocument document;
    private Style inputStyle;
    private Style outputStyle;
    private Style notificationStyle;
    private JToggleButton crSwitch;
    private JToggleButton lfSwitch;
    private JTextField inputField;
    
    private AdjustmentListener scrollBarAdjustmentListener;
    private boolean adjustScrollBar = true;
    private int previousScrollBarValue = -1;
    private int previousScrollBarMaximum = -1;
    
    
    public SerialMonitorDisplayPane( SerialPortCommunicator communicator, ActionListener configureActionHandler ) {
        this.communicator = communicator;
        this.configureActionHandler = configureActionHandler;
        
        initComponents();
        try {
            communicator.connect( 
                (reconnected) -> {
                    String formattedMessage = null;
                    if ( reconnected ) {                        
                        String rawMessage = getLocalizedText("reconnectedNotification");
                        formattedMessage = MessageFormat.format( rawMessage, communicator.getConfig().getPortName() );
                    } else {
                        String rawMessage = getLocalizedText("connectedNotification");
                        formattedMessage = MessageFormat.format( rawMessage, communicator.getConfig().getPortName() );                        
                    }
                    printNotificationLine( formattedMessage );
                },
                (is) -> {
                    try {                    
                        byte[] buffer = new byte[is.available()];
                        int n = is.read(buffer);
                        printInput(new String( buffer, 0, n ) );                    
                    } catch (IOException ex) {
                        printNotificationLine( getLocalizedText("disconnectedNotification") );
                        communicator.disconnect();
                        communicator.startScanningForPort();
                    }
                }
            );
            
        } catch (TooManyListenersException | UnsupportedCommOperationException | PortInUseException | NoSuchPortException | IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public boolean isCRSelected() {
        return crSwitch.isSelected();
    }
    
    public void setCRSelected( boolean selected ) {
        crSwitch.setSelected(selected);
    }
    
    public boolean isLFSelected() {
        return lfSwitch.isSelected();
    }
    
    public void setLFSelected( boolean selected ) {
        lfSwitch.setSelected(selected);
    }    
    
    public void clear() {
        SwingUtilities.invokeLater( () -> textPane.setText("") );
    }
    
    public void reconnect() {
        try {
            communicator.reconnect();
        } catch (NoSuchPortException | PortInUseException | IOException | UnsupportedCommOperationException | TooManyListenersException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private void initComponents() {        
        JPanel p1 = new JPanel( new BorderLayout() );
        p1.add( createCenterPane(), BorderLayout.CENTER );
        p1.add( createTopPane(), BorderLayout.NORTH );
                
        setLayout( new BorderLayout() );
        add( p1, BorderLayout.CENTER );
        add( createSidePane(), BorderLayout.WEST );
    }
    
    private JComponent createCenterPane() {
        textPane = new JTextPane();
        textPane.setAutoscrolls( false );
        textPane.getCaret().setVisible(false);
        textPane.setEditable( false );
        textPane.setBackground( Color.BLACK );
        textPane.setForeground( Color.LIGHT_GRAY );
        textPane.addKeyListener( new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char character = e.getKeyChar();
                inputField.setText(""+character);
                inputField.requestFocusInWindow();
            }

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        textPane.addFocusListener( new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });        
        
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        
        inputStyle = textPane.addStyle("input", null);
        outputStyle = textPane.addStyle("output", null);
        notificationStyle = textPane.addStyle("notification", null);
        StyleConstants.setForeground(inputStyle, Color.LIGHT_GRAY);
        StyleConstants.setForeground(outputStyle, Color.GREEN);
        StyleConstants.setForeground(notificationStyle, Color.GRAY);
        StyleConstants.setBold(notificationStyle, true);
        
        document = textPane.getStyledDocument();
        
        scrollBarAdjustmentListener = (e) -> SwingUtilities.invokeLater( () ->  checkScrollBar(e) );
        
        JScrollPane scrollPane = new JScrollPane( textPane );
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );        
        scrollPane.getVerticalScrollBar().addAdjustmentListener( scrollBarAdjustmentListener );
        
        return scrollPane;
    }
    
    private JComponent createTopPane() {
        inputField = new JTextField(40);
        inputField.addActionListener( (e) -> sendMessage() );
        inputField.setMaximumSize( inputField.getPreferredSize() );
        
        JButton sendButton = new JButton( getLocalizedComponentText("sendButton") );
        sendButton.addActionListener( (e) -> sendMessage() );
        
        JPanel pane = new JPanel();
        pane.setLayout( new BoxLayout(pane, BoxLayout.LINE_AXIS) );
        
        crSwitch = new JToggleButton("CR", true);
        lfSwitch = new JToggleButton("LF", true);
        
        pane.add( inputField );
        pane.add( Box.createRigidArea( new Dimension(5, 0) ) );
        pane.add( sendButton );
        pane.add( Box.createGlue() );
        pane.add( crSwitch );
        pane.add( Box.createRigidArea( new Dimension(5, 0) ) );
        pane.add( lfSwitch );        
        
        pane.setBorder( BorderFactory.createEmptyBorder(3, 3, 3, 0) );
        
        return pane;
    }
    
    private JComponent createSidePane() {
        JButton configureButton = new JButton( ImageUtilities.loadImageIcon(CONFIG_ICON, false) );        
        configureButton.setToolTipText( getLocalizedComponentTooltip("configureButton") );
        configureButton.addActionListener(configureActionHandler);
        
        JButton reconnectButton = new JButton( ImageUtilities.loadImageIcon(RECONNECT_ICON, false) );
        reconnectButton.addActionListener( (e) -> reconnect() );
        reconnectButton.setToolTipText( getLocalizedComponentTooltip("reconnectButton") );
        
        JButton clearButton = new JButton( ImageUtilities.loadImageIcon(CLEAR_ICON, false) );
        clearButton.addActionListener( (e) -> clear() );
        clearButton.setToolTipText( getLocalizedComponentTooltip("clearButton") );
        
        JPanel pane = new JPanel();
        pane.setLayout( new BoxLayout(pane, BoxLayout.PAGE_AXIS) );
        pane.add( configureButton );
        pane.add( Box.createRigidArea( new Dimension(0, 3) ) );
        pane.add( reconnectButton );
        pane.add( Box.createRigidArea( new Dimension(0, 3) ) );
        pane.add( clearButton );
        
        pane.setBorder( BorderFactory.createEmptyBorder(3, 3, 3, 3) );
        return pane;
    }
    
    private void sendMessage() {
        try {
            String message = inputField.getText();
            inputField.setText("");
            
            byte[] messageBytes = message.getBytes( MESSAGE_CHARSET );
            OutputStream out = communicator.getOut();
            out.write( messageBytes );
            if ( isLFSelected() ) out.write('\n');
            if ( isCRSelected() ) out.write('\r');            
            out.flush();
            printOutputLine( new String(messageBytes, MESSAGE_CHARSET) );
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private void printOutputLine( String message ) {
        print(message+"\n", outputStyle);
    }
    
    private void printInput( String message ) {
        print(message, inputStyle);
    }
    
    private void printNotificationLine( String message ) {
        print(message+"\n", notificationStyle);
    }
    
    private void print( final String message, final Style style ) {
        SwingUtilities.invokeLater( () -> {
            try {
                document.insertString( document.getLength(), message, style );
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
    }
    
    
    // Adapted from https://tips4java.wordpress.com/2013/03/03/smart-scrolling/
    private void checkScrollBar(AdjustmentEvent e) {
        //  The scroll bar listModel contains information needed to determine
        //  whether the viewport should be repositioned or not.

        JScrollBar scrollBar = (JScrollBar) e.getSource();
        BoundedRangeModel scrollBarModel = scrollBar.getModel();
        int value = scrollBarModel.getValue();
        int extent = scrollBarModel.getExtent();
        int maximum = scrollBarModel.getMaximum();

        boolean valueChanged = previousScrollBarValue != value;
        boolean maximumChanged = previousScrollBarMaximum != maximum;

        //  Check if the user has manually repositioned the scrollbar
        if (valueChanged && !maximumChanged) {
            adjustScrollBar = value + extent >= maximum;
        }

        //  Reset the "value" so we can reposition the viewport and
        //  distinguish between a user scroll and a program scroll.
        //  (ie. valueChanged will be false on a program scroll)
        if (adjustScrollBar) {
            //  Scroll the viewport to the end.
            scrollBar.removeAdjustmentListener(scrollBarAdjustmentListener);
            value = maximum - extent;
            scrollBar.setValue(value);
            scrollBar.addAdjustmentListener(scrollBarAdjustmentListener);
        }

        previousScrollBarValue = value;
        previousScrollBarMaximum = maximum;
    }
    
    private static String getLocalizedComponentText( String componentName ) {
        return NbBundle.getMessage(SerialMonitorDisplayPane.class, "SerialMonitorDisplayPane." + componentName + ".text"); // NOI18N
    }
    
    private static String getLocalizedComponentTooltip( String componentName ) {
        return NbBundle.getMessage(SerialMonitorDisplayPane.class, "SerialMonitorDisplayPane." + componentName + ".tooltip"); // NOI18N
    }
    
    private static String getLocalizedText( String id ) {
        return NbBundle.getMessage(SerialMonitorDisplayPane.class, "SerialMonitorDisplayPane." + id ); // NOI18N
    }
}
