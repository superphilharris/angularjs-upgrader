package org.angularjsupgrader.model;

import org.angularjsupgrader.model.angularjs.JsFunction;

/**
 * Created by Philip Harris on 27/02/2020
 */
public abstract class AbstractComponent {
    public String templateUrl;
    public String template;
    public String controllerAs;
    public JsFunction controller;
}
