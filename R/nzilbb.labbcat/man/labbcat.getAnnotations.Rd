\name{labbcat.getAnnotations}
\alias{labbcat.getAnnotations}
\title{Gets the annotations on the given layer of the given graph}
\description{
  Gets the annotations on the given layer of the given graph (transcript).
}
\usage{
labbcat.getAnnotations(labbcat, id, layerId, pageLength = NULL, pageNumber = NULL)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{A graph ID (i.e. transcript name)}
  \item{layerId}{A layer name}
  \item{pageLength}{The maximum number of annotations to return, or null to return all},
  \item{pageNumber}{The zero-based page number to return, or null to return the first page},
}
\value{
  A named list of annotations, with members
  \item{id}{The annotation's unique ID},
  \item{layerId}{The name of the layer it comes from}
  \item{label}{The value of the annotation}
  \item{startId}{The ID of the start anchor},
  \item{endId}{The ID of the end anchor},
  \item{parentId}{The ID of the parent annotation},
  \item{ordinal}{The ordinal of the annotation among its peers},
  \item{confidence}{A rating from 0-100 of the confidence of the label,
    e.g. 10: default value, 50: automatically generated, 100: manually annotated}
}
\seealso{
  \code{\link{labbcat.getGraphIds}}
  \code{\link{labbcat.getGraphIdsInCorpus}}
  \code{\link{labbcat.getGraphIdsWithParticipant}}
  \code{\link{labbcat.countAnnotations}}
}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Get all the orthography tokens in UC427_ViktoriaPapp_A_ENG.eaf
orthography <- labbcat.getAnnotations(labbcat, "UC427_ViktoriaPapp_A_ENG.eaf", "orthography")

## Get the first 20 orthography tokens in UC427_ViktoriaPapp_A_ENG.eaf
orthography <- labbcat.getAnnotations(labbcat, "UC427_ViktoriaPapp_A_ENG.eaf", "orthography", 20, 0)
}
\keyword{graph}
\keyword{transcript}
