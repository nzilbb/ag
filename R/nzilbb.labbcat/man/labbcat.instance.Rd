\name{labbcat.instance}
\alias{labbcat.instance}
\title{Connect to LaBB-CAT}
\description{
  Connects to the given LaBB-CAT instance, and returns and object that must be used for all other functions.
}
\usage{
labbcat.instance(url, username = NULL, password = NULL)
}
\arguments{
  \item{url}{URL to the LaBB-CAT instance.}
  \item{username}{the LaBB-CAT username, if it is password-protected.}
  \item{password}{the LaBB-CAT password, if it is password-protected.}
}
\value{An object that can be passed as the labbcat parameter for other functions in this package}
\examples{
## connect to an open intance of LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/")

## connect to a password-protected instance of LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", 
        username="demo", password="demo")
}
\keyword{connect}
\keyword{username}
\keyword{password}