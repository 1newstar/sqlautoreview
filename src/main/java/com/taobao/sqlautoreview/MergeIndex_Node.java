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
 * ���������ϲ�
 * �������������������
 */
public class MergeIndex_Node {
	 //���������������ű�
	 String createIndexScript;
	 //����������
     String index_name;
     //�������ֶ�
     String indexed_columns;
     //�������ֶθ���
     int indexed_columns_num;
     //�Ƿ���
     int keep;
     
     /*
      * ���캯��
      */
     public MergeIndex_Node(String createIndexScript)
     {
    	 this.createIndexScript=createIndexScript;
    	 this.index_name=getIndexName();
    	 this.indexed_columns=getIndexedColumns();
    	 this.indexed_columns_num=getIndexColumnsNum();
    	 this.keep=0;
     }
     
     /*
      * �������������
      */
     private String getIndexName() 
     {
    	int addr_index=createIndexScript.indexOf(" index ");
    	int addr_on=createIndexScript.indexOf(" on ");
    	return createIndexScript.substring(addr_index+7, addr_on).trim();
	}
     
     /*
      * ����������ֶ�
      */
     private String getIndexedColumns() {
		int addr_left_kuohao=createIndexScript.indexOf("(");
		int addr_right_kuohao=createIndexScript.indexOf(")");
		return createIndexScript.substring(addr_left_kuohao+1, addr_right_kuohao).trim();
	}
     /*
 	 * ͳ��set_new_indexes�������������ֶθ���
 	 */
 	private int getIndexColumnsNum() 
 	{
 		String[] array_indexed_columns=indexed_columns.split(",");
 		return array_indexed_columns.length;
 		
 		
 	}
}
