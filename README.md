# Trireme
Trireme is the product of four students' Secure Software Engineering class final project. It is a lightweight, secure peer-to-peer file transfer application that runs on any platform.

## Building
`./gradlew shadowjar`

Windows .exe is packaged using Launch4j.

## Usage
Java 21 or greater is required on all platforms.

### Receiving
- Import sender's certificate
- Enter receive port
- Start listening for incoming transfers

### Sending
- Export certificate, give it to recipient
- Enter receiver host and port
- Select file
- Send
