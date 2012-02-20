/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 *
 * Authors:
 *   danchen <danchen@taobao.com>
 *
 */

package com.taobao.sqlautoreview;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import org.apache.log4j.Logger;

/*
 * function:����XML���õ�һ����
 */
public class HandleXMLConf {
	Element root;
	//log4j��־
    private static Logger logger = Logger.getLogger(HandleXMLConf.class);
	
	public HandleXMLConf(String filename)
	{
        //String path = getClass().getResource("/").getPath();
		//String sqlmapfilename="sqlreviewdb.xml";
		//sqlmapfilename=sqlmapfilename.replaceAll("%20", " ");
		
		try{
            Document dom = loadXml(filename);
            root = dom.getRootElement();
            if(root == null){ 
            	logger.error("�޷��ҵ����ݿ������ļ���root�����,�����˳�");
            	return;
            }
		}
        catch(Exception e)
        {
        	logger.error("�޷��ҵ����ݿ�������ļ�"+filename+",�����˳�");
        	return;
        }
	}
	
	public Document loadXml(String path) throws DocumentException, IOException 
	{
		//InputStream input = new FileInputStream(Utils.getResourceAsFile(path));
		InputStream input=Utils.getResourceAsStream(path, "utf-8");
		SAXReader reader = new SAXReader();
		Document doc = reader.read(input);
		return doc;
	}
	
	public String getDbConfigIP()
    {
		String ip="";
        //��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	Element tmp=r.next();
        	if(tmp.getName().equals("ip"))
        	{
        		ip = tmp.getData().toString();
        	}
        }  
        return ip;
    }
	
	public int getDbConfigPort()
	{
		int port=-1;
		//��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	Element tmp=r.next();
        	 if(tmp.getName().equals("port"))
        	 {
        		port = Integer.valueOf(tmp.getData().toString());
        		break;
        	 }
	    }
        return port;
	}
    
	public String getDbConfigDbname()
	{
		String dbname="";
		//��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	Element tmp=r.next();
        	 if(tmp.getName().equals("dbname"))
        	 {
        		dbname = tmp.getData().toString();
        		break;
        	 }
	    }
        return dbname;
	}
	
	public String getDbConfigUser()
	{
		String user="";
		//��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	Element tmp=r.next();
        	 if(tmp.getName().equals("user"))
        	 {
        		user = tmp.getData().toString();
        		break;
        	 }
	    }
        return user;
	}
	
	public String getDbConfigPassword()
	{
		String password="";
		//��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	Element tmp=r.next();
        	 if(tmp.getName().equals("password"))
        	 {
        		password = tmp.getData().toString();
        		break;
        	 }
	    }
        return password;
	}
	
	public int getSQLMapFileID()
	{
		int file_id=-1;
		//��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	 Element tmp=r.next();
        	 if(tmp.getName().equals("file_id"))
        	 {
        		 file_id = Integer.valueOf(tmp.getData().toString());
        		 break;
        	 }
	    }
        return file_id;
	}
	
	public String getSQLMapFileName()
	{
		String file_name="";
		//��ȡ����
        for(Iterator<Element> r = root.elementIterator();r.hasNext();)
        {
        	 Element tmp=r.next();
        	 if(tmp.getName().equals("file_name"))
        	 {
        		 file_name = tmp.getData().toString();
        		 break;
        	 }
	    }
        return file_name;
	}
}
