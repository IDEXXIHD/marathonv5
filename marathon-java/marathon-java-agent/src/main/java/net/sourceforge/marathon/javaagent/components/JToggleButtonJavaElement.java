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
package net.sourceforge.marathon.javaagent.components;

import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.JToggleButton;

import net.sourceforge.marathon.javaagent.IJavaAgent;
import net.sourceforge.marathon.javaagent.JavaElement;
import net.sourceforge.marathon.javaagent.JavaTargetLocator.JWindow;

public class JToggleButtonJavaElement extends JavaElement {

    public static final Logger LOGGER = Logger.getLogger(JToggleButtonJavaElement.class.getName());

    public JToggleButtonJavaElement(Component component, IJavaAgent driver, JWindow window) {
        super(component, driver, window);
    }

    @Override public boolean marathon_select(String value) {
        boolean selected = Boolean.parseBoolean(value);
        boolean current = ((JToggleButton) component).isSelected();
        if (selected != current) {
            click();
        }
        return true;
    }

}
