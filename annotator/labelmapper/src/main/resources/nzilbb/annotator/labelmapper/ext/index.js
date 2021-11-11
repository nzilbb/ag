// show spinner
startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// list the existing mappings
getJSON("listMappings", mappings => {
    try {
        const mappingsDiv = document.getElementById("mappings");
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
            getJSON(resourceForFunction("summarizeMapping", mapping), summary => {
                addSummaryStatistic(summary.utteranceCount, tr,
                                    resourceForFunction("utteranceSummaryToCsv", mapping),
                                    "utterances-"+mapping+".csv",
                                    "Summary by utterance as CSV");
                addSummaryStatistic(summary.stepCount, tr);
                addSummaryStatistic(summary.sourceCount, tr);
                addSummaryStatistic(summary.targetCount, tr);
                addSummaryStatistic(summary.meanOverlapRate.toFixed(3), tr);
            });
        } // next mapping
    } finally {
        // hide spinner
        finishedLoading();
    }
});

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
