package cn.panshi.etf4j.utils;

import java.util.List;

public class PagingSearchResult<T_item> {
	private List<T_item> data;
	private int pageNo;

	public List<T_item> getData() {
		return data;
	}

	public void setData(List<T_item> data) {
		this.data = data;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

}
