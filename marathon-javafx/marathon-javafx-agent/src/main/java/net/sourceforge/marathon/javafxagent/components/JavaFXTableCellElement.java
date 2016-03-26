package net.sourceforge.marathon.javafxagent.components;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import net.sourceforge.marathon.javafxagent.IJavaFXElement;
import net.sourceforge.marathon.javafxagent.IPseudoElement;
import net.sourceforge.marathon.javafxagent.JavaFXElement;
import net.sourceforge.marathon.javafxagent.JavaFXElementFactory;

public class JavaFXTableCellElement extends JavaFXElement implements IPseudoElement {

    private JavaFXElement parent;
    private int viewRow;
    private int viewColumn;

    public JavaFXTableCellElement(JavaFXElement parent, int row, int column) {
        super(parent);
        this.parent = parent;
        this.viewRow = row;
        this.viewColumn = column;
    }

    @Override public IJavaFXElement getParent() {
        return parent;
    }

    @Override public String createHandle() {
        JSONObject pa = new JSONObject().put("cell", new JSONArray().put(viewRow).put(getViewColumnName()));
        JSONObject o = new JSONObject().put("selector", "select-by-properties").put("parameters",
                new JSONArray().put(new JSONObject().put("select", pa.toString()).toString()));
        return parent.getHandle() + "#" + o.toString();
    }

    @Override public Node getPseudoComponent() {
        Node tableView = parent.getComponent();
        return getCellAt((TableView<?>) tableView, viewRow, viewColumn);
    }

    @Override public List<IJavaFXElement> getByPseudoElement(String selector, Object[] params) {
        if (selector.equals("editor"))
            return Arrays.asList(JavaFXElementFactory.createElement(getEditor(), driver, window));
        return super.getByPseudoElement(selector, params);
    }

    private Node getEditor() {
        TableCell<?, ?> cell = (TableCell<?, ?>) getPseudoComponent();
        cell.startEdit();
        Node cellComponent = cell.getGraphic();
        cellComponent.getProperties().put("marathon.celleditor", true);
        return cellComponent;
    }

    public int getViewRow() {
        return viewRow + 1;
    }

    public int getViewColumn() {
        return viewColumn + 1;
    }

    public String getViewColumnName() {
        String columnName = getColumnName((TableView<?>) parent.getComponent(), viewColumn);
        if (columnName == null)
            return "" + (viewColumn + 1);
        return columnName;
    }

    public int getRow() {
        return viewRow;
    }

    public String getColumn() {
        String columnName = getColumnName((TableView<?>) parent.getComponent(), viewColumn);
        if (columnName == null)
            return "" + viewColumn;
        return columnName;
    }

    public int getCol() {
        return viewColumn;
    }

}