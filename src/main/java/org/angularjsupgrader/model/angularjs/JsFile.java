package org.angularjsupgrader.model.angularjs;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 16/01/2020
 */
public class JsFile extends AbstractJsFunctionWrapper {
    public String filename;
    public Map<String, JsModule> modules = new HashMap<>();


    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{filename:'" + filename + "', modules:[\n\t" + modules.values().stream().map(Object::toString).collect(Collectors.joining(",\n")) + "\n]}";
    }
}
