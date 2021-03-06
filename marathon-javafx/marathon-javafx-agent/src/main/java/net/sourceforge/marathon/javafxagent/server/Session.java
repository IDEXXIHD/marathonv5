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
package net.sourceforge.marathon.javafxagent.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.scene.Node;
import net.sourceforge.marathon.javafxagent.Device;
import net.sourceforge.marathon.javafxagent.IJavaFXAgent;
import net.sourceforge.marathon.javafxagent.IJavaFXElement;
import net.sourceforge.marathon.javafxagent.JavaFXAgent;
import net.sourceforge.marathon.javafxagent.JavaFXTargetLocator.JFXWindow;
import net.sourceforge.marathon.javafxagent.UnsupportedCommandException;

public class Session {
    
    public static final Logger LOGGER = Logger.getLogger(Session.class.getName());

    private String id;
    private IJavaFXAgent agent;

    private List<LogEntry> logEntries = new ArrayList<LogEntry>();
    private Level logLevel = Level.ALL;

    public Session(Device.Type type) {
        this.id = UUID.randomUUID().toString();
        agent = new JavaFXAgent(type);
    }

    public void log(Level level, String message) {
        if (level.intValue() >= logLevel.intValue()) {
            logEntries.add(new LogEntry(level, message));
        }
    }

    public String getID() {
        return id;
    }

    public Collection<String> getWindowHandles() {
        return agent.getWindowHandles();
    }

    public void deleteWindow() {
        agent.deleteWindow();
    }

    public void window(String name) {
        agent.switchTo().window(name);
    }

    public void setTimeout(long millis) {
        agent.manage().timeouts().implicitlyWait(millis, TimeUnit.MILLISECONDS);
    }

    public String getWindowHandle() {
        return agent.getWindowHandle();
    }

    public String getTitle() {
        return agent.getTitle();
    }

    public IJavaFXElement findElement(String using, String value) {
        if ("name".equals(using)) {
            return agent.findElementByName(value);
        } else if ("tag name".equals(using)) {
            return agent.findElementByTagName(value);
        } else if ("css selector".equals(using)) {
            return agent.findElementByCssSelector(value);
        } else if ("class name".equals(using)) {
            return agent.findElementByClassName(value);
        } else if ("id".equals(using)) {
            return agent.findElementByName(value);
        }
        throw new UnsupportedCommandException("Unsupported look up strategy " + using, null);
    }

    public IJavaFXElement findElement(String id) {
        return agent.findElement(id);
    }

    public List<IJavaFXElement> findElements(String using, String value) {
        if ("name".equals(using)) {
            return agent.findElementsByName(value);
        } else if ("tag name".equals(using)) {
            return agent.findElementsByTagName(value);
        } else if ("css selector".equals(using)) {
            return agent.findElementsByCssSelector(value);
        } else if ("class name".equals(using)) {
            return agent.findElementsByClassName(value);
        } else if ("id".equals(using)) {
            return agent.findElementsByName(value);
        }
        throw new UnsupportedCommandException("Unsupported look up strategy " + using, null);
    }

    public IJavaFXElement getActiveElement() {
        return agent.getActiveElement();
    }

    public IJavaFXElement findElement(IJavaFXElement parent, String using, String value) {
        if ("name".equals(using)) {
            return parent.findElementByName(value);
        } else if ("tag name".equals(using)) {
            return parent.findElementByTagName(value);
        } else if ("css selector".equals(using)) {
            return parent.findElementByCssSelector(value);
        } else if ("class name".equals(using)) {
            return parent.findElementByClassName(value);
        } else if ("id".equals(using)) {
            return parent.findElementByName(value);
        }
        throw new UnsupportedCommandException("Unsupported look up strategy " + using, null);
    }

    public List<IJavaFXElement> findElements(IJavaFXElement parent, String using, String value) {
        if ("name".equals(using)) {
            return parent.findElementsByName(value);
        } else if ("tag name".equals(using)) {
            return parent.findElementsByTagName(value);
        } else if ("css selector".equals(using)) {
            return parent.findElementsByCssSelector(value);
        } else if ("class name".equals(using)) {
            return parent.findElementsByClassName(value);
        } else if ("id".equals(using)) {
            return parent.findElementsByName(value);
        }
        throw new UnsupportedCommandException("Unsupported look up strategy " + using, null);
    }

    public void fillLog(JSONArray dest) {
        for (LogEntry logEntry : logEntries) {
            dest.put(new JSONObject().put("level", logEntry.getLevel()).put("timestamp", logEntry.getTimestamp()).put("message",
                    logEntry.getMessage()));
        }
        logEntries.clear();
    }

    public void quit() {
        agent.quit();
    }

    public JFXWindow getWindow(String windowHandle) {
        if ("current".equals(windowHandle)) {
            return agent.getCurrentWindow();
        }
        return agent.getWindow(windowHandle);
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public JSONObject getWindowProperties() {
        return agent.getWindowProperties();
    }

    public IJavaFXElement findElement(Node component) {
        return agent.findElement(component);
    }

    public byte[] getScreenShot() throws IOException {
        return agent.getScreenShot();
    }

}
