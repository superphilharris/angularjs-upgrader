package org.angularjsupgrader.model.angularjs;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 17/01/2020
 */
public class JsProgram {
    public List<JsFile> files = new LinkedList<>();

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{files:[\n" + files.stream().map(Object::toString).collect(Collectors.joining(",\n")) + "\n]}";
    }
}
