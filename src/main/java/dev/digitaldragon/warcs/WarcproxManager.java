package dev.digitaldragon.warcs;

import dev.digitaldragon.jobs.RunCommand;
import dev.digitaldragon.jobs.SimpleLogsHandler;

import javax.ws.rs.DELETE;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The WarcproxManager class is responsible for managing the WarcProx service.
 * It provides methods to start and stop the service, and automatically uploads
 * generated WARCs to the Internet Archive.
 */
@Deprecated
public class WarcproxManager {
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static RunCommand warcprox = null;
    public static void run() {
        executor.submit(() -> {
            File directory = new File("warcprox");
            if (!directory.exists() && !directory.mkdirs()) {
                System.out.println("Failed to create warcprox directory!!!");
                System.exit(1);
            }

            // -n wikibot-go: prefix for warcs
            // -s 16106127360: split size (15GB)
            // -z: compress warcs with gzip
            warcprox = new RunCommand("warcprox -n wikibot-go -s 16106127360 -z", null, directory, new SimpleLogsHandler());
            warcprox.run();
        });

        executor.submit(() -> {
            while (true) {
                try {
                    // sleep for 5 minutes
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                File directory = new File("warcprox/warcs");
                // find all warcs in the directory
                if (!directory.isDirectory()) {
                    continue;
                }
                if (directory.listFiles() == null) {
                    continue;
                }

                for (File file : directory.listFiles()) {
                    if (file.getName().endsWith(".warc.gz")) {
                        // upload the warc
                        System.out.println("Uploading " + file.getName() + "...");

                        //example warc name: wikibot-go-20230915015737696-00000-4hcng6ds.warc.gz
                        //extract the date and serial number (4hcng6ds) from the name
                        String[] split = file.getName().split("-");
                        String date = split[2];
                        String serial = split[4].split("\\.")[0];

                        //upload the warc
                        uploadWarc(file, date, serial);
                    }
                }
            }
        });
    }

    public static void stopCleanly() {
        if (warcprox != null) {
            // send ^C to warcprox
            System.out.println("Stopping warcprox...");
            warcprox.getProcess().destroy();
        }
    }

    private static void uploadWarc(File file, String date, String serial) {
        String identifier = "wikibot-go-" + date + "-" + serial;
        String uniqueId = date + "-" + serial;
        String title = "WikiBot WARCs: " + uniqueId;
        String description = "WikiBot WARC data for " + uniqueId +  ", generated via warcprox and a <a href=\\\"https://github.com/digitaldwagon/wikiteam3\\\" rel=\\\"nofollow\\\">modified version of WikiTeam3";
        String command = "ia upload " + identifier + " " + file.getAbsolutePath() +
                " --retries 10 " +
                "--metadata=\"mediatype:web\" " +
                "--metadata=\"subject:WikiBot; wikidump; WARC; warcprox\" " +
                "--metadata=\"description:" + description + "\" " +
                "--metadata=\"title:" + title + "\"";

        // upload the warc
        RunCommand upload = new RunCommand(command, null, file.getParentFile(), new SimpleLogsHandler());
        upload.run();
        try {
            int exitCode = upload.getProcess().waitFor();
            if (exitCode == 0) {
                System.out.println("Finished uploading " + file.getName() + "!");
                // add .uploaded to warc name
                file.renameTo(new File(file.getParentFile(), file.getName() + ".uploaded"));
            } else {
                System.out.println("Failed to upload " + file.getName() + "! Exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static File getWarcproxCa() {
        File directory = new File("warcprox");
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(".pem")) {
                return file;
            }
        }
        return null;
    }


}
