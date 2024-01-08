package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;

import static spark.Spark.*;

public class Dashboard {
    public static void register() {
        index();
        cards();
    }


    private static void index() {
        get("/", (request, response) -> {
            response.type("text/html");
            StringBuilder sb = new StringBuilder();
            //Did someone say HTML in Java? :D
            sb.append("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                    	<title>Wikibot Dashboard</title>
                    	<link rel="preconnect" href="https://fonts.googleapis.com">
                    	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    	<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@500&family=Ubuntu+Mono&display=swap" rel="stylesheet">
                    	                    	
                    	<meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        
                    	<style>
                    	body {
                    		width: 100vw;
                    		height: 100vw;
                    		margin: 0;
                    		background-color: #0c0221;
                    		color: #FFFFFF;
                    		font-family: 'JetBrains Mono', monospace;
                    	}
                    	h1 {
                    		font-weight: 500;
                    	}
                    	h3 {
                    		font-weight: 300;
                    	}
                    	a:link {color:#91a7ff;}
                    	a:visited {color:#91a7ff;}
                    	a:hover {color:#91a7ff;}
                    	a:active {color:#91a7ff;}
                    	
                    	.wikibot-intro {
                    		font-weight: 300;
                    		border: 2px solid #212780;
                    		border-radius: 10px;
                    		padding-inline: 20px;
                    		padding-block: 10px;
                    		width: 50vw;
                    		margin-inline: auto;
                    		margin-top: 50px;
                    	}
                    	
                    	.job-container {
                    		font-family: 'Ubuntu Mono', monospace;
                    		font-weight: 200;
                    		border: 2px solid #212780;
                    		border-radius: 10px;
                    		padding-inline: 20px;
                    		padding-block: 5px;
                    		width: 50vw;
                    		margin-inline: auto;
                    		margin-top: 50px;
                    		background-color: #000000;
                    	}
                    	.job-url {
                    		margin: 0;
                    		display: inline;
                    	}
                    	.job-details {
                    		display: inline;
                    		margin-inline: 5px;
                    	}
                    	.job-logs {
                    		white-space: pre-wrap;
                    		height: 300px;
                    		overflow-y: scroll;
                    		overscroll-behavior-y: contain;
                    		scroll-snap-type: y proximity;
                    	}
                    	@media screen and (max-width: 800px) {
                    		.wikibot-intro {
                    			width: 80vw;
                    		}
                    	}
                    </style>
                    </head>
                    <body>
                    	<noscript> <p>Some features of this dashboard require JavaScript (like showing logs and fetching new jobs.)</p><br/><br/><p>(hi JAA.)</p></noscript>
                    	<script>
                    	   const jobCards = [];
                           const logSocket = new WebSocket('wss://wikibot.digitaldragon.dev/api/logfirehose');
                           
                           logSocket.addEventListener('message', async (event) => {
                               const data = JSON.parse(event.data);
                           
                               // the WebSocket message format is { "jobId": "12345", "logLine": "Some log message" }
                               const jobId = data.jobId;
                               const logLine = data.logLine;
                               // Add log message to the log box of the job card.
                               const logBox = document.getElementById(jobId);
                               logBox.textContent += logLine + '\\n'; // Use '\\n' to add line breaks
                               logBox.scrollTop = logBox.scrollHeight; // Auto-scroll to the bottom
                           });
                           
                            const jobSocket = new WebSocket('wss://wikibot.digitaldragon.dev/api/jobevents');
                            jobSocket.addEventListener('message', async (event) => {
                                const data = JSON.parse(event.data);
                                const jobId = data.jobId;
                                const eventType = data.event;
                                if (eventType === 'QUEUED') {
                                    // get the job card html from /cards/:id
                                    const response = await fetch('/cards/' + jobId);
                                    const html = await response.text();
                                    
                                    // add the job card to the page
                                    const jobContainer = document.querySelector('.jobs');
                                    jobContainer.innerHTML += html;
                                }
                            });
                            
                    	</script>
                    	<div>
                    	<h1 style="text-align: center;">Wikibot Dashboard</h1>
                    """);
                    sb.append(String.format("<h3 style=\"text-align: center;\">Tracking %s running and %s queued jobs.</h3>", JobManager.getActiveJobs().size(), JobManager.getQueuedJobs().size()));
                    sb.append("""
                    		<div class="wikibot-intro">
                    			<details>
                    				<summary style="font-size: 20px; text-align: center;">What is Wikibot?</summary>
                    				<span>
                    					<h3>Wikibot archives wikis, from Wikipedia to the smallest wikis.</h2>
                    					<p> Based on <a href="https://github.com/saveweb/wikiteam3">a fork</a> of the original <a href="https://github.com/wikiteam/wikiteam">WikiTeam</a> software,
                    					and the <a href="https://github.com/saveweb/dokuwiki-dumper">DokuWikiDumper</a> project, Wikibot makes static dumps of wikis to be uploaded to the
                    					<a href="https://archive.org/">Internet Archive</a> for preservation.<br/></p>
                    					<p>To use it, stop by <a href="irc://irc.hackint.org/wikibot">#wikibot</a> on irc.hackint.org <a href="https://webirc.hackint.org/#irc://irc.hackint.org/wikibot">(click for webirc)</a>. 
                    					More information: <a href="https://wiki.archiveteam.org/index.php/Archiveteam:IRC">ArchiveTeam's IRC</a>.
                    				    Use the !mw and !dw commands to start a job. For full information, see <a href="https://cdn.digitaldragon.dev/wikibot/help.html">the help page</a>.</p>
                    					
                    					<p>Wikibot is operated by DigitalDragon. Site operators that need to contact us can join our IRC channel, or reach out directly via email: wikibot (at) digitaldragon (dot) dev</p>
                    				</span>
                    			</details>
                    		</div>
                    	</div>
                    	<div class="jobs">
                    	""");
                for (Job job : JobManager.getActiveJobs()) {
                    sb.append(getCard(job));
                }

            sb.append("</div></body>");
            return sb.toString();
        });

    }

    private static void cards() {
        get("/cards/*", (request, response) -> {
            response.type("text/html");
            String id = request.splat()[0];
            Job job = JobManager.get(id);
            if (job == null) {
                return "Job not found!";
            }
            return getCard(job);
        });
    }

    private static String getCard(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"job-container\">");
        if (job.getName().startsWith("http://") || job.getName().startsWith("https://")) {
            sb.append(String.format("<h3 class=\"job-url\"><a href=\"%s\">%s</a></h3>", job.getName(), job.getName()));
        } else {
            sb.append(String.format("<h3 class=\"job-url\">%s</h3>", job.getName()));
        }
        sb.append(String.format("<p class=\"job-details\">(for %s - job %s)</p>", job.getUserName(), job.getId()));
        sb.append(String.format("<p class=\"job-logs\" id=\"%s\">", job.getId()));
        sb.append(job.getExplanation()).append("\n");
        sb.append("Logs will appear below as they are generated:" + "\n");
        sb.append("---- Web: Logs start ----" + "\n");
        sb.append("</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
