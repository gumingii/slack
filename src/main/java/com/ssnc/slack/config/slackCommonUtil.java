package com.ssnc.slack.config;

import java.io.FileReader;
import java.util.Properties;

import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.support.ResourcePropertySource;


public class slackCommonUtil {
	
	public String getSigningSecret() throws Exception {
		FileReader resources= new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("signingSecret");
    	return str;
	}
	
	public String getSingleTeamBotToken() throws Exception{
		FileReader resources= new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("singleTeamBotToken");
    	return str;
	}
	
	public String getChannel() throws Exception {
		FileReader resources= new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("channel");
    	return str;
	}
	
	public String getFPMSRestApiIP() throws Exception	{
		FileReader resources= new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("fpms_ip");
    	return str;
	}
	
	public String getFPMSRestApiPort() throws Exception	{
		FileReader resources= new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("fpms_port");

    	return str;
	}
	
	public String getFPMSRestAPIUser () throws Exception	{
		FileReader resources = new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("fpmsapiuser");

    	return str;
	}
	
	public String getFPMSRestAPIUserpw () throws Exception	{
		FileReader resources= new FileReader("restapi.properties"); 
    	Properties properties = new Properties();
    	properties.load(resources);
      
    	String str = properties.getProperty("fpmsapipw");

    	return str;
	}
	
}
	