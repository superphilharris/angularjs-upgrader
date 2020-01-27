package org.angularjsupgrader;

import com.google.common.base.CaseFormat;
import org.angularjsupgrader.model.typescript.*;

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
        return getNewDependency(jsName, getServicePathFromSibling(dependencyFileName, tsClass, ""));
    }

    private String getServicePathFromSibling(String dependencyFileName, AbstractTsClass tsClass, String currentPath) {
        String servicePath = getDependentServiceFromChildren(dependencyFileName, tsClass.parent, "./" + currentPath, tsClass);
        if (servicePath != null) return servicePath;

        for (TsModule siblingModule : tsClass.parent.childModules) {
            String path = getDependentServiceFromChildren(dependencyFileName, siblingModule, "./" + currentPath + siblingModule.name, null);
            if (path != null) {
                return path;
            }
        }

        AbstractTsModule parentModule = tsClass.parent.parent;
        TsModule currentModule = tsClass.parent;
        while (parentModule instanceof TsModule) {
            currentPath = "../" + currentPath;
            for (TsModule tsModule : parentModule.childModules) {
                if (tsModule != currentModule) {
                    String path = getDependentServiceFromChildren(dependencyFileName, tsModule, currentPath + tsModule.name + "/", null);
                    if (path != null) return path;
                }
            }
            currentModule = (TsModule) parentModule;
            parentModule = ((TsModule) parentModule).parent;
        }
        return null;
    }

    private String getDependentServiceFromChildren(String searchForService, TsModule moduleToLookin, String currentPath, AbstractTsClass classToExclude) {
        for (TsService service : moduleToLookin.services) {
            if (service != classToExclude) {
                if (searchForService.equals(service.name)) {
                    return currentPath + service.name + ".service";
                }
            }
        }
        for (TsModule childModule : moduleToLookin.childModules) {
            String path = getDependentServiceFromChildren(searchForService, childModule, currentPath + childModule.name + "/", classToExclude);
            if (path != null) return path;
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
