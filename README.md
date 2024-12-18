# Trireme
Trireme is the product of four students' Secure Software Engineering class final project. It is a lightweight, secure peer-to-peer file transfer application that runs on any platform.

## Building
`./gradlew build`

## Usage
### Receiving
- Import sender's certificate
- Enter receive port
- Start listening for incoming transfers

### Sending
- Export certificate, give it to recipient
- Enter receiver host and port
- Select file
- Send
