\name{nzilbb.labbcat-package}
\alias{nzilbb.labbcat-package}
\alias{nzilbb.labbcat}
\docType{package}
\title{
\packageTitle{nzilbb.labbcat}
}
\description{
\packageDescription{nzilbb.labbcat}
}
\details{

The DESCRIPTION file:
\packageDESCRIPTION{nzilbb.labbcat}
\packageIndices{nzilbb.labbcat}

LaBB-CAT is a web-based language corpus management system and this
package provides access to data stored in a LaBB-CAT instance.

As LaBB-CAT instances are usually password-protected, the function
\code{labbcat.instance} must be used first of all to create an object
that provides access to a specific instance of LaBB-CAT with specific
credentials.  This object is then used as the first argument for all
other function calls.

}
\author{
\packageAuthor{nzilbb.labbcat}

Maintainer: \packageMaintainer{nzilbb.labbcat}
}
\references{
\cite{Robert Fromont and Jennifer Hay, "{ONZE Miner}: the development of a browser-based research tool", 2008}

\cite{Robert Fromont and Jennifer Hay, "{LaBB-CAT: an Annotation Store}", 2012}
}
\keyword{ package }
\examples{

## connect to LaBB-CAT
labbcat <- labbcat.instance("http://localhost:8080/labbcat/")

## List corpora
corpora <- labbcat.getCorpusIds(labbcat)

}
