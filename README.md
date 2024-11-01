# WikiBot
WikiBot is a bot for easy archiving of MediaWiki, DokuWiki, and PukiWiki based wikis. You can learn more about using it on [the wiki page](https://wiki.archiveteam.org/index.php/Wikibot).

# Setup
Note: this bot is NOT currently set up to be installed by other people (hardcoded Discord channels, google cloud bucket names, etc), though it is on my todo list. However, it should run "well enough" for development purposes.

Install [JDK 21 or later](https://adoptium.net/temurin/releases/) and Python 3.9+. For development, install Gradle (any version should do, as this project includes the gradle wrapper)

Install dokuWikiDumper/pukiWikiDumper/WikiTeam3:

```
pip install dokuwikidumper pukiwikidumper wikiteam3
```

You'll also need to create `~/doku_uploader_ia_keys`, `~/puki_uploader_ia_keys`, and `~/wikiteam3_ia_keys.txt` with your Internet Archive s3-like API keys. Find them here: <https://archive.org/account/s3.php>. Only the access key on the first line, only the secret key on the second, with no further text. It's recommended that you create only one and symlink the rest to make it easier to update the keys.

Install 7z (`p7zip-full`) and [zstd](https://github.com/facebook/zstd/) 1.5.5+, and add them to PATH.

Verify installations:

```
dokuWikiDumper --help
pukiWikiDumper --help
wikiteam3dumpgenerator --help
7z
zstd -V
```

You'll also need to install the Google Cloud CLI and configure application default credentials.

You can now run the bot!

# Configuration
In the same folder as the jar file, create config.json using the example json in the src/.../resources directory and fill in the values.
