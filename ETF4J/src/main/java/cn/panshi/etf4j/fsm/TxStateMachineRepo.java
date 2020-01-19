package cn.panshi.etf4j.fsm;

public interface TxStateMachineRepo {

	void updateState(Class<? extends Enum> stateEnumClazz, String key, String actionCode);

	String getCurrentState(Class<? extends Enum> stateEnumClazz, String key);

}
