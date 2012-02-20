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


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;


/*
 * function: create index for these sqls
 *          
 */

public class CreateIndex {
	//log4j��־
	private static Logger logger = Logger.getLogger(CreateIndex.class);
	//SQL REVIEW DATABASE��������
	IHandleDB wtb;
	//���е�SQL������
	List<SQL_Node> list_sql;
	//Ԫ������ض���
	IMetaData md;
	//Ĭ��Ԫ���ݹ��췽ʽΪ0,������ù���ȫ���Ԫ����
	int metaDataBuildtype=0;
	//�������������еĴ���ͽ���
	String auto_review_error;
	String auto_review_tip;
	
	//ÿ�������е��ƴӴ�С����
	class Column_Card
	{
		String column_name;
		int Cardinality;
		AnalyzeColumnStructure acs;
	}
	
    //�����Ļ����ṹ
    class AnalyzeColumnStructure
    {
    	String column_name; //����
    	String column_type; //�е�����
    	int column_type_score; //���͵÷�
    	String is_null_able;
    	String symbol;   //�����
    	int symbol_score; //���������
    	int Cardinality; //��ֵͬ�ĸ���,����һ������ֵ
    	int Cardinality_score; //�����Ƶĵ÷�
    	int total_score; //�ܷ�,�����������������е�����ֱ�����
        int type; //select �ֶ� 0,where�ֶ�1,group by�ֶ�2,order by�ֶ�3
        boolean exist_index; //�Ƿ�������������
        List<Index_Node> list_index;
        boolean is_order_column; //�Ƿ��������ֶ�
        int is_join_key; //�Ƿ������Ӽ�
        
        
        public AnalyzeColumnStructure()
        {
        	is_order_column=false;
        	column_type_score=0;
        	symbol_score=0;
        	Cardinality_score=0;
        	total_score=0;
        	is_join_key=0;
        }
    }
    
    //�����ĵ���ṹ
    class AnalyzeTableStructure
    {
    	String tablename;
    	//�ֿ�ֱ�
    	String real_tablename;
    	List<AnalyzeColumnStructure> list;
    	
    	public  AnalyzeTableStructure(String tablename) {
    		this.tablename=tablename;
    		this.real_tablename=tablename;
			list = new LinkedList<AnalyzeColumnStructure>();
		}
    }
    
    //�������Ľṹ
    class AnalyzeMTableStructure
    {
    	List<AnalyzeTableStructure> list;
    	public AnalyzeMTableStructure()
    	{
    		list = new LinkedList<AnalyzeTableStructure>();
    	}
    }
    
    //���캯��
	public CreateIndex()
	{
		this.wtb=new HandleSQLReviewDB();
		this.md = new MySQLMetaData();
	}
	
	//ǰ�˵��õĹ��캯��
	public CreateIndex(String IP,int port,String dbname,String user,String password,IHandleDB ihandleSQLReviewDB){
		this.md = new MySQLMetaData(IP,port,dbname,user,password);
		this.wtb=ihandleSQLReviewDB;
	}
	
	//������е�SQL
	public void getAllSQL() throws SQLException
	{
		list_sql=wtb.getAllSQL();
	}
	//������е�SQL
	public void getAllSQL(int sqlmapFileID) throws SQLException
	{
		list_sql=wtb.getAllSQL(sqlmapFileID);
	}
	
	//���������Ԫ����
	public void getMetaData()
	{
	    md.buildDBMetaData();
	}
	
	//����ÿһ��SQL
	public void reviewSQL()
	{
		String sql;
		int sql_id;
		String tablenames="";
		if(metaDataBuildtype != 0)
		{
			//һ����ȫ�⹹��Ԫ����
			getMetaData();
		}
		
		for(Iterator<SQL_Node> r=list_sql.iterator();r.hasNext();)
    	{
			auto_review_error="";
			auto_review_tip="";
			tablenames="";
			SQL_Node sql_node= r.next();
    		sql = sql_node.sqlString;
    		sql_id =  sql_node.id;
    		logger.info("����˵�SQL_ID="+sql_id+",ԭʼSQL TEXTΪ:"+sql);
    		ParseSQL ps = new ParseSQL(sql);
    		ps.sql_dispatch();
    		//���sql parse�Ľ��
    		auto_review_tip=ps.tip;
    		if(checkParseResult(ps.errmsg,ps.sql_type,sql_id)==false){
    			continue;
    		}
    		
    		if(ps.tag==0)
    		{
    			    tablenames=ps.tablename;
    			    //�������Ԫ����
    			    if(fillTableMetaData(ps.tablename,sql_id)==false){
    			    	continue;
    			    }
		    		AnalyzeTableStructure ats = new AnalyzeTableStructure(ps.tablename);
		    		modifyAtsRealTablename(ats,ps.tablename);
		    		try {
		    			//�����ݴ�where�������뵽ats��
		        		LoadWhereDataToACS(ps.whereNode,ats);
		        		if(auto_review_error.length()>0){
		        			wtb.updateSQLStatus(2,"", sql_id,auto_review_error,auto_review_tip,tablenames);
		        			continue;
		        		}
		        		//��ǰ�ǰ�group by��order by columnһ�������ֶ�������
		        		String orderString=contactGroupbyOrderby(ps.groupbycolumn,ps.orderbycolumn);
		        		//�������ֶ�Ԫ����װ��ats
		        		LoadOrderDataToACS(orderString,ats,ps.map_columns);
		        		if(auto_review_error.length()>0){
		        			wtb.updateSQLStatus(2,"", sql_id,auto_review_error,auto_review_tip,tablenames);
		        			continue;
		        		}
		        		//��������ֶεĳ���
		        		auto_review_tip=checkOrderByColumnsLength(orderString,ats);
		        		//������������
		        		String createindexString=ComputeBestIndex(ats);
		        		//������SQL����˽�����浽���ݿ���
		        		wtb.updateSQLStatus(1, createindexString, sql_id,auto_review_error,auto_review_tip,tablenames);
					} catch (Exception e) {
						logger.error("the process of creating index has some errors:", e);
					}
    		}else if(ps.tag==1 && ps.sql_type==3){
    			//���select����parse
    			boolean find_all_table_metadata=true;
    			ParseMutiTableSQL pmts = new ParseMutiTableSQL(sql);
    			pmts.ParseComplexSQL();
    			//������
    			auto_review_error=pmts.errmsg;
    			if(auto_review_error.length()>0){
    				wtb.updateSQLStatus(2, "", sql_id,auto_review_error,auto_review_tip);
    				logger.warn("current sql:"+sql+" parse has some errors:"+auto_review_error);
    				continue;
    			}
    			
    			//������б���
    			for(Iterator<ParseStruct> iter=pmts.list_ParseStruct.iterator();iter.hasNext();)
    			{
    				if(tablenames.equals("")==true){
    					tablenames=iter.next().tablename;
    				}else {
						tablenames=tablenames+","+iter.next().tablename;
					}
    			}
    			
    			//�������SQL���漰�ı������Ԫ����
    			for(Iterator<ParseStruct> iter=pmts.list_ParseStruct.iterator();iter.hasNext();)
    			{
    				ParseStruct tmp_ps=iter.next();
    			    if(fillTableMetaData(tmp_ps.tablename,sql_id)==false){
    			    	find_all_table_metadata=false;
    			    	break;
    			    }
    				
    			}
    			if(find_all_table_metadata==false){
    				continue;
    			}
    			
    			//��������SQL������Ԫ���ݱ������
    			AnalyzeMTableStructure amts=new AnalyzeMTableStructure();
    			String createindexString2="";
    			for(Iterator<ParseStruct> iter2=pmts.list_ParseStruct.iterator();iter2.hasNext();)
    			{
    				ParseStruct tmp_ps2=iter2.next();
    				AnalyzeTableStructure tmp_ats=new AnalyzeTableStructure(tmp_ps2.tablename);
    				modifyAtsRealTablename(tmp_ats,tmp_ps2.tablename);
    				LoadWhereDataToACS(tmp_ps2.whereString,tmp_ats);
    				if(auto_review_error.length()>0){
	        			wtb.updateSQLStatus(2,"", sql_id,auto_review_error,auto_review_tip,tablenames);
	        			continue;
	        		}
    				//��ǰ�ǰ�group by��order by columnһ�������ֶ�������
	        		String orderString2=contactGroupbyOrderby(tmp_ps2.groupbycolumn,tmp_ps2.orderbycolumn);
	        		//�������ֶ�Ԫ����װ��tmp_ats
	        		LoadOrderDataToACS(orderString2,tmp_ats,null);
	        		if(auto_review_error.length()>0){
	        			wtb.updateSQLStatus(2,"", sql_id,auto_review_error,auto_review_tip,tablenames);
	        			continue;
	        		}
	        		//��������ֶεĳ���
	        		String checkorderby=checkOrderByColumnsLength(orderString2,tmp_ats);
	        		if(checkorderby.length()>0){
	        			auto_review_tip=checkorderby;
	        		}
	        		//��ӽ�����
	        		amts.list.add(tmp_ats);
    			}
    			
    			//����������
    			ComputeDriverTable(amts, pmts.list_Table_Relationship);
    			//����ÿ�������������
    			for(int i=0;i<pmts.list_Table_Relationship.size();i++)
    			{
    				AnalyzeTableStructure ats3=getATSByTablename(amts, pmts.list_Table_Relationship.get(i).tablename);
    				String tmp_best_index=ComputeBestIndex(ats3,pmts.list_Table_Relationship.get(i));
    				//ƴ��createindexString2
	        		if(createindexString2.length()==0){
	        			createindexString2=tmp_best_index;
	        		}else {
	        			createindexString2=createindexString2+tmp_best_index;
	        		}
    			}
    			
    			logger.info("muti-table sql all index:"+createindexString2);
    			//������SQL����˽�����浽���ݿ���
        		wtb.updateSQLStatus(1, createindexString2, sql_id,auto_review_error,auto_review_tip,tablenames);
    			
    		}
    	}//end for
	}
	
    /*
     * �ֿ�ֱ�������,��Ҫ�޸�ats�е�real_tablename������ֵ
     */
	private void modifyAtsRealTablename(AnalyzeTableStructure ats,
			String tablename) 
	{
		String real_tablename=md.findMatchTable(tablename);
		if(real_tablename != null && real_tablename.equals(tablename)==false){
			ats.real_tablename=real_tablename;
		}
	}

	/*
	 * ����������,֧��������������
	 * 
	 */
	private int ComputeDriverTable(AnalyzeMTableStructure amts,List<Table_Relationship> list_table_relationship) 
	{
		//����һ���Լ��
		int amts_size=amts.list.size();
		int list_table_relationship_size=list_table_relationship.size();
		if(amts_size != list_table_relationship_size){
			logger.warn("AnalyzeMTableStructure ,List<Table_Relationship> list size() has some difference.");
			return -1;
		}
		if(list_table_relationship_size<2){
			logger.warn("list_table_relationship list size less than 2.");
			return -1;
		}
		
		//����list_table_relationship listͷ��β��Table��card,�м�ı���Ҫ����Card
		AnalyzeTableStructure ats_head=getATSByTablename(amts,list_table_relationship.get(0).tablename);
		AnalyzeTableStructure ats_tail=getATSByTablename(amts,list_table_relationship.get(list_table_relationship_size-1).tablename);
		ComputeTableCard(ats_head,list_table_relationship.get(0));
		ComputeTableCard(ats_tail,list_table_relationship.get(list_table_relationship_size-1));
		logger.debug("list_table_relationship first tablename "+list_table_relationship.get(0).tablename+" table Card:"+list_table_relationship.get(0).Cardinality);
		logger.debug("list_table_relationship last tablename "+list_table_relationship.get(list_table_relationship_size-1).tablename+" table Card:"+list_table_relationship.get(list_table_relationship_size-1).Cardinality);
		if(list_table_relationship.get(0).Cardinality < list_table_relationship.get(list_table_relationship_size-1).Cardinality)
		{
			//��list_table_relationship�е�˳��һ��,���ڵ�һ��λ�õ�������Ϊ������SQL��������
	        reverseListTableRelationship(list_table_relationship);
	        logger.debug("reverse list_table_relationship list.Result is as follows:");
	        logger.debug("list_table_relationship first tablename "+list_table_relationship.get(0).tablename+" table Card:"+list_table_relationship.get(0).Cardinality);
			logger.debug("list_table_relationship last tablename "+list_table_relationship.get(list_table_relationship_size-1).tablename+" table Card:"+list_table_relationship.get(list_table_relationship_size-1).Cardinality);
	        
		}else{
			logger.debug("no need to reverse list_table_relationship list.");
		}
		return 0;
	}
	
	/*
	 * ������ת
	 */
	private void reverseListTableRelationship(List<Table_Relationship> list_table_relationship) 
	{	
		//����һ����ʱ����
		List<Table_Relationship> tmpList=new LinkedList<Table_Relationship>();
		//��ԭ�����е���������,�ȱ��浽����һ��������
		for(int i=list_table_relationship.size()-1;i>=0;i--)
		{
			tmpList.add(list_table_relationship.get(i));
			list_table_relationship.remove(i);
		}
		
		//��ת���Ӽ�,����element������ӽ���������
		for(int j=0;j<tmpList.size();j++)
		{
			String tmp_col=tmpList.get(j).columnname1;
			tmpList.get(j).columnname1=tmpList.get(j).columnname2;
			tmpList.get(j).columnname2=tmp_col;
			list_table_relationship.add(tmpList.get(j));
		}
	}

	/*
	 * ͨ������,���Ҷ�Ӧ��AnalyzeTableStructure
	 */
	private AnalyzeTableStructure getATSByTablename(AnalyzeMTableStructure amts,String tablename) 
	{
		boolean is_find=false;
		AnalyzeTableStructure ats = null;
		for(Iterator<AnalyzeTableStructure> iterator=amts.list.iterator();iterator.hasNext();)
		{
			ats=iterator.next();
			if(ats.tablename.equals(tablename)==true)
			{
				is_find=true;
				break;
			}
		}
		
		if(is_find){
			return ats;
		}else {
			logger.warn("can't find AnalyzeTableStructure by tablename:"+tablename);
			return null;
		}
	}

	/*
	 * ����һ�����Cardinality
	 */
	private int ComputeTableCard(AnalyzeTableStructure ats,Table_Relationship tRelationship) 
	{
		int card=1;
		boolean zero=true;
		String join_columnString="";
		int list_size=ats.list.size();
		
		if(ats.tablename.equals(tRelationship.tablename)==false){
			logger.error("ComputeTableCard��ats��tRelationship������һ��.");
			return -1;
		}
		
		for(int i=0;i<list_size;i++)
		{
			AnalyzeColumnStructure acs=ats.list.get(i);
			if(acs.is_join_key==0)
			{
				//�����������ֶε�Cardinality
				if(acs.is_order_column==false)
				{
					if(acs.symbol.equals("=")==true){
						zero=false;
						card=card*acs.Cardinality;
					}else if(acs.symbol.equals(">")==true){
						zero=false;
						card=card*acs.Cardinality;
					}else if(acs.symbol.equals(">=")==true){
						zero=false;
						card=card*acs.Cardinality;
					}else if(acs.symbol.equals("<")==true){
						zero=false;
						card=card*acs.Cardinality;
					}else if(acs.symbol.equals("<=")==true){
						zero=false;
						card=card*acs.Cardinality;
					}else if(acs.symbol.equals("in")==true){
						zero=false;
						card=card*acs.Cardinality;
					}
				}
			}
			else {
				if(join_columnString.length()>0){
					logger.error("table "+ats.tablename+" has two or more join columns.");
					return -1;
				}else{
				     join_columnString=acs.column_name;
				}
			}
		}
		//��������Ŀ�ı�,�����׳��ִ����,���ֻᵼ�¼���������׼ȷ
		if(card==0){
			logger.warn("table "+ats.tablename+"has no data. cardinality is 0.");
		}
		
		//�����Ӽ���,û���κ�������Cardinality
		if(zero){
			card=0;
		}
		
		tRelationship.Cardinality=card;
		
		//��ȷ����
		return 0;
	}

	//װ�ض��where string��Ԫ����
	//whereString���ݸ�ʽcolumn_name:operator:is_join_key;column_name:operator:is_join_key
	private void LoadWhereDataToACS(String whereString,AnalyzeTableStructure ats) 
	{
		String [] Columns=whereString.split(";");
		for(int i=0;i<Columns.length;i++)
		{
			    String column_name=Columns[i].substring(0, Columns[i].indexOf(":"));
			    String operator=Columns[i].substring(Columns[i].indexOf(":")+1,Columns[i].lastIndexOf(":"));
			    int is_join_key=Integer.valueOf(Columns[i].substring(Columns[i].lastIndexOf(":")+1));
				AnalyzeColumnStructure acs = new AnalyzeColumnStructure();
				acs.symbol=operator;
				//�����������ֵ,�Ⱥŷ�ֵ���,�����ֵ��ʱû����
				if(acs.symbol.equals("="))
					acs.symbol_score = 10;
				else {
					acs.symbol_score = 5;
				}
				acs.column_name=column_name;
				//������һ�е�Ԫ����
				Column_Node cn=md.searchColumnMetaData(ats.tablename, acs.column_name);
				if(cn==null){
					auto_review_error="Table:"+ats.tablename+" Column:"+acs.column_name+" does not exist.";
					logger.warn(auto_review_error);
					return;
				}
				//������������ڵ�����Ԫ����
				acs.list_index = md.searchIndexMetaData(ats.tablename, acs.column_name);
				acs.column_type=cn.column_type;
				//��ֵ��ʱû����
				acs.column_type_score=100;
				acs.is_null_able=cn.is_nullable;
				acs.Cardinality=cn.sample_card;
				//��ֵ��ʱû����
				acs.Cardinality_score=100;
				if(acs.list_index.isEmpty()==true)
					acs.exist_index=false;
				else {
					acs.exist_index=true;
				}
				acs.type=1;
				//��ʾ���Ӽ�
				if(is_join_key==1){
					acs.is_join_key=1;
				}
				
				//���е�������Ϣ��ӵ�table��,׼�������ļ���ʹ��
				if(!checkExistAcsInAts(acs,ats)){
				    ats.list.add(acs);
				}
		}
		
	}

	/*
	 * ����Ƿ��Ѵ��ڵ�acs,ֻ��Ҫ���������Ƿ����
	 */
	private boolean checkExistAcsInAts(AnalyzeColumnStructure acs,
			AnalyzeTableStructure ats) {
		if(acs==null || ats==null){
			logger.warn("checkExistAcsInAts:acs null or ats null.");
			return false;
		}
		for(Iterator<AnalyzeColumnStructure> iterator=ats.list.iterator();iterator.hasNext();)
		{
			if(acs.column_name.equals(iterator.next().column_name)){
				return true;
			}
		}
		return false;
	}

	//���parse�еĴ���,�Լ�һЩsql_type��Ϣ
	private boolean checkParseResult(String errmsg, int sql_type,int sql_id) 
	{
		if(errmsg.length()>0)
		{
			auto_review_error="��SQL���﷨����ʱ�������µĴ���:"+errmsg;
			wtb.updateSQLStatus(2, "", sql_id,auto_review_error,auto_review_tip);
			logger.error("at the process of SQL parse,some errors happens:"+errmsg);
			return false;
		}
		
		if(sql_type==0)
		{
			auto_review_tip="insert���,����Ҫ�������";
			wtb.updateSQLStatus(1, "", sql_id,auto_review_error,auto_review_tip);
			logger.info("insert SQL doesn't need to sql review.");
			return false;
		}
		
		if(sql_type==-1)
		{
			auto_review_error="�޷�ʶ���SQL�������,��ǰ֧��select,insert,update,delete���";
			wtb.updateSQLStatus(2,"", sql_id,auto_review_error,auto_review_tip);
			logger.error("can't recongnize the sql type. now ,support select,insert,update,delete SQL statement");
			return false;
		}
		
		return true;
	}

	/*
	 * ��嵥���Ԫ����
	 */
	private boolean fillTableMetaData(String tablename,int sql_id) 
	{
		//���������ı���,֧�ַֿ�ֱ�
		String real_tablename=md.findMatchTable(tablename);
		if(real_tablename==null)
		{
			 //�����Ŀ�����ݿ����Ƿ���Ĵ���
			 logger.warn(tablename+" Ԫ�����޷�����. because the table doesn't exist.");
		     auto_review_error= "Error:table "+tablename+" does not exist.";
		     wtb.updateSQLStatus(2,"", sql_id,auto_review_error,auto_review_tip);
			 return false;
		}else if(md.checkTableExist(tablename)==false)
		{
			//�����Cache���Ƿ����
			if(metaDataBuildtype==0)
    		{
    		   md.buildTableMetaData(tablename);
    		}
		}
		
		if(real_tablename.equals(tablename)==false)
		{
		   //�ֿ�ֱ�,�����滻tip
			auto_review_tip="Table:"+tablename+"�ֿ�ֱ�,Ԫ���ݼ��ر����滻Ϊ:"+real_tablename;
			logger.info(auto_review_tip);
			wtb.updateSQLStatus(0,"", sql_id,auto_review_error,auto_review_tip);
		}
		
		return true;
	}
	/*
	 * ��group by columns��order by columns�ֶ�ƴ����
	 */
	private String contactGroupbyOrderby(String groupbycolumn,String orderbycolumn) {
		String orderString="";
		if(groupbycolumn.length()>0)
		{
			orderString=groupbycolumn;
		}
		if(orderbycolumn.length()>0)
		{
			if(orderString.length()>0)
			{
				orderString=orderString+","+orderbycolumn;
			}
			else {
				orderString=orderbycolumn;
			}
		}
		
		return orderString;
	}

	/*
	 * ��������ֶεĳ���,����������ֶ�Ϊorder by a,b
	 * �������ֵ���a�����Ͷ���Ϊvarchar(1000),b�Ķ���Ϊvarchar(1000)
	 * ��������ѯ��������ļ�¼���ֺܶ�,��ô��sort_buffer_size��Ҫ����,����ᱨ��
	 */
	private String checkOrderByColumnsLength(String orderbycolumn,AnalyzeTableStructure ats) 
	{
		String column_name;
		String column_type;
		int varchar_length;
		AnalyzeColumnStructure acs;
		String checkError="";
		boolean is_find=false;
		if(orderbycolumn.length()==0) return checkError;
		String[] tmp = orderbycolumn.split(",");
		for(int i=0;i<tmp.length;i++)
		{
			column_name=tmp[i];
			for(Iterator<AnalyzeColumnStructure> r=ats.list.iterator();r.hasNext();)
			{
				acs=r.next();
				if(acs.column_name.equals(column_name))
				{
					column_type=acs.column_type;
					if(column_type.length()>7 && column_type.substring(0, 7).equals("varchar")==true)
					{
						int start=column_type.indexOf("(");
						int end=column_type.indexOf(")");
						varchar_length = Integer.valueOf(column_type.substring(start+1,end));
						if(varchar_length > 200)
						{
							is_find=true;
							logger.warn("order by column:"+column_name+"  column type:"+column_type+" is bigger than varchar(200). there has some danger in sort buffer size.");
						}
					}
					break;
				}
			}
		}
		
		if(is_find==true)
		{
			checkError="order by column type is so big.";
		}
		return checkError;
	}
	
	/*
	 * ���콨�����Ľű�
	 */
	private String BuildCreateIndexScript(String index_columns,AnalyzeTableStructure ats)
	{
		String index_name;
		String createIndexScript="";
		if(index_columns.indexOf(",")>0)
		{
		    index_name="idx_"+ats.tablename+"_"+index_columns.substring(0, index_columns.indexOf(","));
		}
		else {
			index_name="idx_"+ats.tablename+"_"+index_columns;
		}
		
		if(ats.tablename.equals(ats.real_tablename)){
			createIndexScript="create index "+index_name+" on "+ats.tablename+"("+index_columns+");";
		}else{
			//�ֿ�ֱ�
			createIndexScript=ats.tablename+"�ֿ�ֱ�   create index "+index_name+" on "+ats.real_tablename+"("+index_columns+");";
		}
		
		if(index_columns.length()==0)
		{
			        logger.warn("û���κ�where�����ֶ�,����������");
		}
		else {
					logger.info("create index script: "+createIndexScript);
		}	
		
		return createIndexScript;
	}
	
	/*
	 * ���㵥����������
	 */
	private String ComputeBestIndex(AnalyzeTableStructure ats) {
		//��һ�ֹ���
		//����where�����ֶ����Ƿ��������ֶ�,�������ֱ������������
		AnalyzeColumnStructure acs;
		boolean is_find_best_index=false;
		
		for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
		{
			acs=r.next();
			//�������ֶ�
			if(acs.is_order_column==true) continue;
			//�������
			if(acs.list_index != null && acs.symbol.equals("!=")==false)
			{
				for(Iterator<Index_Node> in=acs.list_index.iterator();in.hasNext();)
				{
					Index_Node tmp_iNode=in.next();
					if(tmp_iNode.index_name.equals("PRIMARY") && tmp_iNode.seq_in_index==1)
					{
						logger.info(acs.column_name+"��PRIMARY KEY����,ֱ��ʹ��");
						is_find_best_index=true;
						break;
					}
				}
			}
			
			if(is_find_best_index==true) break;
		}
		//�������������ֶ�,��ֱ���˳�
		if(is_find_best_index==true) return "PRIMARY";
		
		
		//�ڶ��ֹ���
		//���������ֶ����Ƿ���Ψһ���ֶ�,�������ֱ����Ψһ������
		for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
		{
			acs=r.next();
			//�������ֶ�
			if(acs.is_order_column==true) continue;
			//�������
			if(acs.list_index != null && acs.symbol.equals("!=")==false)
			{
				for(Iterator<Index_Node> in=acs.list_index.iterator();in.hasNext();)
				{
					Index_Node tmp_iNode=in.next();
					if(tmp_iNode.non_unique==0 && tmp_iNode.seq_in_index==1)
					{
						logger.info(acs.column_name+"��UNIQUE KEY����,ֱ��ʹ��");
						is_find_best_index=true;
						break;
					}
				}
			}
			
			if(is_find_best_index==true) break;
		}
		//����Ψһ������ǰ�����ֶ�,��ֱ���˳�
		if(is_find_best_index==true) return "UNIQUE KEY";
		
		
		//�����ֹ���,�˱��������,Ψһ������,������������
		//������Ա�SQL����������
		List<Column_Card> list_card_denghao = new ArrayList<Column_Card>();
		List<Column_Card> list_card_no_denghao = new ArrayList<Column_Card>();
		List<Column_Card> list_card_order = new ArrayList<Column_Card>();
		int type; //ȡֵ0,1,2    0����ȺŲ�����column,���������ߵ����ȼ�,1Ϊ�ǵȺ�,2Ϊ�����ֶ�
		type=0;
		SortColumnCard(list_card_denghao,ats,type);
		type=1;
		SortColumnCard(list_card_no_denghao,ats,type);
		type=2;
		SortColumnCard(list_card_order,ats,type);
		
		String index_columns=BuildBTreeIndex(list_card_denghao,list_card_no_denghao,list_card_order);
		String createIndexScript=BuildCreateIndexScript(index_columns,ats);	
		//�������������Ľű�
		return createIndexScript;
	}

	/*
	 * 
	 * ���joinSQL����ÿ���������������
	 */
	private String ComputeBestIndex(AnalyzeTableStructure ats,
			                        Table_Relationship table_Relationship) {
		//��һ�ֹ���
		//����where�����ֶ����Ƿ��������ֶ�,�������ֱ������������
		AnalyzeColumnStructure acs;
		boolean is_find_best_index=false;
		
		for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
		{
			acs=r.next();
			//�������ֶ�
			if(acs.is_order_column==true) continue;
			//�����ֶ�columnname2������Ϊ����
			if(acs.is_join_key==1 && acs.column_name.equals(table_Relationship.columnname2)==true){
				continue;
			}
			//�������
			if(acs.list_index != null 
					&& acs.symbol.equals("!=")==false)
			{
				for(Iterator<Index_Node> in=acs.list_index.iterator();in.hasNext();)
				{
					Index_Node tmp_iNode=in.next();
					if(tmp_iNode.index_name.equals("PRIMARY") && tmp_iNode.seq_in_index==1)
					{
						logger.info(acs.column_name+"��PRIMARY KEY����,ֱ��ʹ��");
						is_find_best_index=true;
						break;
					}
				}
			}
			
			if(is_find_best_index==true) break;
		}//end for
		
		//�������������ֶ�,��ֱ���˳�
		if(is_find_best_index==true) return ats.tablename+" has PRIMARY index;";
		
		
		//�ڶ��ֹ���
		//���������ֶ����Ƿ���Ψһ���ֶ�,�������ֱ����Ψһ������
		for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
		{
			acs=r.next();
			//�������ֶ�
			if(acs.is_order_column==true) continue;
			//�����ֶ�columnname2������Ϊ����
			if(acs.is_join_key==1 && acs.column_name.equals(table_Relationship.columnname2)==true){
				continue;
			}
			//�������
			if(acs.list_index != null && acs.symbol.equals("!=")==false)
			{
				for(Iterator<Index_Node> in=acs.list_index.iterator();in.hasNext();)
				{
					Index_Node tmp_iNode=in.next();
					if(tmp_iNode.non_unique==0 && tmp_iNode.seq_in_index==1)
					{
						logger.info(acs.column_name+"��UNIQUE KEY����,ֱ��ʹ��");
						is_find_best_index=true;
						break;
					}
				}
			}
			
			if(is_find_best_index==true) break;
		}
		//����Ψһ������ǰ�����ֶ�,��ֱ���˳�
		if(is_find_best_index==true) return ats.tablename+" has UNIQUE KEY index;";
		
		
		//�����ֹ���,�˱��������,Ψһ������,������������
		//������Ա�SQL����������
		List<Column_Card> list_card_denghao = new ArrayList<Column_Card>();
		List<Column_Card> list_card_no_denghao = new ArrayList<Column_Card>();
		List<Column_Card> list_card_order = new ArrayList<Column_Card>();
		int type; //ȡֵ0,1,2    0����ȺŲ�����column,���������ߵ����ȼ�,1Ϊ�ǵȺ�,2Ϊ�����ֶ�
		type=0;
		SortColumnCard(list_card_denghao,ats,type,table_Relationship);
		type=1;
		SortColumnCard(list_card_no_denghao,ats,type);
		type=2;
		SortColumnCard(list_card_order,ats,type);
		
		String index_columns=BuildBTreeIndex(list_card_denghao,list_card_no_denghao,list_card_order);
		String createIndexScript=BuildCreateIndexScript(index_columns,ats);	
		//�������������Ľű�
		return createIndexScript;
	}
	/*
	 * ��������������
	 */
	private String BuildBTreeIndex(List<Column_Card> list_card_denghao,
			                      List<Column_Card> list_card_no_denghao,
			                      List<Column_Card> list_card_order) 
	{
		String str="";
		if(list_card_denghao.size()==0 && list_card_no_denghao.size()==0 && list_card_order.size()==0)
		{
			return str;
		}

		//���ΰ������������鴮��������
		//��ֵ�����ֶη������������ֶεĵ�һ��Ӫ
		for(int i=0;i<list_card_denghao.size();i++)
		{
			str=str+","+list_card_denghao.get(i).column_name;
		}
		
		//�����ֶη������������ֶεĵڶ���Ӫ
		for(int k=list_card_order.size()-1;k>=0;k--)
		{
			int addr=str.indexOf(","+list_card_order.get(k).column_name);
			if(addr<0){
				str=str+","+list_card_order.get(k).column_name;
			}
		}
		
	    //�ǵ�ֵ�����ֶη������������ֶεĵ�����Ӫ
		for(int j=0;j<list_card_no_denghao.size();j++)
		{
			if(str.indexOf(","+list_card_no_denghao.get(j).column_name) == -1)
			{
			     str=str+","+list_card_no_denghao.get(j).column_name;
			}
		}
		
		if(str.substring(0, 1).equals(",")==true)
			str=str.substring(1);
		
		return str;
	}

	/*
	 * ���е�Cardinality��������
	 */
	private void SortColumnCard(List<Column_Card> list_card,
			AnalyzeTableStructure ats,int type) {
		// TODO Auto-generated method stub
		AnalyzeColumnStructure acs;
		if(ats.list.isEmpty()==true) return;
		//װ������
		//�ȺŲ���
		if(type==0)
		{
			for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
			{
			    acs=r.next();
			    //�����������ֶε�Cardinality
			    if(acs.is_order_column==true) continue;
			    if(acs.symbol.equals("=")==true)
			    {
				    Column_Card cc = new Column_Card();
				    cc.column_name=acs.column_name;
				    cc.Cardinality=acs.Cardinality;
				    cc.acs=acs;
				    list_card.add(cc);
			    }
			}
		}
		
		//�ǵȺŲ���
		if(type==1)
		{
			for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
			{
			    acs=r.next();
			    //�����������ֶε�Cardinality
			    if(acs.is_order_column==true) continue;
			    if(acs.symbol.equals("=")==false)
			    {
				    Column_Card cc = new Column_Card();
				    cc.column_name=acs.column_name;
				    cc.Cardinality=acs.Cardinality;
				    cc.acs=acs;
				    list_card.add(cc);
			    }
			}
		}
		
		//�����ֶβ���
		if(type==2)
		{
			for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
			{
			    acs=r.next();
			    if(acs.is_order_column==true)
			    {
				    Column_Card cc = new Column_Card();
				    cc.column_name=acs.column_name;
				    cc.Cardinality=acs.Cardinality;
				    cc.acs=acs;
				    list_card.add(cc);
			    }
			}
			
			//�����ֶεĻ�,����Ҫ�Ƚ�Cardinality,ֱ�ӷ���
			return;
		}
		
		//����
		for(int i=0;i<list_card.size();i++)
		{
			for(int j=i+1;j<list_card.size();j++)
			{
				if(list_card.get(i).Cardinality < list_card.get(j).Cardinality)
				{
					//����
					Column_Card tmp_cc = list_card.get(i);
					list_card.set(i, list_card.get(j));
					list_card.set(j, tmp_cc);	
				}
			}
		}
	}

	
	/*
	 * ���е�Cardinality��������
	 */
	private void SortColumnCard(List<Column_Card> list_card,AnalyzeTableStructure ats,
			int type,Table_Relationship table_Relationship) 
	{
		AnalyzeColumnStructure acs;
		if(ats.list.isEmpty()==true) return;
		//װ������
		//�ȺŲ���
		if(type==0)
		{
			for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
			{
			    acs=r.next();
			    //�����������ֶε�Cardinality
			    if(acs.is_order_column==true) continue;
			    //�����ֶ�columnname2������Ϊ����
				if(acs.is_join_key==1 && acs.column_name.equals(table_Relationship.columnname2)==true){
					continue;
				}
			    if(acs.symbol.equals("=")==true)
			    {
				    Column_Card cc = new Column_Card();
				    cc.column_name=acs.column_name;
				    cc.Cardinality=acs.Cardinality;
				    cc.acs=acs;
				    list_card.add(cc);
			    }
			}
		}
		
		//�ǵȺŲ���
		if(type==1)
		{
			for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
			{
			    acs=r.next();
			    //�����������ֶε�Cardinality
			    if(acs.is_order_column==true) continue;
			    if(acs.symbol.equals("=")==false)
			    {
				    Column_Card cc = new Column_Card();
				    cc.column_name=acs.column_name;
				    cc.Cardinality=acs.Cardinality;
				    cc.acs=acs;
				    list_card.add(cc);
			    }
			}
		}
		
		//�����ֶβ���
		if(type==2)
		{
			for(Iterator<AnalyzeColumnStructure> r = ats.list.iterator();r.hasNext();)
			{
			    acs=r.next();
			    if(acs.is_order_column==true)
			    {
				    Column_Card cc = new Column_Card();
				    cc.column_name=acs.column_name;
				    cc.Cardinality=acs.Cardinality;
				    cc.acs=acs;
				    list_card.add(cc);
			    }
			}
			
			//�����ֶεĻ�,����Ҫ�Ƚ�Cardinality,ֱ�ӷ���
			return;
		}
		
		//����
		for(int i=0;i<list_card.size();i++)
		{
			for(int j=i+1;j<list_card.size();j++)
			{
				if(list_card.get(i).Cardinality < list_card.get(j).Cardinality)
				{
					//����
					Column_Card tmp_cc = list_card.get(i);
					list_card.set(i, list_card.get(j));
					list_card.set(j, tmp_cc);	
				}
			}
		}
	}

	/*
	 * װ����������
	 */
	private void LoadOrderDataToACS(String orderbycolumn,
			                        AnalyzeTableStructure ats,Map<String, String> map) {
		
		//���û�������ֶ�,����Ҫ����Ĳ���
		if(orderbycolumn.length()==0) return;
		//ʹ�ü�����Ҫ��Ϊ��ȥ��
		Set<String> set = new HashSet<String>();
		String[] tmp = orderbycolumn.split(",");
		for(int i=0;i<tmp.length;i++)
		{
			set.add(tmp[i]);
		}
		
		String column_name;
		for(Iterator<String> r = set.iterator();r.hasNext();)
		{
			column_name=r.next();
			//Ҫ�ȼ����ats�����Ƿ��Ѵ���,���������,����,�������,ֱ�Ӹ����ֶ�
			checkOrderColumnExist(ats,column_name,map);
		}
	}

	/*
	 * ��Ϊ��������װ��where�����ֶ�����,��װ�������ֶ�,���Կ��ܻ�����ظ�
	 */
	private void checkOrderColumnExist(AnalyzeTableStructure ats, String column_name,Map<String, String> map) {
		// TODO Auto-generated method stub
		AnalyzeColumnStructure acs;
		boolean is_find=false;
		for(Iterator<AnalyzeColumnStructure> r=ats.list.iterator();r.hasNext();)
		{
			acs=r.next();
			if(acs.column_name.equals(column_name)==true)
			{
				//����,��ֻ��Ҫ���������ֶε�ֵ
				acs.is_order_column=true;
				is_find=true;
				break;
			}
		}
		
		if(is_find==false)
		{
			//ֱ��д��ats��
			AnalyzeColumnStructure newacs = new AnalyzeColumnStructure();
			newacs.column_name=column_name;
			//������һ�е�Ԫ����
			Column_Node cn=md.searchColumnMetaData(ats.tablename, column_name);
			if(cn==null){
				//�����Ƿ�ʹ���˱���
				if(map==null){
					auto_review_error="Table:"+ats.tablename+" Column:"+column_name+" does not exist.";
					logger.warn(auto_review_error);
					return;
				}
				String real_column_name=map.get(column_name);
				if(real_column_name!=null){
					if(real_column_name.indexOf("(")>=0)
					{
						//ʹ�ú�������Ҳ��������
						return;
					}else {
						cn=md.searchColumnMetaData(ats.tablename, real_column_name);
						if(cn==null){
							auto_review_error="Table:"+ats.tablename+" Column:"+column_name+" does not exist.";
							logger.warn(auto_review_error);
							return;
						}else {
							newacs.column_name=real_column_name;
						}
					}
				}else {
					auto_review_error="Table:"+ats.tablename+" Column:"+column_name+" does not exist.";
					logger.warn(auto_review_error);
					return;
				}
				
			}
			
			newacs.column_type=cn.column_type;
			newacs.is_order_column=true;
			ats.list.add(newacs);
		}
	}

	/*
	 * װ��where����
	 */
	private void LoadWhereDataToACS(Tree_Node whereNode,
			                        AnalyzeTableStructure ats) {
		// TODO Auto-generated method stub
		if(whereNode==null)
		{
			logger.warn("LoadWhereDataToACSװ��where����ʱ�����쳣,whereNode=null");
			return;
		}
		if(whereNode.node_type==4)
		{
			//��ǰ���Ϊand / or
			LoadWhereDataToACS(whereNode.left_node,ats);
			LoadWhereDataToACS(whereNode.right_node,ats);
		}
		else if(whereNode.node_type==2)
		{
			AnalyzeColumnStructure acs = new AnalyzeColumnStructure();
			//��ǰ���Ϊ������>,<,=,>=,<= in
			acs.symbol=whereNode.node_content;
			//�����������ֵ,�Ⱥŷ�ֵ���
			if(acs.symbol.equals("="))
				acs.symbol_score = 10;
			else {
				acs.symbol_score = 5;
			}
			//���ӿ϶�Ϊ������������
			acs.column_name=whereNode.left_node.node_content;
			//������һ�е�Ԫ����
			Column_Node cn=md.searchColumnMetaData(ats.tablename, acs.column_name);
			if(cn==null){
				auto_review_error="Table:"+ats.tablename+" Column:"+acs.column_name+" does not exist.";
				logger.warn(auto_review_error);
				return;
			}
			acs.column_type=cn.column_type;
			//to do
			acs.column_type_score=100;
			acs.is_null_able=cn.is_nullable;
			acs.Cardinality=cn.sample_card;
			//to do ����ķ�����һ����Եķ���
			acs.Cardinality_score=100;
		    //������������ڵ�����Ԫ����
			acs.list_index = md.searchIndexMetaData(ats.tablename, acs.column_name);
			if(acs.list_index.isEmpty()==true)
				acs.exist_index=false;
			else {
				acs.exist_index=true;
			}
			acs.type=1;
			
			//���е�������Ϣ��ӵ�table��,׼�������ļ���ʹ��
			if(!checkExistAcsInAts(acs,ats)){
			    ats.list.add(acs);
			}
		}
	}

	//��ӡ���е�SQL
	public void printAllSQL()
	{
		String sql;
		logger.info("���д���˵�SQL�ܹ�"+list_sql.size()+"����SQL���£�");
		for(Iterator<SQL_Node> r=list_sql.iterator();r.hasNext();)
    	{
    		sql = r.next().sqlString;
    		logger.info("SQL="+sql);
    	}
	}
	
	/*
	 * ǰ�˵��õĽӿ�
	 */
	public void createIndexService(int fileMapId) throws Exception
	{
		    try {
				getAllSQL(fileMapId);
				reviewSQL();
			} catch (Exception e) {
				logger.error(e);
				throw e;
			}
	        
	}
	
	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException 
	{
        CreateIndex ci = new CreateIndex();
        //�κεط�new������󣬶���Ҫ������������һ����飬������û�д����ɹ������κ�һ��û�д����ɹ������̷���
        if(ci.wtb.checkConnection()==false)
        {
        	logger.error("�޷�������SQL REVIEW DATABASE���������á�");
        	return;
        }
        if(ci.md.checkConnection()==false)
        {
        	logger.error("�޷������϶�Ӧ�� DATABASE���������á�");
        	return;
        }
        
        ci.getAllSQL();
        ci.reviewSQL();
	}

}
