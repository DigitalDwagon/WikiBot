# WikiBot
WikiBot is a Discord and IRC bot for easy archiving of MediaWiki and DokuWiki based wikis. It spawns powershell or bash processes in the background to run the dokuWikiDumper and dumpgenerator commands respectively.
Note: Mediawiki uploading is currently Bash-only. 

# Setup
Note: this bot is NOT currently set up to be installed by other people (hardcoded Discord channels, google cloud bucket names, etc), though it is on my todo list. Dragona be here (not just me)!
If you don't have it already, install JDK 17 (Link: https://adoptium.net/temurin/releases/) and Python

Install dokuWikiDumper for DokuWikis:

pip install dokuwikidumper

Install mediawiki-scraper (WikiTeam3) for MediaWikis:

git clone https://github.com/mediawiki-client-tools/mediawiki-scraper
cd mediawiki-scraper
poetry update && poetry install && poetry build
pip install --force-reinstall dist/*.whl

Install ia CLI for uploading:

pip install internetarchive

Run ia configure to log into your Internet Archive account. You'll also need to create ~/doku_uploader_ia_keys (no file extension) with your Internet Archive s3-like API keys. Find them here: https://archive.org/account/s3.php. Only the access key on the first line, only the secret key on the second, with no further text.

Verify installations:
dokuWikiDumper --help
dumpgenerator --help
ia --help

You'll also need to install the Google Cloud CLI and configure application default credentials.

You can now run the bot!

# Configuration
In the same folder as the jar file, create a .env file with the following:
token=totallyrealdiscordtoken # The token for your discord bot user
ircpass=password123! # irc password for the bot to use
ircchannel=#example # IRC channel for the bot to use
ircnick=WikiBot # The irc nick for the bot to use
irclogin=true # use irc with authentication?
disable_doku_archive=false # disable dokuwiki archiving commands?
disable_mediawiki_archive=false # disable mediawiki archiving commands?
is_test=false # whether the bot is running in development mode
