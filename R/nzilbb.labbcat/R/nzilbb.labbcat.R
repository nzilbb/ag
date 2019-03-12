### Internal variables:

## minimum version of LaBB-CAT required:
.min.labbcat.version <- "20190312.1838"

## encode a parameter value for inclusion in the URL
enc <- function(value) {
    return(stringr::str_replace_all(URLencode(value),"\\+","%2B"))
}

## build a store call URL 
buildUrl <- function(labbcat, call, parameters = NULL) {
    url <- paste(labbcat$storeUrl, call, sep="")
    if (!is.null(parameters)) {
        for (name in names(parameters)) {
            url <- paste(url, "&", name, "=", parameters[name], sep="")
        } # next parameter
    } # there are parameters
    url <- enc(url)
    return(url)
}

## Export functions:

labbcat.instance <- function(url, username = NULL, password = NULL) {
    baseUrl <- url
    ## ensure baseUrl has a trailing slash
    if (!grepl("/$", url)) baseUrl <- paste(url, "/", sep="")
    
    storeUrl <- paste(baseUrl, "store?call=", sep="")
    
    if (is.null(username)) {
        authorization <- NULL
        resp <- httr::GET(storeUrl)
    } else {  
        authorization <- httr::authenticate(username, password)
        resp <- httr::GET(storeUrl, authorization)
    }
    if (httr::status_code(resp) != 200) { # 200 = OK
        if (httr::status_code(resp) == 401 && is.null(username) && is.null(password)) {
            ## it's password-protected, but they haven't provided credentials
            ## so ask them for the username and password
            return(labbcat.instance(
                url, readline("LaBB-CAT Username: "), readline("LaBB-CAT Password: ")))
        } else {
            print(paste("ERROR: ", httr::http_status(resp)$message))
            return(NULL)
        }
    } else { ## respons was OK
        ## check the LaBB-CAT version
        resp.content <- httr::content(resp, as="text", encoding="UTF-8")
        resp.json <- jsonlite::fromJSON(resp.content)
        version <- resp.json$model$version
        if (is.null(version) || version < .min.labbcat.version) {
            print(paste("ERROR:", baseUrl, "is version", version, "but the minimum version is", .min.labbcat.version))
            return(NULL)
        } else { ## everything OK
            return (list(
                baseUrl = baseUrl,
                version = version,
                storeUrl = storeUrl,
                authorization = authorization
            ))
        }
    }
}

labbcat.getId <- function(labbcat) {
    url <- buildUrl(labbcat, "getId")
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getLayerIds <- function(labbcat) {
    url <- buildUrl(labbcat, "getLayerIds")
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getLayer <- function(labbcat, id) {
    url <- buildUrl(labbcat, "getLayer", list(id=id))
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getCorpusIds <- function(labbcat) {
    url <- buildUrl(labbcat, "getCorpusIds")
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getParticipantIds <- function(labbcat) {
    url <- buildUrl(labbcat, "getParticipantIds")
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getGraphIds <- function(labbcat) {
    url <- buildUrl(labbcat, "getGraphIds")
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getGraphIdsInCorpus <- function(labbcat, id) {
    url <- buildUrl(labbcat, "getGraphIdsInCorpus", list(id=id))
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getGraphIdsWithParticipant <- function(labbcat, id) {
    url <- buildUrl(labbcat, "getGraphIdsWithParticipant", list(id=id))
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getMatchingGraphIdsPage <- function(labbcat, expression, pageLength = NULL, pageNumber = NULL, order = NULL) {
    parameters <- list(expression=expression)
    if (!is.null(pageLength)) parameters <- append(parameters, list(pageLength=pageLength))
    if (!is.null(pageNumber)) parameters <- append(parameters, list(pageNumber=pageNumber))
    if (!is.null(order)) parameters <- append(parameters, list(order=order))
    url <- buildUrl(labbcat, "getMatchingGraphIdsPage", parameters)
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.countAnnotations <- function(labbcat, id, layerId) {
    parameters <- list(id=id, layerId=layerId)
    url <- buildUrl(labbcat, "countAnnotations", parameters)
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getAnnotations <- function(labbcat, id, layerId, pageLength = NULL, pageNumber = NULL) {
    parameters <- list(id=id, layerId=layerId)
    if (!is.null(pageLength)) parameters <- append(parameters, list(pageLength=pageLength))
    if (!is.null(pageNumber)) parameters <- append(parameters, list(pageNumber=pageNumber))
    url <- buildUrl(labbcat, "getAnnotations", parameters)
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getAnchors <- function(labbcat, id, anchorId) {
    parameters <- list(id=id)
    for (id in anchorId) parameters <- append(parameters, list(anchorId=id))
    url <- buildUrl(labbcat, "getAnchors", parameters)
    resp <- httr::GET(url, labbcat$authorization)
    resp.content <- httr::content(resp, as="text", encoding="UTF-8")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
    }
    resp.json <- jsonlite::fromJSON(resp.content)
    return(resp.json$model$result)
}

labbcat.getSoundFragment <- function(labbcat, id, start, end, sampleRate = NULL) {
    url <- paste(labbcat$baseUrl, "soundfragment", sep="")
    parameters <- list(id=id, start=start, end=end)
    if (!is.null(sampleRate)) parameters <- list(id=id, start=start, end=end, sampleRate=sampleRate)
    filename <- paste(stringr::str_replace(id, "\\.[^.]+$",""), "__", start, "-", end, ".wav", sep="")
    resp <- httr::POST(url, labbcat$authorization, httr::write_disk(filename, overwrite=TRUE), body = parameters, encode = "form")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        if (httr::status_code(resp) != 404) { # 404 means the audio wasn't on the server
            ## some other error occurred so print what we got from the server
            print(readLines(filename))
        }
        file.remove(filename)
        return(NULL)
    }
    content.disposition <- as.character(httr::headers(resp)["content-disposition"])
    content.disposition.parts <- strsplit(content.disposition, "=")
    if (length(content.disposition.parts[[1]]) > 1
        && filename != content.disposition.parts[[1]][2]) {
        ## file name is specified, so use it
        file.rename(filename, content.disposition.parts[[1]][2])
        filename <- content.disposition.parts[[1]][2]
    }
    return(filename)
}
