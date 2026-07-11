package com.qfc.file;

import java.util.ArrayList;
import java.util.List;

public class ExcelPreview {

    private List<List<String>> rows = new ArrayList<List<String>>();

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }
}
