/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asascience.ncsos.util;

import java.io.FileReader;
import java.util.HashSet;

/**
 *
 * @author SCowan
 */
public final class VocabDefinitions {
    
    private static final String CF_PARAMETERS = "resources/cf_parameters.txt";
    private static HashSet<String> cfSet;
    private static org.slf4j.Logger _log = org.slf4j.LoggerFactory.getLogger(VocabDefinitions.class);
    
    private VocabDefinitions() {}
    
    /**
     * Determines the necessary term for the parameter.
     * @param param the name of the parameter to look for, expected lowercase w/ "_"
     * @return if in CF table: http://mmissw.org/ont/cf/parameter/param else http://mmisw.org/ont/ioos/parameter/param
     */
    public static String GetDefinitionForParameter(String param) {
        if (cfSet == null)
            CreateCFSet();
        
        if (cfSet.contains(param))
            return "http://mmisw.org/ont/cf/parameter/" + param;
        
        // default
        return "http://mmisw.org/ont/ioos/parameter/" + param;
    }
    
    private static void CreateCFSet() {
        try {
            FileReader fin = new FileReader(CF_PARAMETERS);
            cfSet = new HashSet<String>();
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1];
            while(fin.read(buffer) > 0) {
                if (buffer[0] == ';') {
                    cfSet.add(builder.toString());
                    builder.setLength(0);
                } else {
                    builder.append(buffer[0]);
                }
            }
        } catch (Exception ex) {
            _log.error(ex.toString());
        }
    }
    
}
