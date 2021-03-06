/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.modules;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.modules.ModulePackager.ModuleFiles;
import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Package a module from a project and launch it. The system property {@link #MODULE_DIR_PROP} must
 * be defined.
 * 
 * @author Sylvain CUAZ
 * @see ModulePackager
 */
public class ModuleLauncher {
    /**
     * Required system property, it must point to a directory with module classes in bin/, this
     * class will put the packaged module in the dist/ subdirectory.
     */
    public static final String MODULE_DIR_PROP = "module.dir";
    /**
     * System property to use if the module properties files isn't "module.properties" (this
     * property is evaluated relative to {@link #MODULE_DIR_PROP}).
     */
    public static final String MODULE_PROPS_FILE_PROP = "module.propsFile";

    static ModuleFiles createModuleFiles(String moduleDirPath) throws IOException {
        return new ModuleFiles(new File(moduleDirPath), System.getProperty(MODULE_PROPS_FILE_PROP));
    }

    public static void main(String[] args) throws IOException {
        final ModuleFiles moduleFiles = createModuleFiles(System.getProperty(MODULE_DIR_PROP));
        final boolean launchFromPackage = !Boolean.getBoolean("module.fromProject");

        // always update dist/ to avoid out of date problems
        final File jar = ModulePackager.createDist(moduleFiles);
        // to avoid out of date modules from OpenConcerto (e.g. when launching this module, the jars
        // of MODULES_DIR are used for dependencies)
        FileUtils.mkdir_p(Gestion.MODULES_DIR);
        FileUtils.copyFile(jar, new File(Gestion.MODULES_DIR, jar.getName()));

        final PropsModuleFactory factory;
        if (launchFromPackage) {
            factory = new JarModuleFactory(jar);
        } else {
            factory = new RuntimeModuleFactory(moduleFiles.getPropertiesFile());
            try {
                Class.forName(factory.getMainClass());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Module classes are not in the classpath (they should be in " + moduleFiles.getClassesDir() + ")", e);
            }
        }

        Gestion.main(args);
        // add after main() otherwise we could be overwritten by an older jar
        ModuleManager.getInstance().addFactoryAndStart(factory, false);
    }
}
