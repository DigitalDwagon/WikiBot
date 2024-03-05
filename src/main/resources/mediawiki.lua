--[[local urlparse = require("socket.url")
local http = require("socket.http")
local https = require("ssl.https")
local cjson = require("cjson")
local utf8 = require("utf8")]]
local downloaded = {}
local addedtolist = {}
local seturls = {}

processed = function(url)
    if downloaded[url] or addedtolist[url] --[[or seturls[url]] then
        return true
    end
    return false
end


wget.callbacks.download_child_p = function(urlpos, parent, depth, start_url_parsed, iri, verdict, reason)
    --print("dcp " .. urlpos["url"]["url"])
    local url = urlpos["url"]["url"]
    if processed(url) then
        return false
    end
    if verdict == true then
        addedtolist[url] = true
    end
    return verdict
end

--[[wget.callbacks.get_urls = function(file, url, is_css, iri)
    print("gu " .. url)
    downloaded[url] = true
    local urls = {}

    if string.match(url, "https://archiveteam%.invalid/wikibot%-input%-file/") then
        -- content after https://archiveteam.invalid/wikibot-input-file/ is the filename with the list of URLs
        local filename = string.match(url, "https://archiveteam%.invalid/wikibot%-input%-file/(.*)")

        local file = io.open(filename, "r")
        if file then
            print("Reading URLs from " .. filename)
            for line in file:lines() do
                table.insert(urls, { url=line })
            end
            file:close()
        end
        print("Returning URLs from " .. url)
        return urls
    end

    --try to extract images, css, and javascript from the page
    for url in file:gmatch('img.-src="([^"]+)"') do
        if not processed(url) then
            table.insert(urls, {url=url})
            addedtolist[url] = true
        end
    end

    for url in file:gmatch('link.-href="([^"]+)"') do
        if not processed(url) then
            table.insert(urls, {url=url})
            addedtolist[url] = true
        end
    end

    for url in file:gmatch('script.-src="([^"]+)"') do
        if not processed(url) then
            table.insert(urls, {url=url})
            addedtolist[url] = true
        end
    end

    return urls
end--]]

local url_count = 0

--wget.callbacks.write_to_warc = function(url, http_stat)
--    status_code = http_stat["statcode"]
--    print(url_count .. "=" .. status_code .. " " .. url["url"])
--    url_count = url_count + 1
--    downloaded[url["url"]] = true
--    return true
--end

wget.callbacks.httploop_result = function(url, err, http_stat)
    --[[if string.match(url["url"], "https://archiveteam%.invalid/wikibot%-input%-file/") then
        -- content after https://archiveteam.invalid/wikibot-input-file/ is the filename with the list of URLs
        local filename = string.match(url["url"], "https://archiveteam%.invalid/wikibot%-input%-file/(.*)")

        local file = io.open(filename, "r")
        if file then
            print("Reading URLs from " .. filename)
            for line in file:lines() do
                seturls[line] = true
            end
            file:close()
        end
        print("Returning URLs from " .. url["url"])
    end]]



    --print("hr " .. url["url"])
    status_code = http_stat["statcode"]
    print(url_count .. "=" .. status_code .. " " .. url["url"])
    downloaded[url["url"]] = true
    url_count = url_count + 1
end
