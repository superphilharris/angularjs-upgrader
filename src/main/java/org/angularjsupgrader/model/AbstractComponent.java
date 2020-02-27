package org.angularjsupgrader.model;

import org.angularjsupgrader.model.angularjs.JsFunction;
import org.angularjsupgrader.model.angularjs.JsStatementBranch;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Harris on 27/02/2020
 */
public abstract class AbstractComponent {
    public String templateUrl;
    public String template;
    public String controllerAs;
    public JsFunction controllerFunction;
    public String controllerInjectedName;
    public Map<String, JsStatementBranch> resolve = new HashMap<>();
}
