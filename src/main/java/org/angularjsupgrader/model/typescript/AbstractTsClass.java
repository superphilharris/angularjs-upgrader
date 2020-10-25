package org.angularjsupgrader.model.typescript;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 22/01/2020
 */
public abstract class AbstractTsClass {
    public String name;
    public List<TsFunction> functions = new LinkedList<>();
    public List<String> dependencies = new LinkedList<>();
    public TsModule parent = null;
    public LinkedList<TsStatement> initialization = new LinkedList<>();
    public List<String> upgradeErrors = new LinkedList<>();

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{name:'" + name + "'}";
    }
}
