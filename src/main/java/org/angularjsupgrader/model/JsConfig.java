package org.angularjsupgrader.model;

import org.angularjsupgrader.model.angularjs.JsInjectable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 28/02/2020
 */
public class JsConfig {
    public JsInjectable originalInjectable;
    public List<JsRoutePage> pages = new LinkedList<>();
}
