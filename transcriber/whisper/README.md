# Whisper Transcriber

In September 2022, [OpenAI](https://openai.com/) published a [blog entry](https://openai.com/blog/whisper/) about [Whisper](https://github.com/openai/whisper), a DNN ASR system trained on a huge collection of recordings in multiple languages, many with transcripts in both the original language and translated into English.

The DNN can do two tasks:

- Speech To Text (STT) - i.e. transcribe recordings, and
- Translation of non-English speech to English

For our purposes, there are two interesting things about this system:

1. The reported performance is on par with cloud-based systems like [Google STT](https://cloud.google.com/speech-to-text/), and
2. it's not cloud-based - the models are downloaded and STT happens on the local machine.

This is a [Transcriber](https://nzilbb.github.io/ag/apidocs/nzilbb/ag/automation/Transcriber.html) implementation that uses Whisper for non-cloud-based transcription.
