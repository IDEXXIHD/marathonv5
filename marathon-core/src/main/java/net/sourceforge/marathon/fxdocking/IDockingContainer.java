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
package net.sourceforge.marathon.fxdocking;

import java.util.List;

import javafx.scene.Node;
import net.sourceforge.marathon.fxdocking.DockingConstants.Split;

public interface IDockingContainer {

    void remove(Dockable dockable);

    void split(Dockable base, Dockable dockable, Split position, double proportion);

    void tab(Dockable base, Dockable dockable, int order, boolean select);

    void remove(Node container);

    void replace(Node base, INewDockingContainer indc);

    void getDockables(List<DockableState> dockables);

    void debugPrint(String indent);

}
