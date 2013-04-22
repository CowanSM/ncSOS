/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asascience.ncsos.describesen;

import com.asascience.ncsos.cdmclasses.TimeSeries;
import com.asascience.ncsos.cdmclasses.iStationData;
import com.asascience.ncsos.outputformatter.DescribeNetworkFormatter;
import com.asascience.ncsos.outputformatter.DescribeSensorFormatter;
import com.asascience.ncsos.service.SOSBaseRequestHandler;
import com.asascience.ncsos.util.VocabDefinitions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author SCowan
 */
public class SOSDescribeStationV2 extends SOSBaseRequestHandler implements ISOSDescribeSensor {
    
    private final String procedure;
    private final String stationName;
    private final iStationData stationData;
    private final String urlBase;
    
    private final static String DEFAULT_STRING = "unknown";
    private final static String QUERY = "?service=SOS&request=DescribeSensor&version=1.0.0&outputFormat=text/xml;subtype=\"sensorML/1.0.1\"&procedure=";
    
    public SOSDescribeStationV2(NetcdfDataset dataset, String procedure, String serverURL) throws IOException {
        super(dataset);
        
        this.procedure = procedure;
        this.stationName = procedure.substring(procedure.lastIndexOf(":"));
        this.stationData = new TimeSeries(new String[] {this.stationName}, null, null);
        this.urlBase = serverURL;
    }
    
    public void setupOutputDocument(DescribeSensorFormatter output) {
        output.setDescriptionNode(this.getGlobalAttribute("description", "no description"));
        output.setName(this.procedure);
        formatSmlIdentification(output);
        formatSmlClassification(output);
        formatSmlValidTime(output);
        formatSmlServiceMetadata(output);
        formatSmlNetworkProcedures(output);
        formatSmlContacts(output);
        formatSmlHistory(output);
        formatSmlLocation(output);
        formatSmlComponents(output);
    }
    
    public void setupOutputDocument(DescribeNetworkFormatter output) {
    }
    
    private void formatSmlIdentification(DescribeSensorFormatter output) {
        // depending on the number of stations in the dataset, we need to look up the
        // identifiers differently
        // first identify one or multiple stations:
        if (this.stationVariable.getDimensions().size() == 1) {
            Dimension dim = this.stationVariable.getDimension(0);
            if (this.stationVariable.getDataType() == ucar.ma2.DataType.CHAR ||
                    this.stationVariable.getDataType() == ucar.ma2.DataType.STRING || 
                    dim.getLength() < 2) {
                // single station if the dataType is a char or string or the length of the dimension is 1
                formatIdentificationSingleStation(output);
                return;
            }
        }
        // multiple stations
        formatIdentificationMultiStation(output);
    }
    
    private void formatIdentificationSingleStation(DescribeSensorFormatter output) {
        // look for a "platform" global attribute which should give us a variable with the station attributes
        String platform = this.getGlobalAttribute("platform", null);
        ucar.nc2.Variable identVar;
        if (platform != null) {
            identVar = this.getVariableByName(platform);
        } else { 
            // use the station variable
            identVar = this.stationVariable;
        }
        // create each of the identities
        // stationID
        output.addSmlIdentifier("stationID", VocabDefinitions.GetIoosDefinition("stationID"), this.procedure);
        // shortName
        Attribute identAtt = identVar.findAttribute("short_name");
        if (identAtt == null) {
            // put unknown for the value
            output.addSmlIdentifier("shortName", VocabDefinitions.GetIoosDefinition("shortName"), DEFAULT_STRING);
        } else {
            // use the attribute's value
            output.addSmlIdentifier("shortName", VocabDefinitions.GetIoosDefinition("shortName"), identAtt.getStringValue());
        }
        // longName
        identAtt = identVar.findAttribute("long_name");
        if (identAtt == null) {
            output.addSmlIdentifier("longName", VocabDefinitions.GetIoosDefinition("longName"), DEFAULT_STRING);
        } else {
            output.addSmlIdentifier("longName", VocabDefinitions.GetIoosDefinition("longName"), identAtt.getStringValue());
        }
        // wmoid, if it exists
        identAtt = identVar.findAttribute("wmo_code");
        if (identAtt != null) {
            output.addSmlIdentifier("wmoID", VocabDefinitions.GetIoosDefinition("wmoID"), identAtt.getStringValue());
        }
    }
    
    private void formatIdentificationMultiStation(DescribeSensorFormatter output) {
        // stationID
        output.addSmlIdentifier("stationID", VocabDefinitions.GetIoosDefinition("stationID"), this.procedure);
        // shortName
        ucar.nc2.Variable platformVariable = this.getVariableByName("platform_short_name");
        ucar.nc2.Attribute platformAttr = (platformVariable != null) ? platformVariable.findAttribute(this.stationName) : null;
        if (platformAttr != null) {
            output.addSmlIdentifier("shortName", VocabDefinitions.GetIoosDefinition("shortName"), platformAttr.getStringValue());
        } else {
            output.addSmlIdentifier("shortName", VocabDefinitions.GetIoosDefinition("shortName"), DEFAULT_STRING);
        }
        // longName
        platformVariable = this.getVariableByName("platform_long_name");
        platformAttr = (platformVariable != null) ? platformVariable.findAttribute(this.stationName) : null;
        if (platformAttr != null) {
            output.addSmlIdentifier("longName", VocabDefinitions.GetIoosDefinition("longName"), DEFAULT_STRING);
        } else {
            output.addSmlIdentifier("longName", VocabDefinitions.GetIoosDefinition("longName"), DEFAULT_STRING);
        }
        // wmoid, if it exists
        platformVariable = this.getVariableByName("platform_wmo_code");
        platformAttr = (platformVariable != null) ? platformVariable.findAttribute(this.stationName) : null;
        if (platformAttr != null) {
            output.addSmlIdentifier("wmoID", VocabDefinitions.GetIoosDefinition("wmoID"), platformAttr.getStringValue());
        }
    }
    
    private void formatSmlClassification(DescribeSensorFormatter output) {
        // add platformType, operatorSector and publisher classifications (assuming they are global variables
        String value = this.getGlobalAttribute("platform_type", DEFAULT_STRING);
        output.addSmlClassifier("platformType", VocabDefinitions.GetIoosDefinition("platformType"), "platform", value);
        value = this.getGlobalAttribute("operator_sector", DEFAULT_STRING);
        output.addSmlClassifier("operatorSector", VocabDefinitions.GetIoosDefinition("operatorSector"), "sector", value);
        value = this.getGlobalAttribute("publisher", DEFAULT_STRING);
        output.addSmlClassifier("publisher", VocabDefinitions.GetIoosDefinition("publisher"), "organization", value);
        
        // sponsor is optional
        value = this.getGlobalAttribute("sponsor", null);
        if (value != null) {
            output.addSmlClassifier("sponsor", VocabDefinitions.GetIoosDefinition("sponsor"), "organization", value);
        }
        // as is parentNetwork
        value = this.getGlobalAttribute("parent_network", null);
        if (value != null) {
            output.addSmlClassifier("parentNetwork", VocabDefinitions.GetIoosDefinition("parentNetwork"), "organization", value);
        }
    }
    
    private void formatSmlValidTime(DescribeSensorFormatter output) {
        output.setValidTime(this.stationData.getBoundTimeBegin(), this.stationData.getBoundTimeEnd());
    }
    
    private void formatSmlServiceMetadata(DescribeSensorFormatter output) {
        // not 100% sure of what to do here
    }
    
    private void formatSmlNetworkProcedures(DescribeSensorFormatter output) {
        // create the network urn
        String[] urnSplit = this.procedure.split(":");
        String networkUrn = "";
        for (int i=0; i<urnSplit.length-1; i++) {
            if (urnSplit[i].equalsIgnoreCase("station") || urnSplit[i].equalsIgnoreCase("sensor")) {
                networkUrn += "network";
            } else {
                networkUrn += urnSplit[i];
            }
        }
        output.addSmlCapabilities("networkProcedures", "network-all", networkUrn);
    }
    
    private void formatSmlContacts(DescribeSensorFormatter output) {
        // waiting for some Q's to be answered from kyle
    }
    
    private void formatSmlHistory(DescribeSensorFormatter output) {
        // not entirely sure how this should be implemented from dataset info
    }
    
    private void formatSmlLocation(DescribeSensorFormatter output) {
        // get the lat/lon of the station
        // position
        String pos = this.stationData.getLowerLat(0) + " " + this.stationData.getLowerLon(0);
        output.setSmlLocation("http://www.opengis.net/def/crs/EPSG/0/4326", pos);
    }
    
    private void formatSmlComponents(DescribeSensorFormatter output) {
        // create a component for each data variable
        String name, id, sensorUrn, url, description, outputName, definition, uom;
        for (ucar.nc2.VariableSimpleIF var : this.getFeatureDataset().getDataVariables()) {
            name = "Sensor " + var.getFullName();
            id = "sensor-" + var.getFullName();
            sensorUrn = this.procedure.replaceAll(":station:", ":sensor:") + ":" + var.getFullName();
            // describe sensor url
            url = this.urlBase + QUERY + sensorUrn;
            description = var.getDescription();
            output.addSmlComponent(name, id, description, url);
            // set the output for the component
            outputName = getValueFromVariableAttribute(var, "long_name", DEFAULT_STRING);
            definition = VocabDefinitions.GetDefinitionForParameter(getValueFromVariableAttribute(var, "standard_name", DEFAULT_STRING));
            uom = getValueFromVariableAttribute(var, "units", DEFAULT_STRING);
            output.addSmlOuptutToComponent(name, outputName, definition, uom);
        }
    }
    
}
