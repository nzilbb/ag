
// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const mappingsDiv = document.getElementById("mappings");
function listMappings() {
    // show spinner
    startLoading();
    
    mappingsDiv.innerHTML = "";
    // list the existing mappings
    getJSON("listMappings", mappings => {
        try {
            if (mappings.length == 0) {
                const td = document.createElement("td");
                td.colSpan = 8;
                td.appendChild(document.createTextNode("There are no mappings yet"));
                const tr = document.createElement("tr");
                tr.appendChild(td);
                mappingsDiv.appendChild(tr);
            } else {
                for (let mapping of mappings) {
                    const a = document.createElement("a");
                    a.appendChild(document.createTextNode(mapping));
                    a.href = resourceForFunction("mappingToCsv", mapping);
                    a.download = mapping + ".csv";
                    a.type = "text/csv";
                    a.title = "Download raw edit path data as CSV";
                    const td = document.createElement("td");
                    td.appendChild(a);
                    const tr = document.createElement("tr");
                    tr.appendChild(td);
                    mappingsDiv.appendChild(tr);
                    // get summary info
                    const mappingId = mapping;
                    getJSON(resourceForFunction("summarizeMapping", mappingId), summary => {
                        addSummaryStatistic(
                            summary.utteranceCount, tr,
                            resourceForFunction("utteranceSummaryToCsv", mappingId),
                            "utterances-"+mappingId+".csv",
                            "Summary by utterance as CSV");
                        addSummaryStatistic(summary.stepCount, tr);
                        addSummaryStatistic(summary.sourceCount, tr);
                        addSummaryStatistic(summary.targetCount, tr);
                        addSummaryStatistic(summary.meanOverlapRate.toFixed(3), tr);
                        // add delete button
                        let td = document.createElement("td");
                        const button = document.createElement("button");
                        button.appendChild(document.createTextNode("❌"));
                        button.title = `Delete ${mappingId}`;
                        button.onclick = ()=>{deleteMapping(mappingId);};
                        td.appendChild(button);
                        tr.appendChild(td);
                    });
                } // next mapping
            } // there are mappings
        } finally {
            // hide spinner
            finishedLoading();
        }
    });
}

function addSummaryStatistic(statistic, tr, linkHref, linkName, linkTitle) {
    let td = document.createElement("td");
    if (!linkHref) {
        td.appendChild(document.createTextNode(statistic));
    } else {
        const a = document.createElement("a");
        a.appendChild(document.createTextNode(statistic));
        a.href = linkHref;
        a.download = linkName;
        a.type = "text/csv";
        a.title = linkTitle;
        td.appendChild(a);
    }
    tr.appendChild(td);
}

function deleteMapping(mappingId) {
    if (confirm(`Are you sure you want to delete the mapping ${mappingId}?`)) {
        get(resourceForFunction("deleteMapping", mappingId), () => {
            // refresh mappings list
            listMappings();
        }, "text/plain");
    }
}

listMappings();
