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
package net.sourceforge.marathon.providers;

import java.util.logging.Logger;

import com.google.inject.Provider;

import net.sourceforge.marathon.runtime.api.PlaybackResult;

public class PlaybackResultProvider implements Provider<PlaybackResult> {

    public static final Logger LOGGER = Logger.getLogger(PlaybackResultProvider.class.getName());

    @Override public PlaybackResult get() {
        return new PlaybackResult();
    }

}
