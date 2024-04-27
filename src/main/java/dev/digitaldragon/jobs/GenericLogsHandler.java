package dev.digitaldragon.jobs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A StringLogHandler that writes logs to log.txt and a Discord channel.
 */
@Deprecated
public class GenericLogsHandler implements StringLogHandler {
    private final Job job;

    public GenericLogsHandler(Job job) {
        this.job = job;

        //onMessage("----- Bot: Logs manager init -----");
    }

    @Deprecated
    public synchronized void onMessage(String message) {
        job.log(message);
        /*WikiBot.getBus().post(new JobLogEvent(job, message));
        try {
            //LogWebsocket.sendLogMessageToClients(job.getId(), message);

            //System.out.println(message);
            //writeLineToFile(new File(job.getDirectory(), "log.txt"), message);

            if (message.contains("https://archive.org/details/") && message.contains(" ")) {
                String[] split = message.split(" ");
                for (String s : split) {
                    if (s.contains("https://archive.org/details/")) {
                        job.setArchiveUrl(s);
                        break;
                    }
                }
            } else if (message.contains("https://archive.org/details/")) {
                job.setArchiveUrl(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }//*/
    }

    private static void writeLineToFile(File file, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(line);
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public void end() {

    }
}
