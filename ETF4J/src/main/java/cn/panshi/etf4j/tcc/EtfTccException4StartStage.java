package cn.panshi.etf4j.tcc;

@SuppressWarnings("serial")
public class EtfTccException4StartStage extends Exception {
	String error;

	public EtfTccException4StartStage(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
