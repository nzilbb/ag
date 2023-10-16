# GoogleTranscriber

This is a nzilbb.ag.automation.Transcriber implementation that uses 
[Google Speech To Text](https://cloud.google.com/speech-to-text/) 

## Building

Running automated tests requires a valid Google Cloud account and an associated key
enabled for Google Speech and Google Storage.

### Building without automated tests

To build the module without automated tests:

```
mvn package -Dmaven.test.skip
```

### Building with automated tests

In order to build the module including automated tests:

1. Create a Google Cloud account: <https://cloud.google.com>
1. Create a 'project': <https://console.cloud.google.com/>
1. Update <src/test/java/nzilbb/transcriber/google/TestGoogleTranscriber.java> 
   setting the `projectId` to natch your project
1. In the poject dashboard menu, select *APIs and Services*
1. Select the *Credentials* option
1. Under *Service Accounts* click *Manage service accounts*
1. Click the *Create Service Account* link at the top
1. Give the Service Account a descriptive name (e.g. "Transcription") and an account ID
   (e.g. "transcription")
1. Click *Done* to create the account
1. Once created, click the *Keys* seciton of the account
1. Click *Add key*
1. Select *Create new key*
1. Select *JSON*
1. Download the resulting .json file, and save it in your home directory with the name
    `GoogleTranscriber-key.json`

Once this is done, building with tests can be achieved with the command:

```
mvn package
```
