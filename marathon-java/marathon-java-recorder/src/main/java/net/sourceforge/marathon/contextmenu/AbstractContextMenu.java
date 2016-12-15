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
package net.sourceforge.marathon.contextmenu;

import net.sourceforge.marathon.component.RComponentFactory;
import net.sourceforge.marathon.javarecorder.IJSONRecorder;

public abstract class AbstractContextMenu implements IContextMenu {

    protected final IJSONRecorder recorder;
    private final RComponentFactory finder;
    protected final ContextMenuWindow window;

    public AbstractContextMenu(ContextMenuWindow window, IJSONRecorder recorder, RComponentFactory finder) {
        this.window = window;
        this.recorder = recorder;
        this.finder = finder;
    }

    public IJSONRecorder getRecorder() {
        return recorder;
    }

    public RComponentFactory getFinder() {
        return finder;
    }

    public ContextMenuWindow getWindow() {
        return window;
    }
}
