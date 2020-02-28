package org.angularjsupgrader.model.typescript;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Harris on 25/01/2020
 */
public class TsRouting extends AbstractTsClass {
    public String sourcedFrom;
    public Map<String, TsComponent> pathToComponent = new HashMap<>();
}
