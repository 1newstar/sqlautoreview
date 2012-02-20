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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;



/*
 * sqlreviewdb ���ݿ��д����
 */
public class HandleSQLReviewDB implements IHandleDB{
	    //log4j��־
	    private static Logger logger = Logger.getLogger(HandleSQLReviewDB.class);
	    
		public String IP;
		public int port;
		public String dbname;
		public String user;
		public String password;
		public Connection conn;
		
       //���캯��
       public HandleSQLReviewDB()
       {
    	   HandleXMLConf dbconfig = new HandleXMLConf("sqlreviewdb.xml");
    	   this.IP=dbconfig.getDbConfigIP();
    	   this.port=dbconfig.getDbConfigPort();
    	   this.dbname=dbconfig.getDbConfigDbname();
    	   this.user=dbconfig.getDbConfigUser();
    	   this.password=dbconfig.getDbConfigPassword();
    	   this.conn=getConnection();
       }
       
       //������ݿ�����
       public Connection getConnection()
       {
    	   String JDriver = "com.mysql.jdbc.Driver";
    	   String conURL="jdbc:mysql://"+IP+":"+port+"/"+dbname;
    	   try {
               Class.forName(JDriver);
           }
           catch(ClassNotFoundException cnf_e) {  
        	   // ����Ҳ���������
        	   logger.error("Driver Not Found: ", cnf_e);
           }

           try {
               Connection conn = DriverManager.getConnection(conURL, this.user, this.password);  
               return conn;
           }
           catch(SQLException se)
           {
        	   logger.error("�޷����������ݿ������.", se);
        	   return null;
           } 
       }
       
       //������ӵ���Ч��
	   	public boolean checkConnection() 
       {
	   		if(this.conn==null){
	   			return false;
	   		}else {
	   			return true;
			}	
	   	}
       
	     //����Ĵ�����XmlToSQL�еĴ����ظ�����Ҫ����Ϊ����SQL�п��ܲ�ͨ��XmlToSQL��������ݿ���
	   	 //Ϊ�˱��������parseÿһ��SQL֮ǰ,�ٴζ�ÿ��SQL��ʽ��һ��
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
       //дSQL���ݵ����ݿ���
       public boolean insertDB(int sqlmap_file_id,String java_class_id,String sql_xml,String real_sql,String sql_comment)
       {
    	   try{
    		   sql_comment=new String(sql_comment.getBytes(),"GBK");
    	       String command="insert into xmltosql(sqlmap_file_id,java_class_id,sql_xml,real_sql,sql_comment,gmt_create,gmt_modified,status) "; 
    	       command=command+"values("+sqlmap_file_id+",'"+java_class_id+"','"+sql_xml+"','"+real_sql+"','"+sql_comment+"',"+"now(),"+"now(),"+"0);";
    	       Statement stmt = conn.createStatement();
    	       stmt.execute(command);
               stmt.close();
    	   }
    	   catch(Exception e)
    	   {
    		   logger.error("д�����ݿ����", e);
    		   return false;
    	   }
     
    	   return true;
       }
       
       //�����ݿ����ô���˵�SQL
       public List<SQL_Node> getAllSQL()
       {
    	 
    	List<SQL_Node> list_sql = new LinkedList<SQL_Node>();
       	try
       	{
       	   HandleXMLConf sqlmapfileconf=new HandleXMLConf("sqlmapfile.xml");
    	   int sqlmap_file_id=sqlmapfileconf.getSQLMapFileID();
       	   String command="select id,real_sql from xmltosql where status = 0 and sqlmap_file_id="+sqlmap_file_id;
       	   Statement stmt = conn.createStatement();
   	       stmt.execute(command);
   	       ResultSet rs = stmt.getResultSet();
   	  
   	       while(rs.next())
   	       {
   	    	   SQL_Node snNode= new SQL_Node();
   	    	   snNode.id=rs.getInt("id");
   	    	   snNode.sqlString=rs.getString("real_sql");
   	    	   snNode.sqlString=formatSql(snNode.sqlString);
   	    	   list_sql.add(snNode);
   	       }
   	       
   	       rs.close();
           stmt.close();
           return list_sql;
       	}
           catch(SQLException e)
           {
        	   logger.error("��������˵�SQL����", e);
        	   return null;
           }
       }
       
     //�����ݿ����ô���˵�SQL
       public List<SQL_Node> getAllSQL(int sqlmapfileID)
       {
    	 
    	List<SQL_Node> list_sql = new LinkedList<SQL_Node>();
       	try
       	{
    	   int sqlmap_file_id=sqlmapfileID;
       	   String command="select id,real_sql from xmltosql where status = 0 and sqlmap_file_id="+sqlmap_file_id;
       	   Statement stmt = conn.createStatement();
   	       stmt.execute(command);
   	       ResultSet rs = stmt.getResultSet();
   	  
   	       while(rs.next())
   	       {
   	    	   SQL_Node snNode= new SQL_Node();
   	    	   snNode.id=rs.getInt("id");
   	    	   snNode.sqlString=rs.getString("real_sql");
   	    	   snNode.sqlString=formatSql(snNode.sqlString);
   	    	   list_sql.add(snNode);
   	       }
   	       
   	       rs.close();
           stmt.close();
           return list_sql;
       	}
           catch(SQLException e)
           {
        	   logger.error("��������˵�SQL����", e);
        	   return null;
           }
       }
       
       //���ĵ���SQL�����״̬,�Լ�SQL���
       public int updateSQLStatus(int status,String sql_auto_index,int id,String auto_review_err,String auto_review_tip)
       {
    	   String command="update xmltosql set status="+status+",sql_auto_index='"+sql_auto_index+"',";
    	   command=command+"auto_review_err='"+auto_review_err+"',";
    	   command=command+"auto_review_tip='"+auto_review_tip+"',";
		   command=command+"auto_review_time=now(),gmt_modified=now() where id="+id+";";
    	   try {
				Statement stmt = conn.createStatement();
		   	    stmt.execute(command);
		   	    if(stmt.getUpdateCount()!=1)
		   	    	return -1;
		   	    else {
					return 0;
				}
			} catch (SQLException e) {
				logger.error("ִ��command="+command+"�����쳣", e);
				return -2;
			}
       }
       
       //���ĵ���SQL�����״̬,�Լ�SQL���
       public int updateSQLStatus(int status,String sql_auto_index,int id,String auto_review_err,String auto_review_tip,String tablenames)
       {
    	   String command="update xmltosql set status="+status+",sql_auto_index='"+sql_auto_index+"',";
    	   command=command+"auto_review_err='"+auto_review_err+"',";
    	   command=command+"auto_review_tip='"+auto_review_tip+"',";
    	   command=command+"table_name='"+tablenames+"',";
		   command=command+"auto_review_time=now(),gmt_modified=now() where id="+id+";";
    	   try {
				Statement stmt = conn.createStatement();
		   	    stmt.execute(command);
		   	    if(stmt.getUpdateCount()!=1)
		   	    	return -1;
		   	    else {
					return 0;
				}
			} catch (SQLException e) {
				logger.error("ִ��command="+command+"�����쳣", e);
				return -2;
			}
       }

	@Override
	public List<Index_Node> getAllIndexes() 
	{
		List<Index_Node> list_indexes = new LinkedList<Index_Node>();
       	try
       	{
       	   HandleXMLConf sqlmapfileconf=new HandleXMLConf("sqlmapfile.xml");
    	   int sqlmap_file_id=sqlmapfileconf.getSQLMapFileID();
       	   String command="select table_name,sql_auto_index from xmltosql where status = 1 and sqlmap_file_id="+sqlmap_file_id;
       	   Statement stmt = conn.createStatement();
   	       stmt.execute(command);
   	       ResultSet rs = stmt.getResultSet();
   	  
   	       while(rs.next())
   	       {
   	    	   Index_Node index_Node=new Index_Node();
   	    	   //����SQL���漰�ı���
   	    	   index_Node.table_name=rs.getString("table_name");
   	    	   //����SQL���������Ľű�
   	    	   index_Node.index_name=rs.getString("sql_auto_index");
   	    	   list_indexes.add(index_Node);
   	       }
   	       
   	       rs.close();
           stmt.close();
           return list_indexes;
       	}
           catch(SQLException e)
           {
        	   logger.error("���������ɵ�indexes����", e);
        	   return null;
           }
	}

	/*
	 * �Զ�������ʹ��
	 */
	public void deleteSqlByID(int sqlmap_file_id)
	{
		String command="delete from xmltosql where sqlmap_file_id="+sqlmap_file_id;
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.execute(command);
			int rows=stmt.getUpdateCount();
			stmt.close();
			logger.info(rows+" is deleted.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * �Զ�������ʹ��
	 */
	public void closeConn() {
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public List<Index_Node> getAllIndexes(int sqlmap_file_id) 
	{
		List<Index_Node> list_indexes = new LinkedList<Index_Node>();
       	try
       	{
       	   String command="select table_name,sql_auto_index from xmltosql where status = 1 and sqlmap_file_id="+sqlmap_file_id;
       	   Statement stmt = conn.createStatement();
   	       stmt.execute(command);
   	       ResultSet rs = stmt.getResultSet();
   	  
   	       while(rs.next())
   	       {
   	    	   Index_Node index_Node=new Index_Node();
	    	   //����SQL���漰�ı���,�����ж������,��,�ָ�
	    	   index_Node.table_name=rs.getString("table_name");
	    	   //����SQL���������Ľű�,�����ж��������������,��;�ָ�
	    	   index_Node.index_name=rs.getString("sql_auto_index");
	    	   list_indexes.add(index_Node);
   	       }
   	       
   	       rs.close();
           stmt.close();
           return list_indexes;
       	}
           catch(SQLException e)
           {
        	   logger.error("���������ɵ�indexes����", e);
        	   return null;
           }
	}

	
	/*
	 * ɾ��merge���
	 */
	@Override
	public void deleteMergeResult(int sqlmap_file_id) {
		
		String command="delete from mergeresult where sqlmap_file_id="+sqlmap_file_id;
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.execute(command);
			int rows=stmt.getUpdateCount();
			stmt.close();
			logger.debug("mergeresult sqlmap_file_id "+sqlmap_file_id+rows+" is deleted.");
		} catch (SQLException e) {
			logger.error("some error happen:",e);
		}
	}

	/*
	 * ����merge���
	 */
	@Override
	public void saveMergeResult(int sqlmap_file_id, String tablename,
			String real_tablename, String exist_indexes, String new_indexes,
			String merge_result) {
		String command="insert into mergeresult(sqlmap_file_id,tablename,real_tablename,exist_indexes,new_indexes,merge_result,gmt_create,gmt_modified) values(";
		command=command+sqlmap_file_id+",'"+tablename+"','"+real_tablename+"','"+exist_indexes+"','"+new_indexes+"','"+merge_result+"',now(),now())";
		logger.debug(command);
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.execute(command);
			stmt.close();
		} catch (SQLException e) {
			logger.error("some error happen:",e);
		}
		
	}
}
