\name{labbcat.instance}
\alias{labbcat.instance}
\title{Connect to LaBB-CAT}
\description{
  Connects to the given LaBB-CAT instance, and returns and object that
  must be used for all other functions.

  If a username and password are not passed, and the LaBB-CAT instance
  is password-protected (and the function is called in interactive
  mode), then the user will be prompted for the username and password.

  This is the recommended method for accessing password-protected LaBB-CAT
  instances, in order to avoid saving passwords in script files. The
  username and password parameters are provided for cases where the
  script is not run in interactive mode.
  
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
## connect to an open or password-protected instance of LaBB-CAT
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/")

## connect to a password-protected instance of LaBB-CAT with explicit credentials
labbcat <- labbcat.instance("https://labbcat.canterbury.ac.nz/demo/", 
        username="demo", password="demo")
}
\keyword{connect}
\keyword{username}
\keyword{password}