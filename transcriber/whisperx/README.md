# WhisperXTranscriber

Automatic transcriber that uses the[WhisperX](https://arxiv.org/abs/2303.00747)
ASR system to perform speech-to-text and diarize it.

## Building

Running automated tests requires a valid Hugging Face token for
downloading models for diarization.

### Building without automated tests

To build the module without automated tests:

```
mvn package -Dmaven.test.skip
```

### Building with automated tests

In order to build the module including automated tests:

1. Create a Hugging Face account: <https://cloud.google.com>
1. Generate a token: <https://huggingface.co/docs/hub/security-tokens>\
   It is a short text string starting `hf_`
1. Save it in a file in your home directory called `.huggingfacetoken`

Once this is done, building with tests can be achieved with the command:

```
mvn package
```

