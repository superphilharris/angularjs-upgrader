package org.angularjsupgrader;

import org.angularjsupgrader.model.typescript.AbstractTsClass;
import org.angularjsupgrader.model.typescript.TsDependency;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Harris on 27/01/2020
 */
public class UpgradePathServiceImpl {

    private final Map<String, TsDependency> libraryDependencyMap;

    public UpgradePathServiceImpl() {
        this.libraryDependencyMap = new HashMap<>();
        libraryDependencyMap.put("$http", getNewDependency("HttpClient", "@angular/common/http"));
    }

    public TsDependency getDependency(String jsName, AbstractTsClass tsClass) {
        if (libraryDependencyMap.containsKey(jsName)) {
            return libraryDependencyMap.get(jsName);
        }
        return getNewDependency(jsName, null);
    }

    private TsDependency getNewDependency(String name, String packagePath) {
        TsDependency tsDependency = new TsDependency();
        tsDependency.name = name;
        tsDependency.packagePath = packagePath;
        return tsDependency;
    }
}
