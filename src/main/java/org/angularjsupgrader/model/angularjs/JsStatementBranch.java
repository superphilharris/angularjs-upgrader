package org.angularjsupgrader.model.angularjs;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 3/02/2020
 */
public class JsStatementBranch extends AbstractJsStatementPart {
    public List<AbstractJsStatementPart> subParts = new LinkedList<>();

    @Override
    public String toString() {
        return subParts.stream().map(Objects::toString).collect(Collectors.joining(" "));
    }
}
