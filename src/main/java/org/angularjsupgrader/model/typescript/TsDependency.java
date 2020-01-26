package org.angularjsupgrader.model.typescript;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 27/01/2020
 */
public class TsDependency {
    public String name;
    public String packagePath;
    public List<TsDependency> dependencies = new LinkedList<>();
}
