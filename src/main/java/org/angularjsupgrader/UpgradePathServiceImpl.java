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
        libraryDependencyMap.put("$location", getNewDependency("Location", "@angular/common"));
        libraryDependencyMap.put("$routeParams", getNewDependency("ActivatedRoute", "@angular/router"));
        libraryDependencyMap.put("$window", getNewDependency("Window", "Dont Exist in Angular"));
        //libraryDependencyMap.put("$filter", getNewDependency("$filter", "$filter")); -> used to be the way to access pipes
        // libraryDependencyMap.put("$scope"); TODO: convert $scope to Input/Output
        // $q
        // $controller
        // $timeout
        // $injector
        // $resource
        // $upload

        // NGX Bootstrap
        libraryDependencyMap.put("$uibModal", getNewDependency("BsModalService", "ngx-bootstrap/modal"));
        libraryDependencyMap.put("$modal", libraryDependencyMap.get("$uibModal"));
        libraryDependencyMap.put("$uibModalInstance", getNewDependency("BsModalRef", "ngx-bootstrap/modal"));
        libraryDependencyMap.put("$modalInstance", libraryDependencyMap.get("$uibModalInstance"));
        libraryDependencyMap.put("toastrService", getNewDependency("ToastrService", "ngx-toastr")); // TODO: this is a custom service
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
