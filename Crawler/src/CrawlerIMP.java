import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
public class CrawlerIMP implements Runnable {
    private HashSet<String> Vis;
    private Queue<String>Seed;
    private RobotParser RP;
    AtomicInteger idx;

    public CrawlerIMP(String path) {
        Vis=new HashSet<>();
        Seed=new LinkedList<>();
        idx=new AtomicInteger(0);
        RP=new RobotParser();
        readSeed(path);
    }

    public void StartCrawelling() {
        String curLink;
        Document doc=null;
        List<String>childLinks=null;
        while(true) {
            synchronized(this) {
                while(Seed.isEmpty()) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                curLink=Seed.poll();
            }
            if(!RobotParser.isSafe(curLink)) {
                System.out.println("NOT Safe!!! "+curLink);
                continue;
            }

            try {
                Connection con=Jsoup.connect(curLink).ignoreHttpErrors(true);
                doc = con.maxBodySize(Integer.MAX_VALUE).get();
                if(con.response().statusCode()==200  )
                {
                    childLinks=getLinks(doc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            synchronized(this) {
                DownloadPage(doc);
                for(String li:childLinks) {
                    if(Vis.add(li)) {
                        Seed.add(li);
                    }
                }
                notifyAll();
            }
        }
    }

    private List<String> getLinks(Document doc) {
        List<String> links=new ArrayList<>();
        Elements linksInside=doc.select("a[href]");
        for(Element element: linksInside) {
            String childLink=element.attr("abs:href");
            links.add(childLink);
        }
        return links;
    }
    void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }
    private void DownloadPage(Document doc) {
        try {
            int id=idx.getAndSet(idx.intValue()+1);
            File dir=new File("Crawler/Files/"+id);
            while(!dir.mkdir()){
                deleteDir(dir);
            }
            BufferedWriter buff=new BufferedWriter(new FileWriter("Crawler/Files/"+id+"/"+id+".html"));
            BufferedWriter linkbuff=new BufferedWriter(new FileWriter("Crawler/Files/"+id+"/"+"link.txt"));
            System.out.println(doc.baseUri());
            buff.write(doc.html());
            buff.close();
            linkbuff.write(doc.baseUri());
            linkbuff.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void readSeed(String path) {
        File file=new File(path);

        try {
            Scanner sc=new Scanner(file);
            String link;

            while (sc.hasNextLine()) {
                link=sc.nextLine();
                Seed.add(link);
                Vis.add(link);
            }

            sc.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        // TODO Auto-generated method stub
        StartCrawelling();
    }
}
