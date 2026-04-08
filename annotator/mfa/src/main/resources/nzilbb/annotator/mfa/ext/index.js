
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});
// are they a privileged user?
const url = "edit/index.html";
getText(url, (data) => {
  if (data) { // they can access the URL
    window.location = url;
  } else {
    getText("mfaVersion", version => {
      if (version) {
        document.getElementById("mfaVersion").innerHTML = version;
      } else {
        document.getElementById("mfaVersion").innerHTML = "Could not determine MFA version";
        document.getElementById("mfaVersion").className = "error";
      }
    });
  }
});
