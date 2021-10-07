package org.benetech.model.rows;

import org.opendatakit.aggregate.odktables.rest.entity.Row;

public class RowSelect {
    private Boolean selected;
    private Row row;

    /**
     * @return Boolean return the selected
     */
    public Boolean getSelected() {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    /**
     * @return Row return the row
     */
    public Row getRow() {
        return row;
    }

    /**
     * @param row the row to set
     */
    public void setRow(Row row) {
        this.row = row;
    }

}
