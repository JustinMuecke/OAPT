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
import java.util.List;
import java.util.stream.Collectors;



public class Main {

    private  String filepath = null;
    private ArrayList<OntModel> models = null;

    public static void main(String[]args) throws IOException
    {
        System.out.println("WORKING");
        System.out.println(System.getProperty("user.dir"));
        final String filepath = "../data/ontologies/";
        List<OntModel> models=new ArrayList<OntModel>();
        List<File>filesInFolder=new ArrayList<File>();
        System.out.println(System.getProperty("java.class.path"));
        read(filepath, models);
    }




    public static void read(String file, List<OntModel> models) throws IOException
    {
        List<File> filesInFolder = null;

        try {
            filesInFolder = Files.walk(Paths.get(file))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("the total number number of owl files:\t"+filesInFolder.size());
        Controller con;
        ModuleEvaluation moduleEvaluation;
        double start=0, end=0;
        String content = "Ontology, No. of modules ,Time ,HOMO ,HEMO ,rel. Size";
        File Ofile = new File("src/resources/merge/analysis.csv");
        File pfile=Ofile.getParentFile();
        if(!pfile.exists())
        {
            pfile.mkdir();
        }
        FileWriter fw = new FileWriter(Ofile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        for(int i=0;i<1;i++)
        {
            try {
                File f = filesInFolder.get(i);
                String path = f.getPath();
                System.out.println(i + ",the file:\t" + path);
                start = System.currentTimeMillis();
                con = new Controller(path);
                models.add(con.getOntModel());
                FindOptimalCluster OP = new FindOptimalCluster(con.MB);
                int NumCH = OP.FindOptimalClusterFunc();
                Coordinator.KNumCH = NumCH;
                //con.runPartition();
                con.InitialRun_API("SeeCOnt", Coordinator.KNumCH);
                //ArrayList<OntModel> modules=Coordinator.getModules();
                end = (System.currentTimeMillis() - start) * .001;
                content = "\n" + f.getName() + "," + NumCH + "," + end;
                moduleEvaluation = new ModuleEvaluation(con.getModelBuild(), con.getClusters());
                moduleEvaluation.Eval_SeeCont();
                content = "\n" + f.getName() + "," + NumCH + "," + end + "," + moduleEvaluation.getHoMO() + "," + moduleEvaluation.getHEMo() + "," + moduleEvaluation.getRS();
                //stream.write(content.getBytes());
                bw.write(content);
                //System.out.println(moduleEvaluation.getHEMo()+",number of clusters---"+moduleEvaluation.getHoMO()+","+moduleEvaluation.getRS());
                System.out.println(NumCH + ",the file:\t" + f.getName() + ",time \t" + end);
            } catch (Exception e){
                File file_error = new File("src/resources/merge/error.csv");
                FileWriter fwe = new FileWriter(file_error);
                BufferedWriter bwe = new BufferedWriter(fwe);
                bwe.write(filesInFolder.get(i).getName() + ":\t" + e.toString());
            }
        }
        //stream.close();
        bw.close();
        //System.out.println("the number of models:\t"+models.size());
    }


    public void modularize(List<File> filesInFolder) throws IOException
    {
        GeneralAnalysis GA;
        String content = "Ontology,No. calss,No. of total class ,No. of sub ,No. of Prop,"+",No. of object Pro. ,,No. of data prop";
        File file = new File("src/resources/results/analysis.csv");
        File pfile=file.getParentFile();
        if(!pfile.exists())
        {
            pfile.mkdir();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);

        for(int i=0;i<models.size();i++)
        {
            File f=filesInFolder.get(i);
            OntModel om=models.get(i);
            GA=new GeneralAnalysis(om);
            GA.computeStatics();
            content="\n"+f.getName()+","+GA.getNumClass()+","+GA.getTotnumClass()+","+GA.getNumClass()+","+GA.getNumProp()+","+GA.getNumObectPro()+","+GA.getNumDataPro();
            bw.write(content);
        }
        bw.close();
    }



    public OntModel getOntModel1()
    {
        return models.get(0);
    }

}
