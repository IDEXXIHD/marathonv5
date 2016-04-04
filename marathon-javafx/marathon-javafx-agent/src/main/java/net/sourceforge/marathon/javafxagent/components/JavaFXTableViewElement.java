package net.sourceforge.marathon.javafxagent.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import net.sourceforge.marathon.javafxagent.IJavaFXAgent;
import net.sourceforge.marathon.javafxagent.IJavaFXElement;
import net.sourceforge.marathon.javafxagent.JavaFXElement;
import net.sourceforge.marathon.javafxagent.JavaFXTargetLocator.JFXWindow;

public class JavaFXTableViewElement extends JavaFXElement {

    public JavaFXTableViewElement(Node component, IJavaFXAgent driver, JFXWindow window) {
        super(component, driver, window);
    }

    @Override public List<IJavaFXElement> getByPseudoElement(String selector, Object[] params) {
        if (selector.equals("mnth-cell"))
            return Arrays.asList(
                    new JavaFXTableCellElement(this, ((Integer) params[0]).intValue() - 1, ((Integer) params[1]).intValue() - 1));
        else if (selector.equals("all-cells")) {
            TableView<?> tableView = (TableView<?>) getComponent();
            int rowCount = tableView.getItems().size();
            int columnCount = tableView.getColumns().size();
            ArrayList<IJavaFXElement> r = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    r.add(new JavaFXTableCellElement(this, i, j));
                }
            }
            return r;
        } else if (selector.equals("select-by-properties")) {
            return findSelectByProperties(new JSONObject((String) params[0]));
        }
        return super.getByPseudoElement(selector, params);
    }

    private List<IJavaFXElement> findSelectByProperties(JSONObject o) {
        List<IJavaFXElement> r = new ArrayList<>();
        if (o.has("select")) {
            JSONObject jo = new JSONObject((String) o.get("select"));
            JSONArray cell = (JSONArray) jo.get("cell");
            r.add(new JavaFXTableCellElement(this, cell.getInt(0), getColumnIndex(cell.getString(1))));
        }
        return r;
    }

    @Override public boolean marathon_select(String value) {
        TableView<?> tableView = (TableView<?>) node;
        TableViewSelectionModel<?> selectionModel = tableView.getSelectionModel();
        if ("".equals(value)) {
            selectionModel.clearSelection();
            return true;
        } else if (value.equals("all")) {
            int rowSize = tableView.getItems().size();
            for (int i = 0; i < rowSize; i++)
                selectionModel.select(i);
            return true;
        } else if (selectionModel.isCellSelectionEnabled()) {
            selectCells(tableView, value);
            return true;
        } else {
            int[] selectedRows = getSelectedRows(value);
            selectionModel.clearSelection();
            for (int i = 0; i < selectedRows.length; i++) {
                int rowIndex = selectedRows[i];
                tableView.scrollTo(rowIndex);
                selectionModel.selectIndices(rowIndex);
            }
            return true;
        }
    }

    @Override public String _getText() {
        return getSelection((TableView<?>) getComponent());
    }

    public String getContent() {
        return new JSONArray(getContent((TableView<?>) getComponent())).toString();
    }

    /*
     * NOTE: Same code exits in RFXTableView class. So in case if you want to
     * modify. Modify both.
     */
    private String[][] getContent(TableView<?> tableView) {
        int rows = tableView.getItems().size();
        int cols = tableView.getColumns().size();
        String[][] content = new String[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String valueAt = new JavaFXTableCellElement(this, i, j)._getText();
                if (valueAt == null)
                    valueAt = "";
                content[i][j] = valueAt;
            }
        }
        return content;
    }

}