/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hivesys.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author swoorup
 */
public class Configuration {

    private static final Configuration singleton = new Configuration();
    public static String CONFIG_FILE = "hivesystemconfig.xml";
    public static String CONTENT_FOLDER = "hivesystemcontent";

    public void loadConfig() {
        String xmlConfigFile = System.getProperty("user.home") + File.separator + CONFIG_FILE;
        File fConfigXML = new File(xmlConfigFile);

        if (!fConfigXML.exists()) {
            FileWriter fWriter;
            try {
                fWriter = new FileWriter(fConfigXML);

                String newXML
                        = "<?xml version=\"1.0\"?>\n"
                        + "<hivesystemconfig>\n"
                        + "\t<database>\n"
                        + "\t\t<host>localhost</host>\n"
                        + "\t\t<port>3306</port>\n"
                        + "\t\t<username></username>\n"
                        + "\t\t<password></password>\n"
                        + "\t\t<databasename>Hive</databasename>\n"
                        + "\t</database>\n"
                        + "\t<solr>\n"
                        + "\t\t<host>localhost</host>\n"
                        + "\t\t<port>8983</port>\n"
                        + "\t\t<core>hive-solr-schema</core>\n"
                        + "\t</solr>\n"
                        + "\t<contentstore>\n"
                        + "\t\t<path>" + System.getProperty("user.home") + File.separator + CONTENT_FOLDER + "</path>\n"
                        + "\t</contentstore>\n"
                        + "\t<boxview>\n"
                        + "\t\t<apikey>ABCDEFGHIJKLMNOPQRSTUVWXYZ</apikey>\n"
                        + "\t</boxview>\n"
                        + "</hivesystemconfig>\n";

                fWriter.write(newXML);
                fWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Wrote sample configuration file to: " + xmlConfigFile);
            System.out.println("Please set the correct setting and restart the server!!!");
            System.exit(1);
            return;
        }
        loadConfig(xmlConfigFile);
    }

    public void loadConfig(String xmlconfigfile) {
        File fConfigXML = new File(xmlconfigfile);
        if (!fConfigXML.exists()) {
            System.out.println("Configuration file not found!");
            System.exit(1);
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fConfigXML);

            doc.getDocumentElement().normalize();

            String rootElement = doc.getDocumentElement().getNodeName();
            if (!rootElement.equalsIgnoreCase("hivesystemconfig")) {
                throw new ParserConfigurationException("Invalid XML File");
            }

            Element dbNode = (Element) doc.getElementsByTagName("database").item(0);
            Element solrNode = (Element) doc.getElementsByTagName("solr").item(0);
            Element contentstoreNode = (Element) doc.getElementsByTagName("contentstore").item(0);
            Element boxviewNode = (Element) doc.getElementsByTagName("boxview").item(0);

            String dbURL = dbNode.getElementsByTagName("host").item(0).getTextContent() + ":"
                    + dbNode.getElementsByTagName("port").item(0).getTextContent() + "/"
                    + dbNode.getElementsByTagName("databasename").item(0).getTextContent();

            String dbUser = dbNode.getElementsByTagName("username").item(0).getTextContent();
            String dbPassword = dbNode.getElementsByTagName("password").item(0).getTextContent();

            String solrConn = "http://" + solrNode.getElementsByTagName("host").item(0).getTextContent() + ":"
                    + solrNode.getElementsByTagName("port").item(0).getTextContent() + "/solr/"
                    + solrNode.getElementsByTagName("core").item(0).getTextContent();
            
            String contentStore = contentstoreNode.getElementsByTagName("path").item(0).getTextContent();
            
            String boxviewpikey = boxviewNode.getElementsByTagName("apikey").item(0).getTextContent();
            
            System.out.println(
                    "Connecting to database: " + dbURL + 
                    " with username: " + dbUser + " and password: " + dbPassword);
            
            System.out.println(
            "Connecting to solr: " + solrConn);
            
            System.out.println(
                    "Content is stored in: " + contentStore);
            
            System.out.println(
            "Box View API key: " + boxviewpikey);
            
            DatabaseSource.getInstance().setUrl(dbURL);
            DatabaseSource.getInstance().setUser(dbUser);
            DatabaseSource.getInstance().setPassword(dbPassword);
            
            ContentStore.getInstance().setContentdir(contentStore);

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            System.out.println("Error reading configuration file: " + xmlconfigfile);
            System.exit(1);
        }

    }

    public static Configuration getInstance() {
        return singleton;
    }

}