### Internal functions:

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
        print(paste("ERROR: ", httr::http_status(resp)$message))
        labbcat <- NULL
    }
    list(
        baseUrl = baseUrl,
        storeUrl = storeUrl,
        authorization = authorization
    )
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

labbcat.getSoundFragment <- function(labbcat, id, start, end) {
    url <- paste(labbcat$baseUrl, "soundfragment", sep="")
    filename <- paste(stringr::str_replace(id, "\\.[^.]+$",""), "__", start, "-", end, ".wav", sep="")
    resp <- httr::POST(url, labbcat$authorization, httr::write_disk(filename, overwrite=TRUE), body = list(id=id, start=start, end=end), encode = "form")
    if (httr::status_code(resp) != 200) { # 200 = OK
        print(paste("ERROR: ", httr::http_status(resp)$message))
        print(resp.content)
        return()
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
