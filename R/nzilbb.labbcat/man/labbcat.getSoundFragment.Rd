\name{labbcat.getSoundFragment}
\alias{labbcat.getSoundFragment}
\title{Gets a sound fragment from LaBB-CAT}
\description{
  Extracts part of a WAV file.
}
\usage{
labbcat.getSoundFragment(labbcat, id, start, end)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{The graph ID (transcript name) of the sound recording},
  \item{start}{The start time in seconds},
  \item{end}{The end time in seconds}
}
\value{The name of the file, which is saved in the current directory}
\examples{
## Get the 5 seconds starting from 10s after the beginning of a recording
wav.file <- labbcat.getSoundFragment(labbcat, "AP2505_Nelson.eaf", 10.0, 5.0)
}
\keyword{sample}
\keyword{sound}
\keyword{fragment}
\keyword{wav}
