\name{labbcat.getCorpusIds}
\alias{labbcat.getCorpusIds}
\title{Gets a list of corpus IDs}
\description{
  Returns a list of corpora in the given LaBB-CAT instance.
}
\usage{
labbcat.getCorpusIds(labbcat)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance}
}
\value{A list of corpus IDs}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## List corpora
corpora <- labbcat.getCorpusIds(labbcat)
}
\keyword{corpora}
