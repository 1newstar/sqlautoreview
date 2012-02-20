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

/*
 * ����ÿ���������Ľ��
 */
class ParseStruct {
	//����
	public String tablename;
	//�����
	public String alias_tablename;
	//whereString
	public String whereString;
	//where���������
	public Tree_Node whereNode;
	//��ѯ�ֶ�
    public String select_column;
    //group by�ֶ�
    public String groupbycolumn;
    //�����ֶ�
    public String orderbycolumn;
    
    public ParseStruct()
    {
    	groupbycolumn="";
    	orderbycolumn="";
    }
}
