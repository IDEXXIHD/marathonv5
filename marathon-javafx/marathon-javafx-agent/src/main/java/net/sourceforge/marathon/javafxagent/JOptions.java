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
package net.sourceforge.marathon.javafxagent;

import java.util.logging.Logger;

public class JOptions {

    public static final Logger LOGGER = Logger.getLogger(JOptions.class.getName());

    private JTimeouts timeouts;
    private IJavaFXAgent agent;

    public JTimeouts timeouts() {
        if (timeouts == null) {
            timeouts = new JTimeouts(agent);
        }
        return timeouts;
    }

    public JOptions(IJavaFXAgent agent) {
        this.agent = agent;
    }
}
