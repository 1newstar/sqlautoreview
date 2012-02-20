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

//��������֮��Ĺ�ϵ,ֻ��������������
class Table_Relationship
{
	//����
	String tablename;
	//��ı���
	String alias_tablename;
	//������һ����Ĺ�����
	String columnname1;
	//������һ����Ĺ�����
	String columnname2;
	//�ڴ˲�ѯ�����£����Card
	int Cardinality;
	
	public Table_Relationship()
	{
		tablename="";
		alias_tablename="";
		columnname1="";
		columnname2="";
		Cardinality=0;
	}
}
