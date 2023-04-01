# HLSJava
Hierarhical Location Service application for Communication and Information Security course

## Course: Communication and Information Security
### Semester 2 5782

## List of Students in the group

Andy Thaok  adigaandyt 311555221


### List of tasks and who worked on what
| Task | Performed by | Hours
|------|--------------|-------
| AAA  | Student 1    | XXX Hours

## Instructions

### Compilation instructions
The project was compiled using Intellij

File -> Project Structure -> Artifacts -> + -> Jar -> From modules with dependencies - > Pick main class - > in the Director for META-INF change 'java' at the end of the path to 'resources' -> OK

Build -> Build Artifacts... -> HLSJava:Jar -> Build

### Running instructions
1. make sure config file and javafx folder are in the same directory as the jar
2. put all the RSA keys in a folder called RSA_Keys
3. rsaPrivateKey is the nodes private key
4. X_rsaPublicKey is other PCs public key, where X is the name of the other PC
5.run runme.bat

### Configuration file format
First line is the secret key
Second line is the MAC key
Third line is the PCs name

The RSA keys are in the RSA_Keys folder in the jar directory
the nodes private key is named rsaPrivateKey
public keys named X_rsaPublicKey(X is the name of the other PC)

## Documentation

### Thread safety
Nothing added should effect thread safety


### How encryption and decryption work
Encryption:
bytes arrays are handled using ByteBuffer
5 arrays get made 
[IV] [DATA] [TIME] [RSA] [NAME] [MAC] 
[IV] is randomly generated (128bits)
[DATA] is either the message or the file data
[TIME] is the current date and time
[NAME] is the PCs name from the Config.txt
[RSA] is generated using [DATA|TIME|NAME] with SHA256withRSA and the PCs 4096bit private key
using the IV, [DATA|TIME|RSA|NAME] gets encrypted using a key from Config.txt, GCM for files, CBC for messages, 256 bit AES key for both
using [IV|enDATA|enTIME|enRSA|enNAME] MAC is generated using HmacSHA256 and a key from Config.txt (en for encrypted)
[IV|enDATA|enTIME|enRSA|enNAME|MAC] is returned 

Decryption:
using ByteBuffer the input is split 
[IV|enDATA|enTIME|enRSA|enNAME|MAC]
[IV] [enDATA] [enTIME] [enRSA] [enNAME] [MAC] 
using [IV|enDATA|enTIME|enRSA|enNAME], [MAC] and key from Config.txt the HMAC gets verified 
using IV and key from Config.txt, [enDATA|enTIME|enRSA|enNAME] gets decrypted (it's [enDATA] in the code since it's all encrypted together)
using [NAME] we get the senders public key from RSA_Keys folder
using [DATA|Timestamp|NAME], Public key of the sender and [RSA] we verify the RSA Signature
using [Timestamp] we take out the time portion and check if it's older than 5 second

if any of the checks fail the message isn't read, if it's a file it remains saved as an encrypted file

[DATA] is returned if all the checks pass

### How HMAC is performed
stated above


### How Public Key Digital Signatures are Produced and Checked

RSA Key pairs were generated and saved to file using
http://www.java2s.com/example/java/security/gen-rsa-key-pair-and-save-to-file.html
(switched 2048 to 4096)

check is stated above
