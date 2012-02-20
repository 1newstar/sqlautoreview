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

import java.util.List;

/*
 * interface��
 * HandleSQLReviewDB����IHandleDB�ӿڵ�һ��ʵ��
 */

public interface IHandleDB {
	//check connection status
	public boolean checkConnection();
	//��SQLд�뵽SQL review database��
	public boolean insertDB(int sqlmap_file_id, String java_class_id, String sql_xml, String real_sql, String sql_comment);
	//��ô���˵�SQL,�������ļ��ж�ȡsqlmap_fileid
	public List<SQL_Node> getAllSQL();
	//��ô���˵�SQL
	public List<SQL_Node> getAllSQL(int sqlmap_file_id);
	//��������SQL�����״̬
	public int updateSQLStatus(int status,String sql_auto_index,int id,String auto_review_err,String auto_review_tip);
	//��������SQL�����״̬
	public int updateSQLStatus(int status,String sql_auto_index,int id,String auto_review_err,String auto_review_tip,String tablenames);
	//�����˳�������index
	//merge index����
	//����ֻ��ҪIndex_Node�е�table_name,index_name�����ֶ�
	public List<Index_Node> getAllIndexes();
	//�����˳�������index
	//merge index����
	//����ֻ��ҪIndex_Node�е�table_name,index_name�����ֶ�
	public List<Index_Node> getAllIndexes(int sqlmap_file_id);
	//ɾ��merge���
	public void deleteMergeResult(int sqlmap_file_id);
	//����merge���
	public void saveMergeResult(int sqlmap_file_id,String tablename,String real_tablename,String exist_indexes,String new_indexes,String merge_result);
}

