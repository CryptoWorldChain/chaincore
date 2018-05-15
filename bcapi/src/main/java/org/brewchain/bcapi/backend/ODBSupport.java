package org.brewchain.bcapi.backend;

import java.util.concurrent.Future;

import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OPair;
import org.brewchain.bcapi.gens.Oentity.OValue;

import onight.tfw.ojpa.api.DomainDaoSupport;

/**
 * add support for odb -- osgi database.
 * 
 * @author brew
 *
 */
public interface ODBSupport extends DomainDaoSupport{

	/**
	 * 设置某个属性
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws ODBException
	 */
	Future<OValue> put(OKey key, OValue value) throws ODBException;
	Future<OValue> put(String key, OValue value) throws ODBException;


	// only for string
	Future<String> putInfo(String key, String value) throws ODBException;
	
	Future<byte[]> putData(String key, byte[] value) throws ODBException;

	/**
	 * 
	 * 批量操作
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws ODBException
	 */
	Future<OValue[]> batchPuts(OKey[] key, OValue[] value) throws ODBException;

	/**
	 * 交换
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws ODBException
	 */
	Future<OValue> compareAndSwap(OKey key, OValue value, OValue compareValue) throws ODBException;

	/**
	 * 批量操作
	 * 
	 * @param key
	 * @param value
	 * @param compareValue
	 * @return
	 * @throws ODBException
	 */
	Future<OValue[]> batchCompareAndSwap(OKey[] key, OValue[] value, OValue[] compareValue) throws ODBException;

	/**
	 * 判断后删除，原子操作
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws ODBException
	 */
	Future<OValue> compareAndDelete(OKey key, OValue compareValue) throws ODBException;

	/**
	 * 批量判断后删除
	 * 
	 * @param key
	 * @param compareValue
	 * @return
	 * @throws ODBException
	 */
	Future<OValue[]> batchCompareAndDelete(OKey[] key, OValue[] compareValue) throws ODBException;

	/**
	 * 删除
	 * 
	 * @param key
	 * @return
	 * @throws ODBException
	 */
	Future<OValue> delete(OKey key) throws ODBException;

	/**
	 * batch delete
	 * 
	 * @param key
	 * @return
	 * @throws ODBException
	 */
	Future<OValue[]> batchDelete(OKey[] key) throws ODBException;

	/**
	 * 获取
	 * 
	 * @param key
	 * @return
	 * @throws ODBException
	 */
	Future<OValue> get(OKey key) throws ODBException;

	/** 
	 * 
	 */
	Future<OValue> get(String key) throws ODBException;


	/** 
	 * 查询二级索引
	 */
	Future<java.util.List<OPair>> listBySecondKey(String secondKey) throws ODBException;
	
	/** 
	 * 插入二级索引
	 */
	Future<java.util.List<OPair>> putBySecondKey(String secondKey, OValue[] values) throws ODBException;
	
	/** 
	 * 从二级索引移除项
	 */
	Future<java.util.List<OPair>> removeBySecondKey(String secondKey, OKey[] keys) throws ODBException;
	

	/**
	 * 批量获取
	 * 
	 * @param key
	 * @return
	 * @throws ODBException
	 */
	Future<OValue[]> list(OKey[] key) throws ODBException;

	void sync() throws ODBException;
}
