\name{labbcat.getSoundFragment}
\alias{labbcat.getSoundFragment}
\title{Gets a sound fragment from LaBB-CAT}
\description{
  Extracts part of a WAV file.
}
\usage{
labbcat.getSoundFragment(labbcat, id, start, end, sampleRate = NULL)
}
\arguments{
  \item{labbcat}{A LaBB-CAT instance object previously created by a call to labbcat.instance},
  \item{id}{The graph ID (transcript name) of the sound recording},
  \item{start}{The start time in seconds},
  \item{end}{The end time in seconds}
  \item{sampleRate}{Optional sample rate in Hz - if a positive integer, then the
    result is a mono file with the given sample rate }
}
\value{The name of the file, which is saved in the current directory}
\examples{
## Connect to LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", "demo", "demo")

## Get the 5 seconds starting from 10s after the beginning of a recording
wav.file <- labbcat.getSoundFragment(labbcat, "AP2505_Nelson.eaf", 10.0, 5.0)

## Get the 5 seconds starting from 10s as a mono 22kHz file
wav.file <- labbcat.getSoundFragment(labbcat, "AP2505_Nelson.eaf", 10.0, 5.0, 22050)
}
\keyword{sample}
\keyword{sound}
\keyword{fragment}
\keyword{wav}
