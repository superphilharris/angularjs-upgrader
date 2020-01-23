package org.angularjsupgrader.model.typescript;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Philip Harris on 18/01/2020
 */
public abstract class AbstractTsModule {
    public List<TsModule> childModules = new LinkedList<>();
}
