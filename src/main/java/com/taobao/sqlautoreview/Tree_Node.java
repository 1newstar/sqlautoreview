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

public class Tree_Node {
	  //��ǰ��������
      public String node_content; 
      //����:���� 1;������>,<,=,>=,<= in like is 2;value 3;��������� and or 4
      public int node_type;
      //������
      public Tree_Node left_node;
      //������
      public Tree_Node right_node;
      //����
      public Tree_Node parent_node;
      
      //��ʼ�������������
      public Tree_Node()
      {
    	  node_content=null;
    	  node_type=-1;
    	  left_node=null;
    	  right_node=null;
    	  parent_node=null;
      }
      
}
