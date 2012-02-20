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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;


import org.apache.log4j.Logger;



/*
 * ��һ��table���������кϲ�
 */
public class TableMergeIndex 
{
	 //log4j��־
    private static Logger logger = Logger.getLogger(TableMergeIndex.class);
	//����
	String tablename;
    //�����Ѿ����ڵ�����
	List<MergeIndex_Node> list_exist_indexes;
	//����SQL review�´���������
	List<MergeIndex_Node> list_new_indexes;
	//�Ѵ��ڵ�����ȥ��
	Set<MergeIndex_Node> set_exist_indexes;
	//�´���������ȥ��
	Set<MergeIndex_Node> set_new_indexes;
	//���
	Set<MergeIndex_Node> set_result_new_indexes;
	
	/*
	 * ���캯��
	 */
	public TableMergeIndex(String tablename)
	{
		this.tablename=tablename;
		this.list_exist_indexes=new LinkedList<MergeIndex_Node>();
		this.list_new_indexes=new LinkedList<MergeIndex_Node>();
		this.set_exist_indexes=new HashSet<MergeIndex_Node>();
		this.set_new_indexes=new HashSet<MergeIndex_Node>();
		this.set_result_new_indexes=new HashSet<MergeIndex_Node>();
	}
	
	/*
	 * �������list
	 */
	private void addIndexToList(List<MergeIndex_Node> list,int type)
	{
		if(list==null){
			return;
		}
		
		if(type==1){
			this.list_exist_indexes=list;
		}else if(type==2){
			this.list_new_indexes=list;
		}
	}
	
	/*
	 * index��һ�μ���ȥ��,ͨ��set�����
	 * index_name,indexed_columns���߶���ͬ,�Ż�ȥ��
	 */
	private void deleteRepeatIndex1() 
	{
		if(this.list_exist_indexes.size()!=0){
			this.set_exist_indexes.addAll(list_exist_indexes);	
		}else {
			logger.info("list_exist_indexes size = 0.");
		}
		
		if(this.list_new_indexes.size()!=0){
			this.set_new_indexes.addAll(list_new_indexes);
		}else{
			logger.info("list_new_indexes size = 0.");
		}
	}
	
	/*
	 * index�ڶ���ȥ��
	 * ���set_new_indexes�е�index,����set_exist_indexes����,��ô��Ҫɾ��
	 * index_name,indexed_columns���߶���ͬ,�Ż�ȥ��
	 */
	private void deleteRepeatIndex2() 
	{
		if(set_exist_indexes.size()==0 || set_new_indexes.size()==0){
			return;
		}
		
		for(MergeIndex_Node mergeIndex_Node:set_exist_indexes)
		{
			if(set_new_indexes.contains(mergeIndex_Node)){
				set_new_indexes.remove(mergeIndex_Node);
			}
		}
	}
	
	/*
	 * index������ȥ��
	 * ������ͬ�������ֶ�,��������ͬ,����������ֻ��Ҫ����һ������
	 * �������ֻ����������set֮��,�����ܴ����ڵ���set�ڲ�
	 */
	private void deleteRepeatIndex3() 
	{
		String indexed_columns;
		if(set_exist_indexes.size()==0 || set_new_indexes.size()==0){
			return;
		}
		for(MergeIndex_Node mergeIndex_Node:set_exist_indexes)
		{
			indexed_columns=mergeIndex_Node.indexed_columns;
			//logger.info(indexed_columns);
			removeExistIndexColumns(indexed_columns);
		}
	}
	
	/*
	 * ɾ����set_new_indexes���ϵ������ֶ���ͬ�Ľڵ�
	 */
	private void removeExistIndexColumns(String indexed_columns)
	{
		logger.debug("removeExistIndexColumns:"+set_new_indexes.size());
		Set<MergeIndex_Node> tmp_set_new_indexes=new HashSet<MergeIndex_Node>();
		
		if(set_new_indexes.size()==0){
			return;
		}
		
		for(MergeIndex_Node mergeIndex_Node:set_new_indexes)
		{
				tmp_set_new_indexes.add(mergeIndex_Node);
		}
		
		set_new_indexes.clear();
		
		for(MergeIndex_Node mergeIndex_Node:tmp_set_new_indexes){
			if(mergeIndex_Node.indexed_columns.equals(indexed_columns)==false)
			{
				set_new_indexes.add(mergeIndex_Node);
			}
		}
		tmp_set_new_indexes.clear();
		logger.debug("removeExistIndexColumns:"+set_new_indexes.size());
	}

	/*
	 * �½��������ֶ�,���Ѵ��ڵ������ֶε��Ӽ�,����˳��Ҫһ��,����������Ҳ����ɾ��
	 * set_new_indexes�ڲ���Ҫ����,set_new_indexes��set_exist_indexesҲ��Ҫ����
	 */
	private void indexMerge1() {
		//���ȴ���set_new_indexes��set_exist_indexes֮�������֮��
		//��ԭ�����ڵ�����Ϊ��׼
		 compareNewIndexToExistIndex();
		
		//�ٴ���set_new_indexes�ڲ�������֮��
		//��Ҫ��set_new_indexes�ֶεĸ��������Ƚ�������,�ֶ��ٵ������ϲ����ֶζ��������
		 mergeNewIndexes();
		
		//����ԭ�����ڵ�����,�Ƿ�����Ҫ�Ժϲ��ĵط�
		//�����,��Ҫ��ɾ�������Ľű����뵽set_result_new_indexes��
		//ɾ��ԭ�������Ķ���ҪС��
		 mergeExistIndexes();
		
		
		//����ԭ�����ڵ�����,�ǲ����½�����,�����ֶε��Ӽ�
		//�����,��Ҫ��ɾ�������Ľű����뵽set_result_new_indexes��
		//ɾ��ԭ�������Ķ���ҪС��
		 compareExistIndexToLastNewIndex();
	}
	
	/*
	 * ͬһ�����ϵ�������������ͬ
	 * ��Ҫ����set_result_new_indexes��������
	 */
	private void indexRename()
	{   Random radom=new Random();
		for(MergeIndex_Node mergeIndex_Node:set_result_new_indexes)
		{
			//�������±�
			int i=Math.abs(radom.nextInt())%100;
			String index_name=mergeIndex_Node.index_name;
			//����Ҫɾ��������,�ǲ���Ҫ�����
			if(mergeIndex_Node.keep==-1){
				continue;
			}
			
			//������ԭ�����������Ƿ��������
			for(MergeIndex_Node tmp_mergeIndex_Node:set_exist_indexes){
				if(tmp_mergeIndex_Node.keep==-1){
					continue;
				}
				if(tmp_mergeIndex_Node.index_name.equals(index_name)){
					mergeIndex_Node.index_name=mergeIndex_Node.index_name+i;
					mergeIndex_Node.createIndexScript="create index "+mergeIndex_Node.index_name+" on "+this.tablename;
					mergeIndex_Node.createIndexScript=mergeIndex_Node.createIndexScript+"("+mergeIndex_Node.indexed_columns+")";
					break;
				}
			}
		}
	}
	/*
	 * ��ԭ�����ڵ�����Ϊ��׼,����µ������ֶ��Ƿ���ԭ���������ֶ���
	 */
	private void compareNewIndexToExistIndex() 
	{
		String indexed_columns;
		for(MergeIndex_Node mergeIndex_Node:set_exist_indexes)
		{
			indexed_columns=mergeIndex_Node.indexed_columns;
			removeExistSimilarIndexColumns(indexed_columns);
		}
		
	}

	/*
	 * ���µ����������Ժϲ�
	 */
	private void mergeNewIndexes() 
	{
		if(set_new_indexes.size()==0){
			return;
		}
		
		List<MergeIndex_Node> list_sort=new LinkedList<MergeIndex_Node>();
		list_sort.addAll(set_new_indexes);
		sortMergeIndexNodeList(list_sort);
		list_sort=selfcheckMatch(list_sort);
		set_result_new_indexes.addAll(list_sort);	
	}

	/*
	 * ��ԭ�����ڵ����������Ժϲ�
	 * drop index index_name on table
	 */
	private void mergeExistIndexes() 
	{
		if(set_exist_indexes.size()==0){
			return;
		}
		List<MergeIndex_Node> list_sort=new LinkedList<MergeIndex_Node>();
		list_sort.addAll(set_exist_indexes);
		sortMergeIndexNodeList(list_sort);
		list_sort=selfcheckMatch(list_sort);
		
		//��list_sort���뵽һ���µļ�����
		Set<MergeIndex_Node> set_exist_indexes2=new HashSet<MergeIndex_Node>();
		set_exist_indexes2.addAll(list_sort);
		
		//�Ƚ��������ϵĲ���,������켴��Ҫɾ��������
		for (Iterator<MergeIndex_Node> iterator=set_exist_indexes.iterator();iterator.hasNext();) {
			MergeIndex_Node mergeIndex_Node=iterator.next();
			if(!set_exist_indexes2.contains(mergeIndex_Node)){
				//����
				mergeIndex_Node.keep=-1;
				//������
				MergeIndex_Node tmp_merIndex_Node=new MergeIndex_Node(mergeIndex_Node.createIndexScript);
				tmp_merIndex_Node.createIndexScript="drop index "+mergeIndex_Node.index_name+" on "+this.tablename;
				tmp_merIndex_Node.keep=-1;
				set_result_new_indexes.add(tmp_merIndex_Node);
				logger.debug("mergeExistIndexes : drop exist index:"+tmp_merIndex_Node.createIndexScript);
			}
		}
		
	}

	
	/*
	 * �����Ҫ�½�������Ϊ��׼,�����ǰ���ڵ������Ӷ�,�ǲ����½������ֶε��Ӽ�
	 */
	private void compareExistIndexToLastNewIndex() 
	{
		String indexed_columns;
		for(MergeIndex_Node mergeIndex_Node:set_new_indexes)
		{
			indexed_columns=mergeIndex_Node.indexed_columns;
			String[] array_new_indexed_columns=indexed_columns.split(",");
			for(MergeIndex_Node mergeIndex_Node2:set_exist_indexes){
				//����Ҫ�����Ѿ�ɾ��������
				if(mergeIndex_Node2.keep==-1){
					continue;
				}
				String[] array_exist_indexed_columns=mergeIndex_Node2.indexed_columns.split(",");
				if(checkMatch(array_new_indexed_columns,array_exist_indexed_columns)){
					//����
					mergeIndex_Node2.keep=-1;
					//��ӽ����Ľ������
					MergeIndex_Node tmp_merIndex_Node=new MergeIndex_Node(mergeIndex_Node2.createIndexScript);
					tmp_merIndex_Node.createIndexScript="drop index "+mergeIndex_Node2.index_name+" on "+this.tablename;
					tmp_merIndex_Node.keep=-1;
					set_result_new_indexes.add(tmp_merIndex_Node);
					logger.debug("compareExistIndexToLastNewIndex : drop exist index:"+tmp_merIndex_Node.createIndexScript);
				}
			}
		}
		
		
		
	}
	
	/*
	 * ����Լ��������ֶ�,�Ƿ����Լ�������element������֮��
	 */
	private List<MergeIndex_Node> selfcheckMatch(List<MergeIndex_Node> list_sort) 
	{
		//���淵�صĽ��
		List<MergeIndex_Node> tmp_list_sort=new LinkedList<MergeIndex_Node>();
		for(int i=0;i<list_sort.size();i++)
		{
			if(list_sort.get(i).keep==-1){
				continue;
			}
			String[] array_new_index1=list_sort.get(i).indexed_columns.split(",");
			for(int j=i+1;j<list_sort.size();j++)
			{
				String[] array_new_index2=list_sort.get(j).indexed_columns.split(",");
				if(checkMatch(array_new_index1,array_new_index2)){
					//�ȴ���
					list_sort.get(j).keep=-1;
				}
			}
		}
		for(int i=0;i<list_sort.size();i++){
			if(list_sort.get(i).keep!=-1){
				tmp_list_sort.add(list_sort.get(i));
			}
		}
		
		list_sort.clear();
		return tmp_list_sort;
		
	}

	/*
	 * ��һ��list<MergeIndex_Node>��indexed_columns_num��С��������
	 */
	private void sortMergeIndexNodeList(List<MergeIndex_Node> list_sort) 
	{
		//����
		for(int i=0;i<list_sort.size();i++)
		{
			for(int j=i+1;j<list_sort.size();j++)
			{
				if(list_sort.get(i).indexed_columns_num < list_sort.get(j).indexed_columns_num)
				{
					//����
					MergeIndex_Node tmp_cc = list_sort.get(i);
					list_sort.set(i, list_sort.get(j));
					list_sort.set(j, tmp_cc);	
				}
			}
		}
		
	}

	/*
	 * ɾ����set_new_indexes���ϵ������ֶ����ƵĽڵ�
	 * �������Ҫ���ֶαȽ�
	 */
	private void removeExistSimilarIndexColumns(String indexed_columns) 
	{
		String[] array_exist_indexed_columns=indexed_columns.split(",");
		Set<MergeIndex_Node> tmp_set_new_indexes=new HashSet<MergeIndex_Node>();
		tmp_set_new_indexes.addAll(set_new_indexes);
		for(Iterator<MergeIndex_Node> iterator=tmp_set_new_indexes.iterator();iterator.hasNext();)
		{
			MergeIndex_Node mergeIndex_Node=iterator.next();
			String[] array_new_indexed_columns=mergeIndex_Node.indexed_columns.split(",");
			if(checkMatch(array_exist_indexed_columns,array_new_indexed_columns)){
				set_new_indexes.remove(mergeIndex_Node);
			}
		}
	}

	
	/*
	 * ��ϸ�Ƚ����������ֶε�����֮��
	 */
	private boolean checkMatch(String[] array_exist_indexed_columns,
			String[] array_new_indexed_columns) 
	{
		if(array_new_indexed_columns.length>array_exist_indexed_columns.length){
			return false;
		}
		for(int i=0;i<array_new_indexed_columns.length;i++){
			if(array_new_indexed_columns[i].equals(array_exist_indexed_columns[i])==false){
				return false;
			}
		}
		
		return true;
	}

	/*
	 * print�ϲ�����Ҫ�½�������
	 */
	public void print_result_new_index()
	{
		System.out.println("\n\n--------Merge Table "+this.tablename+" Indexes--------");
		System.out.println("exist index as follows:");
		for(MergeIndex_Node mergeIndex_Node:list_exist_indexes){
			System.out.println("  "+mergeIndex_Node.createIndexScript);
		}
		System.out.println("new index as follows:");
		for(MergeIndex_Node mergeIndex_Node:list_new_indexes){
			System.out.println("  "+mergeIndex_Node.createIndexScript);
		}
		System.out.println("Merge result:");
		if(set_result_new_indexes.size()==0){
			System.out.println("  ����Ҫ�½�����.");
			return;
		}
		for(MergeIndex_Node mergeIndex_Node:set_result_new_indexes)
		{
			System.out.println("  "+mergeIndex_Node.createIndexScript);
		}
	}
	
	private List<String> getMergeIndexResult()
	{
		List<String> list=new LinkedList<String>();
		for(MergeIndex_Node mergeIndex_Node:set_result_new_indexes)
		{
			list.add(mergeIndex_Node.createIndexScript);
		}
		return list;
	}
	/*
	 * �ⲿ�ӿ�
	 */
	public List<String> tableMergeIndexService(List<MergeIndex_Node> list_exist_index,
			List<MergeIndex_Node> list_new_index)
	{
		addIndexToList(list_exist_index,1);
		addIndexToList(list_new_index,2);
		deleteRepeatIndex1();
		deleteRepeatIndex2();
		deleteRepeatIndex3();
		indexMerge1();
		indexRename();
		return getMergeIndexResult();
	}

}
