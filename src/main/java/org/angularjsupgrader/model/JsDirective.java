package org.angularjsupgrader.model;

import org.angularjsupgrader.model.angularjs.JsInjectable;
import org.angularjsupgrader.model.angularjs.JsStatementBranch;
import org.angularjsupgrader.model.angularjs.RestrictType;
import org.angularjsupgrader.model.angularjs.ScopeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Harris on 26/02/2020
 */
public class JsDirective extends AbstractComponent {
    public JsInjectable originalInjectable;
    public RestrictType restrictType;
    public JsStatementBranch linkFunction;
    public Map<String, ScopeType> inputOutpus = new HashMap<>();
    public boolean bindToController = false;
    public boolean transclude = false;
}
