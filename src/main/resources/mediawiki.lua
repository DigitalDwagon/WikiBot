-- Digital's attempt to .warc mediawiki wikis
-- this file has lots of comments as I document for myself how wget-at works
local urlparse = require("socket.url")
local http = require("socket.http")
local https = require("ssl.https")
local cjson = require("cjson")
local utf8 = require("utf8")

dofile("wikibot/table_show.lua")
JSON = (loadfile "wikibot/JSON.lua")()

-- Constants and settings --
local print_debug_lines = false -- Very spammy - shows all of the information wget passes to this script, plus details about internal function logic
local delay = 0.5 -- Static delay between requests
local retry_delay = 2 -- will wait this number of seconds to the power of the number of retries on a
local max_retries = 5 -- most number of times the bot will retry a url before skipping it or failing the grab, depending on what it is trying to retry
-- ^^ ALSO SET --tries ON THE COMMAND LINE / IN WIKIBOT TO A HIGHER NUMBER OR ELSE WGET-AT WILL CUT OFF THE RETRYING EARLY

-- Variable initializations --
local url_count = 0 -- The total number of URLs that have been grabbed
local downloaded = {} -- URLs that have already been downloaded
local addedtolist = {} -- URLs that have been added to the queue
local url_type = {} -- What kind of URL (page) is being processed (page, revision history, api, etc)
local url_type_process = {} -- processing functions for url types

local urls_to_queue = {} -- URL processing queue, kept in Lua to make it easier to manage retries/etc
local retry_url = nil -- set to a URL and it will be retried as the next request with an extra delay
local retries = 0 -- number of times the active url has been retried

local api_url = nil -- api.php url for the wiki
local index_url = nil -- index.php url for the wiki
local page_queue_start = false -- Set to true when the "all page queuing" is beginning (e.g. we have requested the namespaces from the API, which will then start listing the pages in those namespaces)
local wiki_meta = {}

local abort_grab = false -- Set to true to abort with failure
local exit = false -- Set to exit (cleanly)

local discovered_outlinks = {} -- TODO: outlinks discovered on pages (to be queued to #// or otherwise)

--
-- --- Resume ---
--

local resume_file = io.open("wikibot/meta.json", "r")
if resume_file ~= nil then
	print("This grab will be resumed from file...")
	local resume_content = resume_file:read("*all")
	resume_file:close()
	local resume_data = JSON:decode(resume_content)
	url_count = resume_data["url_count"]
	api_url = resume_data["api_url"]
	index_url = resume_data["index_url"]
	page_queue_start = resume_data["page_queue_start"]
	urls_to_queue = resume_data["urls_to_queue"]
	addedtolist = resume_data["addedtolist"]
	downloaded = resume_data["downloaded"]
	url_type = resume_data["retry_url"]
	print("Loaded meta.json")
end

--
-- --- Helper functions ---
--

-- Whether or not a URL has been queued
processed = function(url)
	if downloaded[url] or addedtolist[url] then
		return true
	end
	return false
end

get_headers = function(type, referer)
	local headers = {}
	if type == "IMAGE" then
		headers["Accept"] = "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5" --
	end
	headers["Referer"] = referer
	if referer == nil then
		headers["Referer"] = ""
	end
end

-- Queues a url
queue_url = function(url, type, referer)
	local headers = get_headers(type, referer)
	url_type[url] = type
	table.insert(urls_to_queue, {url=url, headers=headers})
	addedtolist[url] = true
	print("	-> " .. url)
end

-- Queues a url if it has not already been queued
safe_queue_url = function (url, type, referer)
	if not processed(url) then
		queue_url(url, type, referer)
	end
end

-- safe_queue_url but sets expect_html
safe_queue_html = function(url, type, referer)
	if not processed(url) then
		url_type[url] = type
		local headers = get_headers(url, referer)
		table.insert(urls_to_queue, {url=url, link_expect_html=1, headers=headers})
		addedtolist[url] = true
		print("	-> " .. url)
	end
end

-- safe_queue_url but sets expect_css
safe_queue_css = function(url, type, referer)
	if not processed(url) then
		url_type[url] = type
		local headers = get_headers(url, referer)
		table.insert(urls_to_queue, {url=url, link_expect_css=1, headers=headers})
		addedtolist[url] = true
		print("	-> " .. url)
	end
end

-- print() but for debug information so you can turn it off
debug_print = function(string)
	if print_debug_lines then
		print(string)
	end
end

-- Stop the grab with failure
abort = function()
	abort_grab = true
	exit = true
	print("Aborting...")
end

-- Write content to a file
write_to_file = function(filename, content)
	local file = io.open(filename, "w")
	file:write(content)
	file:close()
end

-- Checks if a string starts with a given prefix
function starts_with(str, prefix)
	return string.sub(str, 1, string.len(prefix)) == prefix
end

-- Decode html escapes
local function html_decode(str)
	local entities = {
		["&amp;"] = "&",
		["&lt;"] = "<",
		["&gt;"] = ">",
		["&quot;"] = '"',
		["&apos;"] = "'",
		["&#(%d+);"] = function(n) return string.char(tonumber(n)) end,
		["&#x(%x+);"] = function(n) return string.char(tonumber(n, 16)) end
	}

	return (str:gsub("(&.-;)", entities))
end

--
-- --- Wget hooks ---
--

-- For API navigation/queuing
-- also contains the sleep between urls
wget.callbacks.httploop_result = function(url, err, http_stat)
	local fullurl = url["url"]
	if fullurl ~= retry_url then
		retry_url = null
		retries = 0
	end

	debug_print(table.show({url=url, err=err, http_stat=http_stat }, "httploop_result"))

	if url_count == 1 then
		--todo the url counter is incremented before this method is called
		url_type[fullurl] = "PAGE"
	end

	local statcode = http_stat["statcode"]
	if statcode == 301
			or statcode == 302
			or statcode == 303
			or statcode == 307
			or statcode == 308
	then
		url_type[http_stat["newloc"]] = url_type[fullurl]
	end

	assert(url_type[fullurl] ~= nil, "URL type for URL is unknown... \nBad URL --> " .. fullurl)

	local file = io.open(http_stat["local_file"], "r")
	local content = file:read("*all")
	file:close()

	if url_type[fullurl] ~= nil then
		debug_print(url_type[fullurl] .. " " .. fullurl)
		url_type_process[url_type[fullurl]](fullurl, err, http_stat, content)
	end

	if api_url ~= nil and not page_queue_start then
		-- This kicks off the process of paginating through the allpages API if it hasn't started and we know the API url
		--table.insert(urls_to_queue, { url = api_url .. "?action=query&meta=siteinfo&siprop=namespaces&format=json"})
		--todo a few extra props missing below
		local siprop = "general|namespaces|namespacealiases|specialpagealiases|magicwords|interwikimap|statistics|usergroups|autocreatetempuser|clientlibraries|libraries|extensions|fileextensions|rightsinfo|restrictions|languages|languagevariants|skins|extensiontags|functionhooks|showhooks|variables|protocols|defaultoptions"
		safe_queue_url(api_url .. "?action=query&meta=siteinfo&siprop=" .. urlparse.escape(siprop) .. "&sinumberingroup=true&format=json", "API", nil)
		-- make sure the URL is escaped or else the escaped version returned by wget-at won't match
		-- may show some categories missing on older wikis but should still return all the other content
		page_queue_start = true
	end


	for src in content:gmatch('<img[^>]+src="([^"]+)"') do
		safe_queue_url(urlparse.absolute(url["url"], html_decode(src)), "IMAGE", fullurl)
	end

	for src in content:gmatch('<script[^>]+src="([^"]+)"') do
		safe_queue_url(urlparse.absolute(url["url"], html_decode(src)), "JS", fullurl)
	end

	for srcset in content:gmatch('<img[^>]+srcset="([^"]+)"') do
		for srcurl in srcset:gmatch("([^ ]+) [^,]+,") do
			-- TODO - is it worth getting all images in an srcset?
			-- replayweb.page seems to require it for functional playback
			-- (firefox gives up on the first 404 in an srcset, even when the src is present, but it seems wasteful)
			safe_queue_url(urlparse.absolute(url["url"], html_decode(srcurl)), "IMAGE", fullurl)
		end
	end

	for href in content:gmatch('<link[^>]+rel="stylesheet"[^>]+href="([^"]+)"') do
		debug_print("	-> " .. urlparse.absolute(url["url"], html_decode(href)))
		safe_queue_url(urlparse.absolute(url["url"], html_decode(href)), "CSS", fullurl)
	end

	local sleep_time = delay

	local action = wget.actions.NOTHING
	--print(table.show({nothing=wget.actions.NOTHING, continue=wget.actions.CONTINUE, abort=wget.actions.ABORT, exit=wget.actions.EXIT}, "exit_statuses"))
	if retry_url ~= nil then
		retries = retries + 1
		if retries >= max_retries then
			print("I give up")
			abort()
		else
			action = wget.actions.CONTINUE
			sleep_time = retry_delay ^ retries
			print("Sleeping " .. sleep_time .. " seconds...")
			io.flush()
		end
	end

	os.execute("sleep " .. sleep_time)
	io.flush()
	return action
end

-- queues urls found in css from load.php files
wget.callbacks.download_child_p = function(urlpos, parent, depth, start_url_parsed, iri, verdict, reason)
	debug_print(table.show({urlpos=urlpos, parent=parent, depth=depth, start_url_parsed=start_url_parsed, iri=iri, verdict=verdict, reason=reason}, "download_child"))

	if parent["file"] == "load.php" then
		debug_print("load.php dependency " .. urlpos["url"]["url"])
		safe_queue_url(urlpos["url"]["url"], "FILE", parent["url"])
	end

	return false
end

-- Chooses the next URL to queue
wget.callbacks.get_urls = function(file, url, is_css, iri)
	debug_print(table.show({file=file, url=url, is_css=is_css, iri=iri}, "get_urls"))
	if exit then
		return {}
	end

	local stop_file = io.open("wikibot/STOP", "r")
	if stop_file ~= nil then
		stop_file:close()
		exit = true
		return {}
	end

	if retry_url ~= nil then
		return nil
	end
	-- return first item from urls_to_queue and remove it
	if #urls_to_queue > 0 then
		local urls = {urls_to_queue[1]}
		table.remove(urls_to_queue, 1)
		return urls
	end

	return {}
end

--note: (wget-at bug?) returning false still causes a request record to be written to the warc
wget.callbacks.write_to_warc = function(url, http_stat)
	io.flush()
	local fullurl = url["url"]
	local status = http_stat["statcode"]
	debug_print(table.show({url, http_stat}, "write_to_warc"))
	print(url_count .. " = " .. status .. " " .. fullurl)
	url_count = url_count + 1
	addedtolist[url["url"]] = nil
	downloaded[url["url"]] = true

	--debug_print(table.show({addedtolist=addedtolist, downloaded=downloaded}, "queue_status"))
	return true
end

wget.callbacks.before_exit = function(exit_status, exit_status_string)
	-- writes important data to files so that the grab can be resumed later
	-- (ex. wikibot will stop wget-at to split the warcs if they get too big)
	meta = {}
	meta["url_count"] = url_count
	meta["api_url"] = api_url
	meta["index_url"] = index_url
	meta["page_queue_start"] = page_queue_start
	meta["urls_to_queue"] = urls_to_queue
	meta["addedtolist"] = addedtolist
	meta["downloaded"] = downloaded
	meta["url_type"] = url_type
	write_to_file("wikibot/meta.json", JSON:encode_pretty(meta))

	if abort_grab then
		print("wget failed, exiting...")
		return 1
	end
	return 0 -- makes my life in wikibot easier
end



url_type_process["PAGE"] = function(url, err, http_stat, content)
	if api_url == nil then
		api_url = content:match('<link[^>]*rel="EditURI"[^>]*href="([^"]+)?[^"]+"')
		debug_print(api_url)
	end

	if index_url == nil and api_url ~= nil then
		index_url = api_url:gsub("api%.php", "index.php")
		debug_print(index_url)
	elseif index_url == nil then
		print("hmm, can't detect the index url")
	end
end

url_type_process["REVISION_LIST"] = function(url, err, http_stat, content)
	for href in content:gmatch('<a[^>]+href="([^"]+)"[^>]+class="[^"]*mw%-changeslist%-date[^"]*"[^>]+') do
		safe_queue_html(urlparse.absolute(url, html_decode(href)), "PAGE", fullurl)
	end

	for href in content:gmatch('<a[^>]+href="([^"]+)"[^>]+class="[^"]*mw%-nextlink[^"]*"[^>]*') do
		safe_queue_html(urlparse.absolute(url, html_decode(href)), "REVISION_LIST", fullurl)
	end
end

url_type_process["API"] = function(url, err, http_stat, content)
	--try to parse json
	debug_print("trying to parse API json for url " .. url)
	io.flush()
	debug_print(content)
	io.flush()
	local json = JSON:decode(content)
	debug_print(table.show({json=json}, "api_response"))
	io.flush()
	local query = json["query"]
	local batchcomplete = json["batchcomplete"]
	if query == nil and batchcomplete == nil then
		print("Bad API response")
		io.flush()
		retry_url = url
		return nil
	end

	if query == nil then
		return nil
	end

	local namespaces = query["namespaces"]
	if namespaces ~= nil then
		for key, namespace in pairs(namespaces) do
			if namespace["id"] < 0 then
				-- special namespaces for system use that don't actually exist in the DB (i.e. can't be paginated)
				goto continue
			end
			safe_queue_url(api_url .. "?action=query&generator=allpages&format=json&gaplimit=50&prop=info&inprop=url&gapnamespace=" .. namespace["id"], "API", nil)
			::continue::
		end
	end

	local general = query["general"]
	if general ~= nil then
		--assert(wiki_meta == {}, "Wiki meta info is present, but has already been discovered in another request")
		wiki_meta = general
	end

	local specialpagealiases = query["specialpagealiases"]
	if specialpagealiases ~= nil then
		--print(table.show(specialpagealiases, "specialpagealiases"))
		assert(wiki_meta ~= nil, "Tried to process special page list, but the wiki meta was null!")
		for key, page in pairs(specialpagealiases) do
			local aliases = page["aliases"]
			-- Inverse sort the table so that any aliases are queued first, to try to reduce duplication when aliases redirect to the main special page and its get grabbed twice
			table.sort(aliases, function(a,b) return a > b  end)
			for akey, alias in pairs(aliases) do
				safe_queue_html(wiki_meta["server"] .. wiki_meta["articlepath"]:gsub("$1", "Special:" .. alias), "PAGE", nil)
			end
			--queue real name after for the same reason as inverse sorting the aliases
			safe_queue_html(wiki_meta["server"] .. wiki_meta["articlepath"]:gsub("$1", "Special:" .. page["realname"]), "PAGE", nil)
		end
	end
	local pages = query["pages"]
	if pages ~= nil then
		for key, page in pairs(pages) do
			safe_queue_html(page["fullurl"], "PAGE", nil)
			safe_queue_html(page["editurl"]:gsub("action=edit", "action=history"), "REVISION_LIST", page["fullurl"])
			--safe_queue_html(page["editurl"]:gsub("action=edit", "action=info"))
			safe_queue_url(page["editurl"]:gsub("action=edit", "action=raw"), "WIKITEXT", page["fullurl"])
		end
	end

	local continue = json["continue"]
	if continue == nil then
		return
	end
	-- TODO this should support continue of all types, e.g. Fandom will add a continue with "siprop" key for the mass meta request we make
	if continue["gapcontinue"] ~= nil then
		local cleaned_url = url:gsub("&gapcontinue=[^&]*", "")
		safe_queue_url(cleaned_url .. "&gapcontinue=" .. continue["gapcontinue"], "API", nil)
	end
end

url_type_process["IMAGE"] = function(url, err, http_stat, content)
	print("File")
end

url_type_process["FILE"] = function(url, err, http_stat, content)
	print("File")
end

url_type_process["CSS"] = function(url, err, http_stat, content)
	print("File")
end

url_type_process["JS"] = function(url, err, http_stat, content)
	print("File")
end
url_type_process["WIKITEXT"] = function(url, err, http_stat, content)
	print("Wikitext")
end

-- Wget callbacks down here are only used for printing debug information --


wget.callbacks.init = function()
	debug_print(table.show({}, "init"))
end

wget.callbacks.lookup_host = function(host)
	debug_print("Host lookup:" .. host)
	return nil
end

wget.callbacks.finish = function(start_time, end_time, wall_time, numurls, total_downloaded_bytes, total_download_time)
	debug_print(table.show({start_time=start_time, end_time=end_time, wall_time=wall_time, numurls=numurls, total_downloaded_bytes=total_downloaded_bytes, total_download_time=total_download_time}, "finish"))
	print("Done! Downloaded " .. numurls .. " URLs in " .. total_download_time .. " seconds.")
	print("Total download size: " .. total_downloaded_bytes .. " bytes")
end


