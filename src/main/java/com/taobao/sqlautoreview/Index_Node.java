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

public class Index_Node {
    //���ݿ���
	String table_schema;
	//����
	public String table_name;
	//�Ƿ�Ψһ
	int non_unique;
	//���ݿ���
	String index_schema;
	//����������
	public String index_name;
	//�����ֶ��������е�����λ��
	int seq_in_index;
	//����
	String column_name;
	//�е���
	int Cardinality;
	//����������,�����϶�����BTree
	String index_type;
}
