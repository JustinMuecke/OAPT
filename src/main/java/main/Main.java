package main;

import fusion.oapt.algorithm.partitioner.SeeCOnt.Findk.FindOptimalCluster;
import fusion.oapt.algorithm.partitioner.SeeCOnt.ModuleEvaluation;
import fusion.oapt.general.analysis.GeneralAnalysis;
import fusion.oapt.general.cc.Controller;
import fusion.oapt.general.cc.Coordinator;
import org.apache.jena.ontology.OntModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Main {



    public static void main(String[] args) throws IOException {
        List<File> filesInFolder = null;
        List<String> completedOntologies = findCompletedOntologies("../data/ont_modules");
        try {
            filesInFolder = Files.walk(Paths.get("../data/ontologies"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("The total number of owl files: " + filesInFolder.size());
        File Ofile = new File("src/resources/merge/analysis.csv");
        File pfile = Ofile.getParentFile();
        if (!pfile.exists()) {
            pfile.mkdir();
        }

        FileWriter fw = new FileWriter(Ofile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        String header = "Ontology, No. of modules ,Time ,HOMO ,HEMO ,rel. Size";
        bw.write(header);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (File file : filesInFolder) {
            if(completedOntologies.contains(file.getName())) continue;
            executor.submit(() -> {
                try {
                    processFile(file, bw);
                } catch (IOException e) {
                    e.printStackTrace();
                    logError(file, e);
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        bw.close();
    }
    private static List<String> findCompletedOntologies(String path) {
        List<String> filesInFolder = null;
        try (Stream<Path> fif = Files.walk(Paths.get(path))){
            filesInFolder = fif
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .map(File::getName)
                    .map(name -> name.split("_Module")[0]+".owl")
                    .distinct()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<String>();
        }
        return filesInFolder;
    }
    private static void processFile(File f, BufferedWriter bw) throws IOException {
        String path = f.getPath();
        double start = System.currentTimeMillis();
        Controller con = new Controller(path);
        FindOptimalCluster OP = new FindOptimalCluster(con.MB);
        int NumCH = OP.FindOptimalClusterFunc();
        Coordinator.KNumCH = NumCH;
        con.InitialRun_API("SeeCOnt", Coordinator.KNumCH);
        double end = (System.currentTimeMillis() - start) * 0.001;
        ModuleEvaluation moduleEvaluation = new ModuleEvaluation(con.getModelBuild(), con.getClusters());
        moduleEvaluation.Eval_SeeCont();
        String content = "\n" + f.getName() + "," + NumCH + "," + end + "," + moduleEvaluation.getHoMO() + "," + moduleEvaluation.getHEMo() + "," + moduleEvaluation.getRS();
        synchronized (bw) {
            bw.write(content);
            bw.flush();
        }
        System.out.println(NumCH + ", the file: " + f.getName() + ", time: " + end);
    }

    private static void logError(File file, Exception e) {
        try (BufferedWriter bwe = new BufferedWriter(new FileWriter("src/resources/merge/error.csv", true))) {
            bwe.write(file.getName() + ":\t" + e.toString());
            bwe.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
