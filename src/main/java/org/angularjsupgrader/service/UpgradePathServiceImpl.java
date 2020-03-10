package org.angularjsupgrader.service;

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

        libraryDependencyMap.put("$window", getNewDependency("Window", "Location, ActivatedRoute, or Router?"));
        libraryDependencyMap.put("$q", getNewDependency("Promise", null));
        libraryDependencyMap.put("$timeout", getNewDependency("setTimeout", "setTimeout directly"));

        //libraryDependencyMap.put("$filter", getNewDependency("$filter", "$filter")); -> used to be the way to access pipes
        // libraryDependencyMap.put("$scope"); TODO: convert $scope to Input/Output
        // $controller
        // $injector
        // $resource
        // $upload -> can now use httpClient, with form/multipart

        // NGX Bootstrap
        libraryDependencyMap.put("$uibModal", getNewDependency("BsModalService", "ngx-bootstrap/modal"));
        libraryDependencyMap.put("$modal", libraryDependencyMap.get("$uibModal"));
        libraryDependencyMap.put("$uibModalInstance", getNewDependency("BsModalRef", "ngx-bootstrap/modal"));
        libraryDependencyMap.put("$modalInstance", libraryDependencyMap.get("$uibModalInstance"));
        libraryDependencyMap.put("toastrService", getNewDependency("ToastrService", "ngx-toastr")); // TODO: this is a custom service
    }

    public TsDependency getServiceDependency(String jsName, AbstractTsClass tsClass) {
        if (libraryDependencyMap.containsKey(jsName)) {
            return libraryDependencyMap.get(jsName);
        }
        String dependencyFileName = camelToKebab(jsName.replace("Service", ""));
        return getNewDependency(jsName, getTsFilePathFromSibling(dependencyFileName, tsClass, ".service"));
    }


    private String getTsFilePathFromSibling(String dependencyFileName, AbstractTsClass tsClass, String tsFileSuffix) {
        String currentPath = "";
        String servicePath = getTsFilePathFromChildren(dependencyFileName, tsClass.parent, "./" + currentPath, tsClass, tsFileSuffix);
        if (servicePath != null) return servicePath;

        for (TsModule siblingModule : tsClass.parent.childModules) {
            String path = getTsFilePathFromChildren(dependencyFileName, siblingModule, "./" + currentPath + siblingModule.name, null, tsFileSuffix);
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
                    String path = getTsFilePathFromChildren(dependencyFileName, tsModule, currentPath + tsModule.name + "/", null, tsFileSuffix);
                    if (path != null) return path;
                }
            }
            currentModule = (TsModule) parentModule;
            parentModule = ((TsModule) parentModule).parent;
        }
        return null;
    }

    private String getTsFilePathFromChildren(String searchForService, TsModule moduleToLookin, String currentPath, AbstractTsClass classToExclude, String tsFileSuffix) {
        for (TsService service : moduleToLookin.services) {
            if (service != classToExclude) {
                if (searchForService.equals(service.name)) {
                    return currentPath + service.name + tsFileSuffix;
                }
            }
        }
        for (TsModule childModule : moduleToLookin.childModules) {
            String path = getTsFilePathFromChildren(searchForService, childModule, currentPath + childModule.name + "/", classToExclude, tsFileSuffix);
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
