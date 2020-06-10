package org.abstracthorizon.extend.server.support;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;

public class Dump {


    public static void outputCore(DeploymentManager manager) {
        try {
            FileWriter out = new FileWriter("extend-core.out");
            try {
                PrintWriter p = new PrintWriter(out);
                outputCore(p, manager);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void outputCore(PrintWriter p, DeploymentManager manager) {
        if (manager != null) {
            p.println("Deployed modules hash:");
            Map<ModuleId, Module> deployedModules = manager.getDeployedModules();
            for (ModuleId key : deployedModules.keySet()) {
                Module m = deployedModules.get(key);
                p.println("  " + key.toString() + " -> " + m.getWorkingLocation() + "(" + System.identityHashCode(m) + ")");
            }
            p.println();
            p.println("Modules:");
            for (Module m : deployedModules.values()) {
                p.println("  " + m.getModuleId() + "(" + System.identityHashCode(m) + ")");
                p.println("    Depends on: ");
                for (Module d : m.getDependsOn()) {
                    p.println("      " + d.getModuleId() + "(" + System.identityHashCode(d) + ")");
                }
                p.println("    Depend on this: ");
                for (Module d : m.getDependOnThis()) {
                    p.println("      " + d.getModuleId() + "(" + System.identityHashCode(d) + ")");
                }
            }
        } else {
            p.println("No manager defined yet!");
        }
        p.flush();
    }
}
