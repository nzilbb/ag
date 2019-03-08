\name{labbcat.getLayer}
\alias{labbcat.getLayer}
\title{Gets a layer definition}
\description{
  Gets a layer definition.
}
\usage{
labbcat.getLayer(labbcat, id)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{ID of the layer to get the definition for}
}
\value{The definition of the given layer}
\seealso{\link{labbcat.getLayerIds}}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Get the configuration of the orthography layer
orthography.layer <- labbcat.getLayer(labbcat, "orthography")
}
\keyword{layer}
