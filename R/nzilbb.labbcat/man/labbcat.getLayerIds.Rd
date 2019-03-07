\name{labbcat.getLayerIds}
\alias{labbcat.getLayerIds}
\title{Gets a list of layer IDs}
\description{
  Gets a list of layer IDs (annotation 'types').
}
\usage{
labbcat.getLayerIds(labbcat)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance}
}
\value{A list of layer IDs}
\examples{
## Get names of all layers
layer.ids <- labbcat.getLayerIds(labbcat)
}
\keyword{layer}
