\name{labbcat.getAnchors}
\alias{labbcat.getAnchors}
\title{Gets the given anchors in the given graph}
\description{
  Gets the given anchors in the given graph (transcript).
}
\usage{
labbcat.getAnchors(labbcat, id, anchorId)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{A graph ID (i.e. transcript name)}
  \item{anchorId}{A vector of anchor IDs (or a string representing one anchor ID)}
}
\value{
  A named list of anchors, with members
  \item{id}{The annotation's unique ID},
  \item{offset}{The offset from the beginning (in seconds if it's a
    transcript of a recording, or in characters if it's a text document)}
  \item{confidence}{A rating from 0-100 of the confidence of the offset,
    e.g. 10: default value, 50: force-aligned, 100: manually aligned}
}
\seealso{\link{labbcat.getAnnotations}}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Get the first 20 orthography tokens in UC427_ViktoriaPapp_A_ENG.eaf
orthography <- labbcat.getAnnotations(labbcat, "UC427_ViktoriaPapp_A_ENG.eaf", "orthography", 20, 0)

## Get the start anchors the above tokens
word.starts <- labbcat.getAnchors(labbcat, "UC427_ViktoriaPapp_A_ENG.eaf", orthography$startId)

}
\keyword{graph}
\keyword{transcript}
