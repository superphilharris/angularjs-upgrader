package org.angularjsupgrader.model.angularjs;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 18/01/2020
 */
public class JsFunction extends AbstractJsFunctionWrapper {
    public String functionName;
    public JsFunction parent;
    public List<String> arguments = new LinkedList<>();
    public List<JsStatementBranch> statements = new LinkedList<>();

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{name:'" + functionName + "'}";
    }
}
