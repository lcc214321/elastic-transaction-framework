package cn.panshi.etf.tcc;

import java.util.Date;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EtfTccDaoRedis implements EtfTccDao {
	static Logger logger = Logger.getLogger(EtfTccDaoRedis.class);
	@Resource
	RedisTemplate redisTemplate;
	@Resource
	ThreadPoolTaskExecutor executor;
	@Resource
	EtfTccBeanUtil etfTccBeanUtil;

	public enum ETF_TCC_KEYS {
		ETF_TCC_PREPARE_SET, //

		ETF_TCC_TRY_LIST, //
		ETF_TCC_CONFIRM_LIST, //

		ETF_TCC_RECORD, //

		ETF_TCC_TIMER_TRY, //
		ETF_TCC_TIMER_CONFIRM, //
		ETF_TCC_TIMER_CANCEL, ETF_TCC_COUNTOR_LIST_TRY;

	}

	@Override
	public EtfTccRecordStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId) {
		String key = genTccRecordKey(transTypeEnumClazz, transType, bizId);

		return (EtfTccRecordStep) redisTemplate.opsForValue().get(key);
	}

	private String genTccRecordKey(String tccTransEnumClazz, String tccTransEnumValue, String bizId) {
		return ETF_TCC_KEYS.ETF_TCC_RECORD + ":" + tccTransEnumClazz + "@" + tccTransEnumValue + "#" + bizId;
	}

	@Override
	public void saveEtfTccRecordStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson) {
		EtfTccRecordStep step = new EtfTccRecordStep();
		step.setCrtDate(new Date());
		step.setTccEnumValue(tccEnumValue);
		step.setBizStateJson(bizStateJson);

		String key = genTccRecordKey(tccEnumClassName, tccEnumValue, bizId);
		redisTemplate.opsForValue().set(key, step);
	}

	@Override
	public boolean addEtfTccTransPrepareList(String tccEnumClassName, String bizId, String tccEnumValue)
			throws EtfTccException4PrepareStage {
		String tccPrepareListKey = ETF_TCC_KEYS.ETF_TCC_PREPARE_SET + ":" + tccEnumClassName + "#" + bizId;
		boolean checkExist = redisTemplate.opsForSet().isMember(tccPrepareListKey, tccEnumValue);
		if (checkExist) {
			return false;
		} else {
			Long added = redisTemplate.opsForSet().add(tccPrepareListKey, tccEnumValue);
			logger.debug("add tcc prepare set success,return " + added);

			this.initTccCounter4Try(tccEnumClassName, bizId);
			return true;
		}
	}

	/**
	 * memo:初始化Tcc try计数器，以便TCC交易并发执行到最后一个try完成后 触发confirm或cancel
	 */
	private void initTccCounter4Try(String tccEnumClassName, String bizId) throws EtfTccException4PrepareStage {
		try {
			Enum[] enumConstants = ((Class<Enum>) Class.forName(tccEnumClassName)).getEnumConstants();
			logger.debug("初始化Tcc try计数器：" + (enumConstants.length - 1) + "，以便TCC交易并发执行到最后一个try完成后 触发confirm或cancel");

			String key = ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_TRY + ":" + tccEnumClassName + "#" + bizId;
			Long countorListSize = redisTemplate.opsForList().size(key);
			if (countorListSize == null || countorListSize == 0L) {
				for (int i = 0; i < enumConstants.length - 1; i++) {
					redisTemplate.opsForList().leftPush(key, "" + (i + 1));
				}
			}

		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new EtfTccException4PrepareStage(e.getMessage());
		}
	}

	@Override
	public Set<String> findTccTransList2Start(String tccEnumClassName, String bizId) {
		return redisTemplate.opsForSet()
				.members(ETF_TCC_KEYS.ETF_TCC_PREPARE_SET + ":" + tccEnumClassName + "#" + bizId);
	}

	@Override
	public void startTccTransByPreparedKey(String transTypeEnumClazz, String tccTransEnumValue, String tccTransBizId) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				EtfTccRecordStep tr = loadTccTransRecordStep(transTypeEnumClazz, tccTransEnumValue, tccTransBizId);
				JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

				etfTccBeanUtil.invokeEtfBean(transTypeEnumClazz, tccTransEnumValue, paramJsonObj);
			}
		});

	}

	@Override
	public EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int expireSeconds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String popTccTransListOnTrySuccess() {
		String currBizId = EtfTccAop.getCurrBizId();
		String currTccTransEnumClazzName = EtfTccAop.getCurrTccTransEnumClazzName();
		String tccTryListKey = calcTccTryListKey(currTccTransEnumClazzName, currBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccTryListKey);
		return (String) popValue;
	}

	protected String calcTccTryListKey(String tccTransEnumClazzName, String bizId) {
		String tccTryListKey = ETF_TCC_KEYS.ETF_TCC_TRY_LIST + ":" + tccTransEnumClazzName + "#" + bizId;
		return tccTryListKey;
	}

	@Override
	public void triggerTccConfirmOrCancel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void popTccTransListAndFlagTccFailure() {
		// TODO Auto-generated method stub

	}

	@Override
	public String popTccCancelListOnCancelFinished() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateTccCanceled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTccCancelFailure() {
		// TODO Auto-generated method stub

	}

	@Override
	public String popTccConfirmListOnSuccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateTccSuccess() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTccFailure() {
		// TODO Auto-generated method stub

	}

}