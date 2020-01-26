package org.angularjsupgrader;

import com.google.common.base.CaseFormat;
import org.angularjsupgrader.model.typescript.AbstractTsClass;
import org.angularjsupgrader.model.typescript.TsDependency;
import org.angularjsupgrader.model.typescript.TsService;

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
        String dependencyFileName = camelToKebab(jsName.replace("Service", ""));
        return getNewDependency(jsName, getServicePathFromSibling(dependencyFileName, tsClass, "./"));
    }

    private String getServicePathFromSibling(String dependencyFileName, AbstractTsClass tsClass, String currentPath) {
        for (TsService service : tsClass.parent.services) {
            if (service != tsClass) {
                if (dependencyFileName.equals(service.name)) {
                    return currentPath + service.name + ".service.ts";
                }
            }
        }

        return null;
    }


    private String camelToKebab(String camelCase) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCase);
    }

    private TsDependency getNewDependency(String name, String packagePath) {
        TsDependency tsDependency = new TsDependency();
        tsDependency.name = name;
        tsDependency.packagePath = packagePath;
        return tsDependency;
    }
}
