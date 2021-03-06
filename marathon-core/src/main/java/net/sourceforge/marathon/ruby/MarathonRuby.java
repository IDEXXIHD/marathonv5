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
package net.sourceforge.marathon.ruby;

import java.util.logging.Logger;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import net.sourceforge.marathon.runtime.api.DefaultMatcher;
import net.sourceforge.marathon.runtime.api.IPropertyAccessor;
import net.sourceforge.marathon.runtime.api.Marathon;

public class MarathonRuby extends Marathon {

    public static final Logger LOGGER = Logger.getLogger(MarathonRuby.class.getName());

    public static class ContextAccessor extends DefaultMatcher {
        private IRubyObject o;
        private IRubyObject marathon;

        public ContextAccessor(IRubyObject o) {
            this.o = o;
            Ruby runtime = o.getRuntime();
            marathon = runtime.evalScriptlet("$marathon");
        }

        @Override public String getProperty(String name) {
            Ruby runtime = o.getRuntime();
            o = marathon.callMethod(runtime.getCurrentContext(), "refresh_if_stale", o);
            marathon.callMethod(runtime.getCurrentContext(), "setContext", o);
            if ("tag_name".equals(name) || "tagName".equals(name)) {
                return o.callMethod(runtime.getCurrentContext(), "tag_name").toString().toUpperCase();
            }
            return o.callMethod(runtime.getCurrentContext(), "attribute", JavaEmbedUtils.javaToRuby(runtime, name)).toString();
        }

        @Override public String toString() {
            return "ContextAccessor [o=" + o + "]";
        }

    }

    final class ContextCloseHandler implements ICloseHandler {
        private final String title;
        private boolean runNeeded;
        private IRubyObject current_context;

        private ContextCloseHandler(String title) {
            this.title = title;
            this.runNeeded = true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.sourceforge.marathon.player.ICloseHandler#run()
         */
        @Override public void run() {
            if (!runNeeded) {
                return;
            }
            runNeeded = false;
            if (current_context == null) {
                switchToContext(title);
                current_context = getCurrentContext();
            } else {
                setContext(current_context);
            }
            IPropertyAccessor context = getContextAsAccessor(current_context);
            namingStrategy.setTopLevelComponent(context);
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.sourceforge.marathon.player.ICloseHandler#setRunNeeded()
         */
        @Override public void setRunNeeded() {
            this.runNeeded = true;
        }

        @Override public String toString() {
            return "Context: " + title + "(" + runNeeded + ")";
        }
    }

    public MarathonRuby(String driverURL) {
        super(driverURL);
    }

    public IPropertyAccessor getContextAsAccessor(IRubyObject o) {
        return new ContextAccessor(o);
    }

    public void context(final String title, long timeout) {
        ICloseHandler r = new ContextCloseHandler(title);
        r.run();
        closeHandlers.add(r);
    }

    public void setContext(IRubyObject context) {
    }

    public IRubyObject getCurrentContext() {
        return null;
    }

    public void onWSConnectionClose(int port) {
    }

    public boolean isDriverAvailable() {
        return true;
    }

}
