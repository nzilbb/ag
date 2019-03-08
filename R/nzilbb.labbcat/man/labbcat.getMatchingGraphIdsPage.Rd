\name{labbcat.getMatchingGraphIdsPage}
\alias{labbcat.getMatchingGraphIdsPage}
\title{Gets a list of IDs of graphs that match a particular pattern}
\description{
  Gets a list of IDs of graphs (i.e. transcript names) that match a particular pattern.

  The results can be exhaustive, by omitting pageLength and pageNumber, or they
  can be a subset (a 'page') of results, by given pageLength and pageNumber values.

  The order of the list can be specified.  If ommitted, the graphs are listed in ID order.

  The expression language is currently not well defined, but expressions such as those in the examples can be used.
}
\usage{
labbcat.getMatchingGraphIdsPage(
        labbcat, expression, pageLength = NULL, pageNumber = NULL, order = NULL)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{expression}{An expression that determines which graphs match},
  \item{pageLength}{The maximum number of IDs to return, or null to return all},
  \item{pageNumber}{The zero-based page number to return, or null to return the first page},
  \item{order}{An expression that determines the order the graphs are
    listed in}
}
\value{A list of graph IDs (i.e. transcript names)}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Get all transcripts whose names start with "BR"
transcripts <- labbcat.getMatchingGraphIdsPage(labbcat, "id MATCHES 'BR.+'")

## Get the first twenty transcripts in the "QB" corpus
transcripts <- labbcat.getMatchingGraphIdsPage(
        labbcat, "my('corpus').label = 'QB'", 20, 0)

## Get the second transcript that has "QB247_Jacqui" as a speaker
transcripts <- labbcat.getMatchingGraphIdsPage(
        labbcat, "'QB247_Jacqui' IN labels('who')", 1, 1)

## Get all transcripts whose names start with "BR" and have "QB247_Jacqui" as a speaker,
## in word-count order 
transcripts <- labbcat.getMatchingGraphIdsPage(
        labbcat, "my('corpus').label = 'QB' AND 'QB247_Jacqui' IN labels('who')", 1, 1,
        "my('transcript_word_count').label")

}
\keyword{graph}
\keyword{transcript}
\keyword{expression}
