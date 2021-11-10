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
                addSummaryStatistic(summary.utteranceCount, tr);
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

function addSummaryStatistic(statistic, tr) {
    let td = document.createElement("td");
    td.appendChild(document.createTextNode(statistic));
    tr.appendChild(td);
}
