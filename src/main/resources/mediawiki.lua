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
    local url = urlpos["url"]["url"]
    if processed(url) then
        return false
    end
    if verdict == true then
        addedtolist[url] = true
    end
    return verdict
end

local url_count = 0

wget.callbacks.httploop_result = function(url, err, http_stat)
    status_code = http_stat["statcode"]
    print(url_count .. "=" .. status_code .. " " .. url["url"])
    downloaded[url["url"]] = true
    url_count = url_count + 1
end
