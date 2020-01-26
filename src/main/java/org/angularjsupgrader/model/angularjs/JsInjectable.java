package org.angularjsupgrader.model.angularjs;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 18/01/2020
 */
public class JsInjectable {
    public String injectableName;
    public String functionName; // If function is defined with a string, then the upgrader will populate the function
    public JsFunction function; // If function is defined inline, then the visitor will populate the function
    public InjectableType type;
    public List<String> injections = new LinkedList<>();

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{injectableName:'" + injectableName + "', type:" + type + "}";
    }
}
