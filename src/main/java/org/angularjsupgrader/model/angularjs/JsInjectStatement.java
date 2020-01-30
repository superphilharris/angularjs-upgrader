package org.angularjsupgrader.model.angularjs;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 29/01/2020
 */
public class JsInjectStatement {
    public String functionName;
    public List<String> injects = new LinkedList<>();

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{functionName:'" + functionName + "'}";
    }
}
