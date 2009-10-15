package com.goodworkalan.cups;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import com.goodworkalan.glob.Find;
import com.goodworkalan.go.go.Argument;
import com.goodworkalan.go.go.Artifact;
import com.goodworkalan.go.go.Command;
import com.goodworkalan.go.go.Environment;
import com.goodworkalan.go.go.Task;

/**
 * Convert a Maven repository into a Jav-a-Go-Go library by converting Maven POM
 * files into Jav-a-Go-Go dependency files.
 */
@Command(parent = CupsTask.class)
public class FlattenTask extends Task {
    private boolean force;
    
    @Argument
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Recurse the Maven repository structure converting Maven POM files into
     * Jav-a-Go-Go dependency files if the Jav-a-Go-Go file does not already
     * exist.
     * 
     * @param environment
     *            The execution environment.
     */
    @Override
    public void execute(Environment environment) {
        if (force) System.out.println("WILL FORCE!");
        LinkedList<String> args = new LinkedList<String>(environment.part.getRemaining());
        String repository = args.removeFirst();
        File directory = new File(repository);
        if (directory.isDirectory()) {
            if (args.isEmpty()) {
                flatten(environment, directory, new Find().include("**/*.pom"));
            } else {
                Find find = new Find();
                for (String arg : args) {
                    Artifact artifact = new Artifact(arg);
                    if (artifact != null) {
                        find.include(artifact.getPath("pom"));
                    }
                }
                flatten(environment, directory, find);
            }
        }
    }
    
    private void flatten(Environment environment, File directory, Find find) {
        PomReader reader = new PomReader(null);
        for (String file : find.find(directory)) {
            Artifact artifact = Artifact.parse(new File(directory, file));
            if (artifact != null) {
                File deps = new File(directory, artifact.getPath("dep"));
                if (artifact.getPath("pom").equals(file.toString()) && (force || !deps.exists())) {
                    flatten(environment, reader, artifact, deps);
                }
            }
        }
    }

    /**
     * Write the artifact dependencies to the given dependency file. This method
     * was extracted to test I/O failure.
     * 
     * @param environment
     *            The execution environment.
     * @param reader
     *            The POM reader.
     * @param artifact
     *            The artifact to whose dependencies will be read from a Maven
     *            POM.
     * @param deps
     *            The dependency file.
     */
    void flatten(Environment environment, PomReader reader, Artifact artifact, File deps) {
        try {
            FileWriter writer = new FileWriter(deps);
            for (Artifact dependency : reader.getImmediateDependencies(artifact)) {
                System.out.println(dependency);
                writer.write("+ ");
                writer.write(dependency.getGroup());
                writer.write(" ");
                writer.write(dependency.getName());
                writer.write(" ");
                writer.write(dependency.getVersion());
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            deps.delete();
            environment.err.println("Unable to flatten POM for artifact " + artifact.toString());
            e.printStackTrace(environment.err);
        }
    }
}