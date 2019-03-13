echo delete editor backup files...
find . -name "*~" -exec rm \{\} \;

echo build nzilbb.labbcat ...
R CMD build nzilbb.labbcat

echo check nzilbb.labbcat ...
R CMD check --as-cran nzilbb.labbcat_*.tar.gz

echo move package to bin
mv nzilbb.labbcat_*.tar.gz ../bin/nzilbb.labbcat.tar.gz

echo finished.
