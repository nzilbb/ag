\name{labbcat.getGraphIds}
\alias{labbcat.getGraphIds}
\title{Gets a list of graph IDs}
\description{
  Gets a list of graph IDs (i.e. transcript names).
}
\usage{
labbcat.getGraphIds(labbcat)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance}
}
\value{A list of graph IDs}
\examples{
## List all transcripts
transcripts <- labbcat.getGraphIds(labbcat)
}
\keyword{graph}
\keyword{transcript}
