package org.angularjsupgrader.model.typescript;

import java.util.stream.Collectors;

/**
 * Created by Philip on 15/01/2020
 */
public class TsProgram extends AbstractTsModule {

    @Override
    public String toString() {
        return this.childModules.stream().map(Object::toString).collect(Collectors.joining("\n"));
    }
}
