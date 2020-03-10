package org.angularjsupgrader.service.upgrader;

import com.google.common.base.CaseFormat;

/**
 * Created by Philip Harris on 9/03/2020
 */
public class StringServiceImpl {

    public String camelToKebab(String camelCase) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCase);
    }

    public String trimQuotes(String untrimmed) {
        return untrimmed.replace("\"", "").replace("'", "");
    }

}
