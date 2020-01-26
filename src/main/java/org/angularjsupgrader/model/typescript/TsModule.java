package org.angularjsupgrader.model.typescript;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 15/01/2020
 */
public class TsModule extends AbstractTsModule {
    public String name; // kebab-case name of module
    public String sourcedFrom; // what file the original angularJs came from
    public List<TsComponent> components = new LinkedList<>();
    public List<TsService> services = new LinkedList<>();
    public TsRouting routing = new TsRouting();
    public AbstractTsModule parent;

    @Override
    public String toString() {
        String components = this.components.stream().map(Object::toString).collect(Collectors.joining(",\n\t"));
        return getClass().getSimpleName() + ":{name:'" + this.name + "'" +
                ((this.childModules.size() > 0) ?
                        ", children:[\n" + this.childModules.stream().map(Objects::toString).collect(Collectors.joining(",\n")) + "\n]" :
                        "") +
                ((components.length() > 0) ? ", components: [\n\t" + components + "\n]}" : "}");
    }
}
