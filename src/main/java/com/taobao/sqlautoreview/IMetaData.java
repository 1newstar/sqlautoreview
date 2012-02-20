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
 * ����ҵ�����ݿ���ʽӿ�,ͨ����Щ�ӿ�,���Ի�����Ԫ����
 * author:danchen /zhaolin
 * create time:2012-1-13
 */
public interface IMetaData {
	//�������״̬
	public boolean checkConnection();
	//���쵥�����indexԪ����
	public List<Index_Node> buildIndexMetaData(String tablename);
	//���쵥�����columnԪ����
    public List<Column_Node> buildColumnMetaData(String tablename);
    //����һ���������Ԫ����column+index of one table
    public void buildTableMetaData(String tablename);
    //������������б��Ԫ����
    public void buildDBMetaData();
    //���ݱ���,����,���ָ���е�Ԫ����
    public Column_Node searchColumnMetaData(String tablename,String column_name);
    //���ݱ���,�������ָ�����ϴ��ڵ�������Ԫ����,һ���п����Ѵ����ڶ����������,���Է��ص���һ��List
    public List<Index_Node> searchIndexMetaData(String tablename,String column_name);
    //��������Ԫ����Cache���Ƿ����
    public boolean checkTableExist(String tablename);
    /*
     * ��������ҵ�����ݿ����Ƿ����,�п��ֿܷ�ֱ�,��ʽtablename_XXXX
     * ʹ����TDDL�����п��ܻ�����������tablename
     * ��Щtablename����SQLMAP�У���Ҫת�������ʹ��
     * ���ʵ�ʵı���,��SQL�еı�����һ��,��Ҫ���ϲ�֪��
     */
    public String findMatchTable(String tablename);
    //���ݱ���,��ñ��ϵ�����,�����Ϊǰ��չʾ�����,��̨���򲻻��õ�����ӿ�
    //֧�ֶ��,����tablenames����淶tablename,tablename
    //��̨��merge indexҲ������������
    //���ظ�ʽseller:PRIMARY(seller_id);idx_seller_nick(nick);|test1:PRIMARY(id);idx_up_uid(user_id,gmt_modified,is_delete);
    public String getIndexesByTableName(String tablenames);
    /*
	 * ���ر�����,��ȡһ�������������
	 */
	public String getIndexesByTableName2(String tablenames);
}
