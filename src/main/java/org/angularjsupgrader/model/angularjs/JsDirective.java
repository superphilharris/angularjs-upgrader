package org.angularjsupgrader.model.angularjs;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Harris on 26/02/2020
 */
public class JsDirective {
    public String templateUrl;
    public String template;
    public String controllerAs;
    public JsFunction controller;
    public RestrictType restrictType;
    public JsStatementBranch linkFunction;
    public Map<String, ScopeType> inputOutpus = new HashMap<>();
    public boolean bindToController = false;
    public boolean transclude = false;
}
