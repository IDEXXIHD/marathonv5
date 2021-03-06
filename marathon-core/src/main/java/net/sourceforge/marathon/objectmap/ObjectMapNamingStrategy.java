/*******************************************************************************
 * Copyright 2016 Jalian Systems Pvt. Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sourceforge.marathon.objectmap;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JComponent;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.scene.control.Alert;
import net.sourceforge.marathon.api.INamingStrategy;
import net.sourceforge.marathon.fx.api.FXUIUtils;
import net.sourceforge.marathon.objectmap.ObjectMapConfiguration.ObjectIdentity;
import net.sourceforge.marathon.objectmap.ObjectMapConfiguration.PropertyList;
import net.sourceforge.marathon.runtime.api.ComponentId;
import net.sourceforge.marathon.runtime.api.ComponentNotFoundException;
import net.sourceforge.marathon.runtime.api.ILogger;
import net.sourceforge.marathon.runtime.api.IPropertyAccessor;
import net.sourceforge.marathon.runtime.api.JSONObjectPropertyAccessor;
import net.sourceforge.marathon.runtime.api.PropertyHelper;
import net.sourceforge.marathon.runtime.api.RuntimeLogger;

public class ObjectMapNamingStrategy implements INamingStrategy {

    public static final Logger LOGGER = Logger.getLogger(ObjectMapNamingStrategy.class.getName());

    private static final String MODULE = "Object Map";
    protected ILogger runtimeLogger;
    private IObjectMapService omapService;
    private IPropertyAccessor topContainerAccessor;

    public ObjectMapNamingStrategy() {
        init();
    }

    public void init() {
        runtimeLogger = RuntimeLogger.getRuntimeLogger();
        omapService = getObjectMapService();
        try {
            omapService.load();
            runtimeLogger.info(MODULE, "Loaded object map omapService");
        } catch (IOException e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            runtimeLogger.error(MODULE, "Error in creating naming strategy:" + e.getMessage(), w.toString());
            FXUIUtils.showMessageDialog(null, "Error in creating naming strategy:" + e.getMessage(), "Error in NamingStrategy",
                    Alert.AlertType.ERROR);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override public void setDirty() {
        omapService.setDirty(true);
    }

    protected IObjectMapService getObjectMapService() {
        return new ObjectMapService();
    }

    @Override public void setTopLevelComponent(IPropertyAccessor accessor) {
        topContainerAccessor = accessor;
    }

    private Class<?> findClass(String cName) {
        try {
            return Class.forName(cName);
        } catch (Throwable e) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(cName);
            } catch (ClassNotFoundException e1) {
                return JComponent.class;
            }
        }
    }

    private String findCSS(ComponentId id, boolean visibility) throws ObjectMapException {
        String css;
        if (id.getName() != null) {
            OMapComponent findComponentByName = omapService.findComponentByName(id.getName(), topContainerAccessor);
            if (findComponentByName == null) {
                throw new NoSuchElementException("No entry found in the object map for `" + id.getName() + "`");
            }
            css = toCSS(findComponentByName, visibility);
        } else {
            css = PropertyHelper.toCSS(id.getNameProps());
        }
        return css;
    }

    private String toCSS(OMapComponent omapComponent, boolean visibility) {
        OMapRecognitionProperty typeProperty = null;
        OMapRecognitionProperty indexProperty = null;
        OMapRecognitionProperty tagNameProperty = null;

        List<OMapRecognitionProperty> properties = omapComponent.getComponentRecognitionProperties();
        StringBuilder sb = new StringBuilder();
        if (properties.size() == 1) {
            OMapRecognitionProperty rp = properties.get(0);
            if (rp.getName().equals("css")) {
                return rp.getValue();
            }
        }
        for (OMapRecognitionProperty rp : properties) {
            if (rp.getName().equals("type")) {
                typeProperty = rp;
            } else if (rp.getName().equals("indexOfType")) {
                indexProperty = rp;
            } else if (rp.getName().equals("tagName")) {
                tagNameProperty = rp;
            } else {
                sb.append("[").append(rp.getName()).append(op(rp.getMethod())).append("'")
                        .append(rp.getValue().replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'")).append("']");
            }
        }
        if (visibility) {
            sb.append("[visible='true']");
        }
        String r = sb.toString();
        if (typeProperty != null) {
            r = "[" + typeProperty.getName() + op(typeProperty.getMethod()) + "'" + typeProperty.getValue() + "']" + r;
        }
        if (indexProperty != null) {
            r = r + "[" + indexProperty.getName() + op(indexProperty.getMethod()) + "'" + indexProperty.getValue() + "']";
        }
        if (tagNameProperty != null) {
            if (tagNameProperty.getMethod().equals("equals")) {
                r = tagNameProperty.getValue() + r;
            } else {
                r = "[" + tagNameProperty.getName() + op(tagNameProperty.getMethod()) + "'" + tagNameProperty.getValue() + "']" + r;
            }
        }
        return r;
    }

    private Object op(String method) {
        if (method.equals("equals")) {
            return "=";
        } else if (method.equals("startsWith")) {
            return "^=";
        } else if (method.equals("endsWith")) {
            return "$=";
        } else if (method.equals("contains")) {
            return "*=";
        } else if (method.equals("matches")) {
            return "/=";
        }
        throw new RuntimeException("Unknown method " + method + " when converting to CSS");
    }

    @Override public String getName(JSONObject s, String n) throws JSONException, ObjectMapException {
        OMapComponent o = findOMapComponent(s, n);
        if (o != null) {
            o.markEntryNeeded(true);
            return o.getName();
        }
        return null;
    }

    private OMapComponent findOMapComponent(JSONObject component, String n) throws JSONException, ObjectMapException {
        JSONObject window = component.getJSONObject("container");
        Properties urpContainer = PropertyHelper.asProperties(window.getJSONObject("containerURP"));
        Properties attributesContainer = PropertyHelper.asProperties(window.getJSONObject("attributes"));
        List<OMapComponent> omapComponents = omapService.findComponentsByProperties(
                PropertyHelper.asProperties(component.getJSONObject("attributes")), urpContainer, attributesContainer);
        if (omapComponents.size() == 1) {
            return omapComponents.get(0);
        }
        if (omapComponents.size() > 1) {
            String message = "More than one component matched for " + component;
            StringBuilder msg = new StringBuilder(message);
            msg.append("\n    The matched object map entries are:\n");
            for (OMapComponent omc : omapComponents) {
                msg.append("        ").append(omc.toString()).append("\n");
            }
            OMapComponent omapComponent = findClosestMatch(component, omapComponents, msg);
            if (omapComponent != null) {
                runtimeLogger.warning(MODULE, message, msg.toString());
                return omapComponent;
            }
            runtimeLogger.error(MODULE, message, msg.toString());
            throw new ComponentNotFoundException("More than one component matched: " + omapComponents, null);
        }
        String name = createName(component, urpContainer, attributesContainer, n);
        Properties urp = PropertyHelper.asProperties(component.getJSONObject("urp"));
        Properties properties = PropertyHelper.asProperties(component.getJSONObject("attributes"));
        return omapService.insertNameForComponent(name, urp, properties, urpContainer, attributesContainer);
    }

    private String createName(JSONObject component, Properties urpContainer, Properties attributesContainer, String n)
            throws ObjectMapException {
        String name = null;
        if (n == null) {
            IPropertyAccessor w = new JSONObjectPropertyAccessor(component.getJSONObject("attributes"));
            List<List<String>> propertyList = findNamingProperties(w.getProperty("component.class.name"));
            for (List<String> properties : propertyList) {
                name = createName(w, properties);
                if (name == null || name.equals("")) {
                    continue;
                }
                if (omapService.findComponentByName(name, urpContainer, attributesContainer) == null) {
                    return name;
                }
                break;
            }
        } else {
            name = n;
        }
        String original = name;
        int index = 2;
        while (omapService.findComponentByName(name, urpContainer, attributesContainer) != null) {
            name = original + "_" + index++;
        }
        return name;
    }

    private String createName(IPropertyAccessor w, List<String> properties) {
        StringBuilder sb = new StringBuilder();
        for (String property : properties) {
            String v = w.getProperty(property);
            if (v == null || v.equals("")) {
                return null;
            }
            sb.append(v).append('_');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString().trim();
    }

    private OMapComponent findClosestMatch(JSONObject component, List<OMapComponent> omapComponents, StringBuilder msg) {
        return null;
    }

    private List<List<String>> findNamingProperties(String cName) {
        List<List<String>> np = findProperties(findClass(cName), omapService.getNamingProperties());
        np.add(OMapComponent.LAST_RESORT_NAMING_PROPERTIES);
        return np;
    }

    private List<List<String>> findProperties(Class<?> class1, List<ObjectIdentity> list) {
        List<PropertyList> selection = new ArrayList<PropertyList>();
        while (class1 != null) {
            for (ObjectIdentity objectIdentity : list) {
                if (objectIdentity.getClassName().equals(class1.getName())) {
                    selection.addAll(objectIdentity.getPropertyLists());
                }
            }
            class1 = class1.getSuperclass();
        }
        Collections.sort(selection, new Comparator<PropertyList>() {
            @Override public int compare(PropertyList o1, PropertyList o2) {
                return o2.getPriority() - o1.getPriority();
            }
        });
        List<List<String>> sortedList = new ArrayList<List<String>>();
        for (PropertyList pl : selection) {
            sortedList.add(new ArrayList<String>(pl.getProperties()));
        }
        return sortedList;
    }

    @Override public void save() {
        omapService.save();
    }

    @Override public String getContainerName(JSONObject container) throws JSONException, ObjectMapException {
        // For a container we shall use urp to generate the name
        JSONObject attributes = container.getJSONObject("attributes");
        String name;
        if (attributes.has("suggestedName")) {
            name = attributes.getString("suggestedName");
        } else {
            JSONObject urp = container.getJSONObject("urp");
            StringBuilder sb = new StringBuilder();
            String[] keys = JSONObject.getNames(urp);
            for (String key : keys) {
                sb.append(urp.get(key).toString()).append(':');
            }
            sb.setLength(sb.length() - 1);
            name = sb.toString();
        }
        return getName(container, name);
    }

    public String getName(JSONObject component) throws JSONException, ObjectMapException {
        return getName(component, null);
    }

    @Override public String[] toCSS(ComponentId componentId, boolean visibility) throws ObjectMapException {
        String[] r = new String[] { null, null };
        r[0] = findCSS(componentId, visibility);
        r[1] = findCSSForInfo(componentId);
        return r;
    }

    private String findCSSForInfo(ComponentId componentId) {
        Properties p;
        if (componentId.getComponentInfo() != null) {
            p = new Properties();
            p.setProperty("select", componentId.getComponentInfo());
        } else if (componentId.getComponentInfoProps() != null) {
            p = componentId.getComponentInfoProps();
        } else {
            return null;
        }
        JSONObject o = new JSONObject(p);
        return "select-by-properties('" + o.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'") + "')";
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    @SuppressWarnings("unused") private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @SuppressWarnings("unused") private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0, j = 0; i < hex.length(); i += 2, j++) {
            bytes[j] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    @Override public String[] getComponentNames() throws ObjectMapException {
        return omapService.findComponentNames(topContainerAccessor);
    }

    @Override public List<List<String>> getContainerNamingProperties(String name) {
        List<PropertyList> allProperties = new ArrayList<>();
        List<ObjectIdentity> namingProperties = omapService.getContainerNamingProperties();
        for (ObjectIdentity objectIdentity : namingProperties) {
            if (objectIdentity.getClassName().equals(name)) {
                List<PropertyList> propertyLists = objectIdentity.getPropertyLists();
                allProperties.addAll(propertyLists);
            }
        }
        Collections.sort(allProperties, new Comparator<PropertyList>() {
            @Override public int compare(PropertyList o1, PropertyList o2) {
                return o2.getPriority() - o1.getPriority();
            }
        });
        List<List<String>> r = new ArrayList<>();
        for (PropertyList propertyList : allProperties) {
            r.add(propertyList.getProperties());
        }
        return r;
    }

    @Override public OMapComponent getOMapComponent(ComponentId id) throws ObjectMapException {
        if (id.getName() != null) {
            OMapComponent findComponentByName = omapService.findComponentByName(id.getName(), topContainerAccessor);
            if (findComponentByName == null) {
                throw new NoSuchElementException("No entry found in the object map for `" + id.getName() + "`");
            }
            return findComponentByName;
        }
        return null;
    }
}
