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

package com.microchip.mplab.nbide.embedded.arduino.utils;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

// TODO: Currently unused. Consider removing.
public class CompositePathMatcher implements PathMatcher {

    private final PathMatcher matcher1;
    private final PathMatcher matcher2;

    public CompositePathMatcher(PathMatcher matcher1, PathMatcher matcher2) {
        this.matcher1 = matcher1;
        this.matcher2 = matcher2;
    }
    
    @Override
    public boolean matches(Path path) {
        boolean ret = true;
        if ( matcher1 != null ) {
            ret = matcher1.matches(path);
        }
        if ( ret && matcher2 != null ) {
            ret &= matcher2.matches(path);
        }
        return ret;
    }
    
}
