\name{labbcat.getParticipantIds}
\alias{labbcat.getParticipantIds}
\title{Gets a list of participant IDs}
\description{
  Gets a list of participant IDs.
}
\usage{
labbcat.getParticipantIds(labbcat)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance}
}
\value{A list of participant IDs}
\examples{
## List all speakers
speakers <- labbcat.getParticipantIds(labbcat)
}
\keyword{speaker}
\keyword{participant}
