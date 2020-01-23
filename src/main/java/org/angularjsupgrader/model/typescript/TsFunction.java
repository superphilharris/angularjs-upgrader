package org.angularjsupgrader.model.typescript;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 15/01/2020
 */
public class TsFunction {
    public String name;
    public List<String> arguments = new LinkedList<>();
    public List<TsFunction> childFunctions = new LinkedList<>();
}
