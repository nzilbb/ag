\name{labbcat.countAnnotations}
\alias{labbcat.countAnnotations}
\title{Gets the number of annotations on the given layer of the given graph}
\description{
  Gets the number of annotations on the given layer of the given graph (transcript).
}
\usage{
labbcat.countAnnotations(labbcat, id, layerId)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{A graph ID (i.e. transcript name)}
  \item{layerId}{A layer name}
}
\value{The number of annotations on that layer}
\seealso{
  \code{\link{labbcat.getGraphIds}}
  \code{\link{labbcat.getGraphIdsInCorpus}}
  \code{\link{labbcat.getGraphIdsWithParticipant}}
}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Count the number of words in UC427_ViktoriaPapp_A_ENG.eaf
token.count <- labbcat.countAnnotations(labbcat, "UC427_ViktoriaPapp_A_ENG.eaf", "orthography")
}
\keyword{graph}
\keyword{transcript}
