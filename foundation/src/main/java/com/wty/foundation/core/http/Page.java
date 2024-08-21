package com.wty.foundation.core.http;

import java.util.Collections;
import java.util.List;

/**
 * @author 创建人： wutianyu
 * @createTime 创建时间： 2023/1/16
 */
public class Page<T> {
    private List<T> rows;
    private int total;

    public List<T> getRows() {
        return rows == null ? Collections.EMPTY_LIST : rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
