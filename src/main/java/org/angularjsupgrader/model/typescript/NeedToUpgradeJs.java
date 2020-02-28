package org.angularjsupgrader.model.typescript;

import org.angularjsupgrader.model.JsConfig;
import org.angularjsupgrader.model.JsDirective;
import org.angularjsupgrader.model.angularjs.JsFile;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 28/02/2020
 */
public class NeedToUpgradeJs {
    public boolean isUpgraded = false;
    public List<JsDirective> directives = new LinkedList<>();
    public List<JsConfig> configs = new LinkedList<>();
    public JsFile sourcedFrom;
}
