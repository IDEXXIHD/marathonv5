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
package net.sourceforge.marathon.runtime.ws;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for converting between JSON and Java Objects.
 */
public class BeanToJsonConverter {

    public static final Logger LOGGER = Logger.getLogger(BeanToJsonConverter.class.getName());

    private static final int MAX_DEPTH = 10;

    /**
     * Convert an object that may or may not be a JSONArray or JSONObject into
     * its JSON string representation, handling the case where it is neither in
     * a graceful way.
     *
     * @param object
     *            which needs conversion
     * @return the JSON string representation of object
     */
    public String convert(Object object) {
        if (object == null) {
            return null;
        }

        try {
            Object converted = convertObject(object, MAX_DEPTH);
            if (converted instanceof JSONObject || converted instanceof JSONArray || converted instanceof String
                    || converted instanceof Number) {
                return converted.toString();
            }

            return String.valueOf(object);
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert: " + object, e);
        }
    }

    /**
     * Convert a JSON[Array|Object] into the equivalent Java Collection type
     * (that is, List|Map) returning other objects untouched. This method is
     * used for preparing values for use by the HttpCommandExecutor
     *
     * @param o
     *            Object to convert
     * @return a Map, List or the unconverted Object
     */
    private Object convertUnknownObjectFromJson(Object o) {
        if (o instanceof JSONArray) {
            return convertJsonArray((JSONArray) o);
        }

        if (o instanceof JSONObject) {
            return convertJsonObject((JSONObject) o);
        }

        return o;
    }

    private Map<String, Object> convertJsonObject(JSONObject jsonObject) {
        Map<String, Object> toReturn = new HashMap<String, Object>();
        Iterator<?> allKeys = jsonObject.keys();
        while (allKeys.hasNext()) {
            String key = (String) allKeys.next();

            try {
                toReturn.put(key, convertUnknownObjectFromJson(jsonObject.get(key)));
            } catch (JSONException e) {
                throw new IllegalStateException("Unable to access key: " + key, e);
            }
        }
        return toReturn;
    }

    private List<Object> convertJsonArray(JSONArray jsonArray) {
        List<Object> toReturn = new ArrayList<Object>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                toReturn.add(convertUnknownObjectFromJson(jsonArray.get(i)));
            } catch (JSONException e) {
                throw new IllegalStateException("Cannot convert object at index: " + i, e);
            }
        }
        return toReturn;
    }

    @SuppressWarnings("unchecked") private Object convertObject(Object toConvert, int maxDepth) throws Exception {
        if (toConvert == null) {
            return JSONObject.NULL;
        }

        if (toConvert instanceof Boolean || toConvert instanceof CharSequence || toConvert instanceof Number) {
            return toConvert;
        }

        if (toConvert instanceof Level) {
            return toConvert.toString();
        }

        if (toConvert.getClass().isEnum() || toConvert instanceof Enum) {
            return toConvert.toString();
        }

        if (toConvert instanceof Map) {
            JSONObject converted = new JSONObject();
            for (Object objectEntry : ((Map<?, ?>) toConvert).entrySet()) {
                Map.Entry<String, Object> entry = (Entry<String, Object>) objectEntry;
                converted.put(entry.getKey(), convertObject(entry.getValue(), maxDepth - 1));
            }
            return converted;
        }

        if (toConvert instanceof JSONObject) {
            return toConvert;
        }

        if (toConvert instanceof Collection) {
            JSONArray array = new JSONArray();
            for (Object o : (Collection<?>) toConvert) {
                array.put(convertObject(o, maxDepth - 1));
            }
            return array;
        }

        if (toConvert.getClass().isArray()) {
            JSONArray converted = new JSONArray();
            int length = Array.getLength(toConvert);
            for (int i = 0; i < length; i++) {
                converted.put(convertObject(Array.get(toConvert, i), maxDepth - 1));
            }
            return converted;
        }

        if (toConvert instanceof Date) {
            return TimeUnit.MILLISECONDS.toSeconds(((Date) toConvert).getTime());
        }

        if (toConvert instanceof File) {
            return ((File) toConvert).getAbsolutePath();
        }

        Method toJson = getToJsonMethod(toConvert);
        if (toJson != null) {
            try {
                return toJson.invoke(toConvert);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return mapObject(toConvert, maxDepth - 1, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Method getToJsonMethod(Object toConvert) {
        try {
            return toConvert.getClass().getMethod("toJson");
        } catch (SecurityException e) {
            // fall through
        } catch (NoSuchMethodException e) {
            // fall through
        }

        return null;
    }

    private Object mapObject(Object toConvert, int maxDepth, boolean skipNulls) throws Exception {
        if (maxDepth < 1) {
            return null;
        }

        // Raw object via reflection? Nope, not needed
        JSONObject mapped = new JSONObject();
        for (SimplePropertyDescriptor pd : SimplePropertyDescriptor.getPropertyDescriptors(toConvert.getClass())) {
            if ("class".equals(pd.getName())) {
                mapped.put("class", toConvert.getClass().getName());
                continue;
            }

            Method readMethod = pd.getReadMethod();
            if (readMethod == null) {
                continue;
            }

            if (readMethod.getParameterTypes().length > 0) {
                continue;
            }

            readMethod.setAccessible(true);

            Object result = readMethod.invoke(toConvert);
            result = convertObject(result, maxDepth - 1);
            if (!skipNulls || result != JSONObject.NULL) {
                mapped.put(pd.getName(), result);
            }
        }

        return mapped;
    }

}
