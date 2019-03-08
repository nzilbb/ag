\name{labbcat.getId}
\alias{labbcat.getId}
\title{Gets the store's ID}
\description{
  Gets the store's ID - i.e. the ID of the LaBB-CAT instance.
}
\usage{
labbcat.getId(labbcat)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance}
}
\value{The annotation store's ID}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Get ID of LaBB-CAT instance
instance.id <- labbcat.getId(labbcat)
}
\keyword{corpora}
