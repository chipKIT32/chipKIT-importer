package com.microchip.mplab.nbide.embedded.arduino.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ArduinoDataSource {
    
    protected static final Pattern TOKEN_PATTERN = Pattern.compile("(\\{[\\.|\\w|\\-|_]*\\})");
    
    protected final ArduinoDataSource parent;
    protected Map <String,String> data;

    public ArduinoDataSource(ArduinoDataSource parent) {
        this( parent, new HashMap<>() );
    }
    
    public ArduinoDataSource(ArduinoDataSource parent, Map <String,String> data) {
        this.parent = parent;
        this.data = data;
    }
    
    public boolean hasParent() {
        return parent != null;
    }
    
    public Optional<String> getValue( String key ) {
        return getValue(key, this, null);
    }
    
    public Optional<String> getValue( String key, Map <String,String> runtimeData ) {
        return getValue(key, this, runtimeData);
    }
    
    public Optional<String> getValue( String key, ArduinoDataSource context, Map <String,String> runtimeData ) {
        return getRawValue(key, context, runtimeData).map( value -> resolveTokens(value, context, runtimeData) );
    }
    
    protected Optional<String> getRawValue( String key, ArduinoDataSource context, Map <String,String> runtimeData ) {
        String value = runtimeData != null ? runtimeData.get(key) : null;        
        if ( value == null ) value = data.get(key);
        
        if ( value != null ) {
            return Optional.of(value);
        } else if ( parent != null ) {
            return parent.getValue(key, context, runtimeData);
        } else {
            return Optional.empty();
        }
    }
    
    public void putValue(String key, String value) {
        data.put(key, value);
    }

    protected String resolveTokens( String value, ArduinoDataSource context, Map <String,String> runtimeData ) {
        String newValue = value;
        Matcher m = TOKEN_PATTERN.matcher( value );
        while ( m.find() ) {
            String tokenWithBraces = m.group(1);
            String token = tokenWithBraces.substring(1, tokenWithBraces.length()-1);
            Optional <String> valueOpt = context.getValue( token, runtimeData );
            if ( valueOpt.isPresent() ) {
                newValue = newValue.replace( tokenWithBraces, valueOpt.get() );
            }
        }
        return newValue;
    }
    
}
