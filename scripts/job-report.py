#!/usr/bin/env python3

# Run this in the jobs/ folder of a wikibot instance.
# It will generate a human-readable report of all the jobs
# on disk, including size and status. Note that the status
# field is not perfect. "Failed" jobs may actually be running
# and some jobs marked as pending upload may not be finished
# dumping, among others.

import os.path
import sys
from os import listdir
from os import path

def get_size(filePath = '.'):
    total_size = 0
    if os.path.isfile(filePath):
        total_size += os.path.getsize(filePath)

    if os.path.isdir(filePath):
        for childPath in os.listdir(filePath):
            childPath = os.path.join(filePath, childPath)
            total_size += get_size(childPath)

    return total_size

def human_readable_size(num, suffix="B"):
    bytes = num
    for unit in ("", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "Zi"):
        if abs(num) < 1024.0:
            return f"{num:3.1f}{unit}{suffix} ({bytes} bytes)"
        num /= 1024.0
    return f"{num:.1f}Yi{suffix} ({bytes} bytes)"


allFiles = {}
jobGroup = {}
dumpDirectories = {}
wikiTypes = {}

for jobDir in listdir("."):
    if not os.path.isdir(jobDir):
        continue
    #if not os.path.basename(jobDir) == "f9b55498-aaf5-464a-a5f9-35ecb1c92517":
    #    continue
    jobId = os.path.basename(jobDir)
    jobFiles = {}
    status = "LOG_ONLY"
    wikiType = "LOG_ONLY"
    for jobFile in listdir(jobDir):
        jobFile = os.path.join(jobDir, jobFile)
        if os.path.isdir(jobFile):
            dumpDirectories[jobId] = jobFile
            status = "FAILED"

            if os.path.exists(os.path.join(jobFile, "images")):
                if os.path.exists(os.path.join(jobFile, "uploaded_to_IA.mark")):
                    status = "DONE"
                elif os.path.exists(os.path.join(jobFile, "offloaded.mark")):
                    status = "DONE"
                elif os.path.exists(os.path.join(jobFile, "all_dumped.mark")):
                    status = "WAITING_UPLOAD"

                wikiType = "MEDIAWIKI"
            elif os.path.exists(os.path.join(jobFile, "attic")):
                if os.path.exists(os.path.join(jobFile, "uploaded_to_IA.mark")):
                    status = "DONE"
                elif os.path.exists(os.path.join(jobFile, "content_dumped.mark")) and os.path.exists(os.path.join(jobFile, "html_dumped.mark")) and os.path.exists(os.path.join(jobFile, "media_dumped.mark")):
                    status = "WAITING_UPLOAD"

                wikiType = "DOKUWIKI"
            elif os.path.exists(os.path.join(jobFile, "wiki")):
                if os.path.exists(os.path.join(jobFile, "uploaded_to_IA.mark")):
                    status = "DONE"
                elif os.path.exists(os.path.join(jobFile, "content_dumped.mark")) and os.path.exists(os.path.join(jobFile, "attach_dumped.mark")):
                    status = "WAITING_UPLOAD"

                wikiType = "PUKIWIKI"
            else:
                wikiType = "UNKNOWN"

            for dumpFile in listdir(jobFile):
                dumpFile = os.path.join(jobFile, dumpFile)
                total_size = get_size(dumpFile)
                jobFiles[os.path.basename(dumpFile)] = human_readable_size(total_size)
            continue
        jobFiles[os.path.basename(jobFile)] = get_size(jobFile)

    if not status in jobGroup:
        jobGroup[status] = {}
    allFiles[jobId] = jobFiles
    jobGroup[status][jobId] = get_size(jobDir)
    wikiTypes[jobId] = wikiType


for group in ["WAITING_UPLOAD", "DONE", "FAILED", "LOG_ONLY"]:
    print("")
    print("")
    print("----------------------------------------------------------------------------------------")
    print("----------------------------------------------------------------------------------------")
    print(f"----------------------------------------- {group}")
    print("----------------------------------------------------------------------------------------")
    print("----------------------------------------------------------------------------------------")
    print("")
    print("")

    if not group in jobGroup:
        print("No jobs.")
        continue

    for job in dict(sorted(jobGroup[group].items(), key=lambda item: item[1], reverse=True)):
        print("-------------------------------------")
        print(job)
        print(f"Total size: {human_readable_size(get_size(job))}")
        if group == "WAITING_UPLOAD":
            print("!reupload " + job)
            if wikiTypes[job] == "MEDIAWIKI":
                print(f"wikiteam3uploader --zstd-level 22 --bin-zstd zstd-latest --collection wikiteam_inbox_1 --parallel {dumpDirectories[job]}")
            if wikiTypes[job] == "DOKUWIKI":
                print(f"dokuWikiUploader --collection wikiteam_inbox_1 {dumpDirectories[job]}")
            if wikiTypes[job] == "PUKIWIKI":
                print(f"dokuWikiUploader --collection wikiteam_inbox_1 {dumpDirectories[job]}")
        print("")
        for file in allFiles[job]:
            print("    " + file + " - " + str(allFiles[job][file]))
