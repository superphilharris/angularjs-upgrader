package org.angularjsupgrader.model.typescript;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 25/01/2020
 */
public class TsRouting extends AbstractTsClass {
    public String sourcedFrom;
    public List<TsRoutingComponent> routingComponents = new LinkedList<>();
}
