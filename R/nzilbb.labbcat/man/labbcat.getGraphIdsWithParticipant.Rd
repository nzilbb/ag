\name{labbcat.getGraphIdsWithParticipant}
\alias{labbcat.getGraphIdsWithParticipant}
\title{Gets a list of IDs of graphs that include the given participant}
\description{
  Gets a list of IDs of graphs (i.e. transcript names) that include the given participant.
}
\usage{
labbcat.getGraphIdsWithParticipant(labbcat, id)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{A participant ID}
}
\value{A list of graph IDs}
\seealso{\link{labbcat.getParticipantIds}}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## List transcripts in which UC427_ViktoriaPapp_A_ENG speaks
transcripts <- labbcat.getGraphIdsWithParticipant(labbcat, "UC427_ViktoriaPapp_A_ENG")
}
\keyword{graph}
\keyword{transcript}
