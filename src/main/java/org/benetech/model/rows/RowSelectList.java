package org.benetech.model.rows;


import java.util.ArrayList;
import java.util.List;

public class RowSelectList {

    private List<RowSelect> rows;

    public RowSelectList() {
        rows = new ArrayList<>();
    }


    public void addRow(RowSelect row) {
        rows.add(row);
    }


    /**
     * @return List<RowSelect> return the rows
     */
    public List<RowSelect> getRows() {
        return rows;
    }

    /**
     * @param rows the rows to set
     */
    public void setRows(List<RowSelect> rows) {
        this.rows = rows;
    }

}
