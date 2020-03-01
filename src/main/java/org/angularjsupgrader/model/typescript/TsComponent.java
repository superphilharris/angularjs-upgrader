package org.angularjsupgrader.model.typescript;

import org.angularjsupgrader.model.angularjs.JsInjectable;

/**
 * Created by Philip Harris on 18/01/2020
 */
public class TsComponent extends AbstractTsClass {
    public JsInjectable controllerSourcedFrom;
    public String templateUrl; // TODO: change this to be set with the contents of the templateUrl
    public String template;
}
