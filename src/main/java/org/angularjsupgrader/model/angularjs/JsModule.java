package org.angularjsupgrader.model.angularjs;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 16/01/2020
 */
public class JsModule {
    public String name;
    public List<JsInjectable> injectables = new LinkedList<>();
    public String sourcedFrom;


    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{name:'" + name + "', injectables:[\n\t\t" + injectables.stream().map(Object::toString).collect(Collectors.joining(",\n\t\t")) + "\n\t]}";
    }
}
