\name{labbcat.getGraphIdsInCorpus}
\alias{labbcat.getGraphIdsInCorpus}
\title{Gets a list of graph IDs in the given corpus}
\description{
  Gets a list of graph IDs (i.e. transcript names) in the given corpus.
}
\usage{
labbcat.getGraphIdsInCorpus(labbcat, id)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{A corpus ID}
}
\value{A list of graph IDs}
\seealso{labbcat.getCorpusIds}
\examples{
## List transcripts in QB corpus
qb.transcripts <- labbcat.getGraphIdsInCorpus(labbcat, "QB")
}
\keyword{graph}
\keyword{transcript}
