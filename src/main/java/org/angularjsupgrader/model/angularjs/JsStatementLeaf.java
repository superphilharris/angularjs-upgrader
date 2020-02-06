package org.angularjsupgrader.model.angularjs;

/**
 * Created by Philip Harris on 3/02/2020
 */
public class JsStatementLeaf extends AbstractJsStatementPart {
    public String text;

    @Override
    public String toString() {
        return text;
    }
}
