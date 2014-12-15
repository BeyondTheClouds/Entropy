/**
 *
 * Copyright 2012-2013-2014. The SimGrid Team. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package.
 *
 * This class manages the definition of the different parameters, which are present in the ''config/simulator.properties''
 * files
 *
 * @author: flavien.quesnel@inria.fr
 * @coauthor: adrien.lebre@inria.fr
 */
package entropy.configuration;

import entropy.Entropy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class EntropyProperties extends Properties {

	/**
	 *
	 */
	//private static final long serialVersionUID = -103113318411928500L;

    /**
     *  Default location of the properties file
     */
   	public static final String DEFAULT_PROP_FILE = "config" + File.separator + "entropy.properties";

    /**
     * Singleton
     */
    private static EntropyProperties INSTANCE;
	
	private static EntropyProperties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EntropyProperties();
        }
        return INSTANCE;
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // //// ///// Property keys
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // //// /////
    // //// ///// Keys related to nodes
    // //// /////

    // TODO, remove these keys and leverage the platform file instead.

    /**
     * If the first solution will be selected.
     */
	public final static String FIRST_SOLUTION = "entropy.first_solution";


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Property default values
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//Default values related to nodes
	public final static boolean DEFAULT_FIRST_SOLUTION = false;


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Constructors
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public EntropyProperties(String file){
		super();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			this.load(reader);
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public EntropyProperties(){
		this(DEFAULT_PROP_FILE);
	}
	
	@Override
	public String getProperty(String key){
		String result = super.getProperty(key);
		
		if(result != null)
			return result.trim();
		
		else
			return result;
	}
	
	public int getPropertyAsInt(String key, int defaultValue){
		String value = getProperty(key);
		
		if(value != null)
			return Integer.parseInt(value);
		
		else
			return defaultValue;
	}
	
	public long getPropertyAsLong(String key, long defaultValue){
		String value = getProperty(key);
		
		if(value != null)
			return Long.parseLong(value);
		
		else
			return defaultValue;
	}
	
	public boolean getPropertyAsBoolean(String key, boolean defaultValue){
		String value = getProperty(key);
		
		if(value != null)
			return Boolean.parseBoolean(value);
		
		else
			return defaultValue;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Methods related to getting information about if the first_solution mode is enabled or not.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static boolean isFirstSolutionEnabled(){
		return getInstance().getPropertyAsBoolean(FIRST_SOLUTION, DEFAULT_FIRST_SOLUTION);
	}
	
	public static void main(String[] args){
		System.out.println("first_solution mode enabled: " + EntropyProperties.isFirstSolutionEnabled());
	}



}
