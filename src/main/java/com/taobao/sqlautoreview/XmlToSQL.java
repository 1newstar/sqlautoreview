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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;




/*
 * function:��xml�е�sql��������,�����浽���ݿ���
 */
public class XmlToSQL {
	//log4j��־
	private static Logger logger = Logger.getLogger(XmlToSQL.class);
	//SQL MAP FILE id
	int sqlmap_file_id;
	//��Ҫ�����SQL MAP FILE
	String sqlmapfilename;
	//��������
	HashMap<String,String> hash;
	//����Դ����
	IHandleDB wsdb ;
	//���캯��
	public XmlToSQL(IHandleDB iHandleSQLReviewDB,int sqlmap_file_id,String sqlmapfilename){
		 this.sqlmap_file_id = sqlmap_file_id;
		 this.sqlmapfilename = sqlmapfilename;
		 this.hash = new HashMap<String,String>();
		 this.wsdb=iHandleSQLReviewDB;
	}
	//���캯��
	public XmlToSQL()
	{
		HandleXMLConf sqlmapfileconf=new HandleXMLConf("sqlmapfile.xml");
		this.sqlmap_file_id=sqlmapfileconf.getSQLMapFileID();
		this.sqlmapfilename=sqlmapfileconf.getSQLMapFileName();
		this.hash = new HashMap<String,String>();
		this.wsdb=new HandleSQLReviewDB();
	}
	
	//������ʽ������SQL
	public static String delByPattern(String str) {
	      Pattern p = Pattern.compile(" {2,}");
	      Matcher m = p.matcher(str);
	      String result = m.replaceAll(" ");
	      return result;
	   }
	
	public static String formatSql(String unFormatSql) {
	      String newSql = unFormatSql.trim().replace("\n", " ")
	            .replace("\t", " ").replace("\r", " ").replace("\f", " ");
	    
	      return delByPattern(newSql);
	   }
	   
	   

	/**
	 * ����XML�ļ���Ϊ�ĵ�����
	 *  
	 */
	private Document loadXml(String path) throws DocumentException, FileNotFoundException {
		//ע������ʹ�õ���FileInptStream��������FileReader
		InputStream input = new FileInputStream(path);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(input);
		return doc;
	}
	
	
	/*
	 * �ڶ���xml������,�õ�������SQL���
	 * 1.����<![CDATA[<=]]>
	 * 2.����include
	 * 3.����prepend
	 */
	private String getRealSQL(Element sqlElement)
	{
		String sql_xml= sqlElement.asXML();
		//����
		int sql_length=sql_xml.length();
		//���շ��ص�SQL
		String last_sql="";
		//>�ĵ�ַ
		int addr_right_kuohao;
		//prepend�ĵ�ַ
		int addr_prepend;
		//<>�е����ַ���
		String sub_sql_xml;
		//���õ�ID
		String refid;
		//��Խ�Ĳ���
		int skip_step=0;
		for(int i=0;i<sql_length;i++)
		{
			if(skip_step>0)
			{
				skip_step--;
				continue;
			}
			if(sql_xml.substring(i,i+1).equals("<")==true)
			{
				addr_right_kuohao=sql_xml.indexOf(">", i);
				sub_sql_xml=sql_xml.substring(i, addr_right_kuohao);
				//���������Ҫ���¼���������λ��
				if(sub_sql_xml.indexOf("![CDATA[")>0)
				{
					addr_right_kuohao=sql_xml.indexOf(">", sql_xml.indexOf("]]", i));
					sub_sql_xml=sql_xml.substring(i, addr_right_kuohao);
				}
				//��ǰֻ�������µ����ֱ�ǩ,������ǩȫ�����˵� 
				if(sub_sql_xml.indexOf("include")>0 && sub_sql_xml.indexOf("refid")>0)
				{
					//include
					refid=sub_sql_xml.substring(sub_sql_xml.indexOf("\"")+1, sub_sql_xml.lastIndexOf("\"")).trim();
					String refsql=hash.get(refid);
					if(refsql==null){
						if(refid.indexOf(".")>0 && (hash.get(refid.substring(refid.indexOf(".")+1))) != null)
						{
							refsql=hash.get(refid.substring(refid.indexOf(".")+1));
							last_sql=last_sql+refsql;
							logger.warn("����SQL�����˷ǳ�������,���Դ���ɹ�.");
						}else {
							logger.error("����SQL�������޷�����.");
						}
					}else{
						last_sql=last_sql+refsql;
					}
				}
				else if(sub_sql_xml.indexOf("![CDATA[")>0)
				{
					//CDATA
					last_sql=last_sql+sub_sql_xml.substring(sub_sql_xml.indexOf("![CDATA[")+8, sub_sql_xml.indexOf("]]"));
				}
				else if(sub_sql_xml.indexOf("prepend")>0)
				{
					//prepend
					addr_prepend=sub_sql_xml.indexOf("prepend");
					int addr_first_yinhao=sub_sql_xml.indexOf("\"", addr_prepend);
					int addr_last_yinhao=sub_sql_xml.indexOf("\"", addr_first_yinhao+1);
					last_sql=last_sql+sub_sql_xml.substring(addr_first_yinhao+1, addr_last_yinhao);	
				}
				//������Խ�Ĳ���
				skip_step=sub_sql_xml.length();
			}
			else {
				last_sql=last_sql+sql_xml.substring(i, i+1);
			}
		}//end for
		
		return last_sql;
	}
	
	/*
	 * �������õ�SQL,��Ҫfollow���µ�xml�淶
	 * <sql id=>
	 * �����п��ܳ������õ�SQLͳһ���浽һ��hashmap��
	 */
	@SuppressWarnings("unchecked")
	private void dealInclude(Element root)
	{
		String refid;
		String refsql;
		Element sqlElement;
		for( Iterator<Element> iterator = root.elementIterator();iterator.hasNext();)
		{
		    sqlElement=iterator.next();
		    if(sqlElement.getName().equals("sql") && sqlElement.attributeValue("id") !=null)
		    {
		    	refid=sqlElement.attributeValue("id");
				refsql=getRealSQL(sqlElement);
				hash.put(refid, refsql);
		    }
		    else {
				continue;
			}
		}
	}
	
	/*
	 * ȡ����һ������,�������ʺ�Ŀո��ֹͣ
	 */
    private String getNextToken(String str,int from_addr)
    {
    	String token="";
    	//������ȫ���
    	if(str==null || str.length()<from_addr){
    		return null;
    	}
    	//�ո�
    	while(from_addr<str.length() && str.substring(from_addr, from_addr+1).equals(" "))
    	{
    		from_addr++;
    	}
    	//����˳�����
    	if(from_addr>str.length()){
    		return null;
    	}
    	//token
    	while(from_addr<str.length() && str.substring(from_addr, from_addr+1).equals(" ")==false)
    	{
    		token=token+str.substring(from_addr, from_addr+1);
    		from_addr++;
    	}
    	
    	return token;
    }
    
	/*
	 * ��SQL����Ԥ����,�Գ�������,���й���
	 */
	private String preDealSql(String sqlString) 
	{
		int addr_where;
		int addr_first_and;
		if(sqlString==null) return null;
		sqlString=sqlString.toLowerCase();
		//��where��ĵ�һ��andɾ��
		addr_where=sqlString.indexOf(" where ");
		if(addr_where>0 && addr_where+7<sqlString.length()){
			if(getNextToken(sqlString, addr_where+7).equals("and"))
			{
				//where���һ���ʳ�����and
				addr_first_and=sqlString.indexOf("and", addr_where+7);
				sqlString=sqlString.substring(0, addr_first_and)+sqlString.substring(addr_first_and+3);
			}
		}
		
		sqlString=sqlString.replace("&gt;", ">");
		sqlString=sqlString.replace("&lt;", "<");
		return sqlString;
	}
	/**
	 * ��SQL MAP�ļ�,������SQL���
	 *  	
	 * @throws DocumentException
	 */
	private void readSqlMap() throws DocumentException {
		Element root;
		if(sqlmapfilename==null) return;     
		try{
            Document dom = loadXml(sqlmapfilename);
            root = dom.getRootElement();
            if(root == null){ 
            	logger.error("can not find sql map file xml root node,sqlautoreview program exit.");
            	return;
            }
		}
        catch(FileNotFoundException e)
        {
        	logger.error("the sql-map-file don't exist,please check the path.");
        	return;
        }
            
            //�ȱ���һ�����õ� SQL
            dealInclude(root);
            //Element���
    		Element sqlElement = null;
    		//ѭ������
    		int max_loop_count=0; 
    		//���洦������SQL
    		String real_sql; 
    		//���濪����д��SQLע��
    		String commentString=""; 
    		//�־û������ݿ�Ķ���

    		for (int i=0; i < root.nodeCount(); i++)
    		{
    			//ѭ����������,����100000��,ǿ���˳�ѭ��
    			max_loop_count++;
    		    if(max_loop_count>100000)
    		    {
    		    	logger.error("the sql map file has more than 100000 sqls.Sql reveiw exit. Please check the sql map file.");
    		    	break;
    		    }
    			Node node=root.node(i);
    			if(node instanceof Element)
    			{
    		        sqlElement = (Element) node;
    			}
    			else {
					continue;
				}
    			  	
    		    //ֻ��Ҫ��������SQL:select,update,delete,insert
    		    if(sqlElement.getName().equals("select") || sqlElement.getName().equals("update") ||
    		    	sqlElement.getName().equals("delete") || sqlElement.getName().equals("insert"))
    		    {
    		    	real_sql=formatSql(getRealSQL(sqlElement));
    		    	//��SQL����Ԥ����
    		    	real_sql=preDealSql(real_sql);
    		    	logger.info("java class id="+sqlElement.attributeValue("id")+" sql="+real_sql);
                    //����һ�������ַ�,�ֶ�ֵ����ܺ��е�����
                    real_sql=real_sql.replace("'", "''");
                    //����sql_xml
                    String sql_xml= sqlElement.asXML();
                    sql_xml=sql_xml.replace("'", "''");
                    //����SQL MAP�е�comment
                    if(root.node(i-1)!=null && root.node(i-1) instanceof Comment)
                    {
                    	commentString=root.node(i-1).asXML();
                    	commentString=commentString.replace("'", "''");
                    }
                    //�־û������ݿ���
                    wsdb.insertDB(sqlmap_file_id,sqlElement.attributeValue("id"), sql_xml, real_sql,commentString);		
    		    }
    		    commentString="";  
    		}
	}
	
	/*
	 * ǰ�˵��õķ���
	 */
	public void xmlToSqlService() throws DocumentException, IOException 
	{
		    readSqlMap();
	}
	
	/**
	 * @param args
	 * @throws DocumentException
	 * @throws IOException 
	 */
	public static void main(String[] args) throws DocumentException, IOException 
	{
		
		    XmlToSQL xts = new XmlToSQL();
		    if(xts.wsdb.checkConnection()==false){
		    	logger.error("�޷�������sqlreviewdb,���������ļ�.");
		    	return;
		    }
		    logger.info("SQLMAP FILE�����ļ���ʼ");
		    xts.readSqlMap();
		    logger.info("SQLMAP FILE�����ļ�����");
	}
}
