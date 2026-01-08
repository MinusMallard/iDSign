# SecureXfer: Secure NFC & Bluetooth Framework for Digital Signing

[cite_start]**SecureXfer** is a hybrid mobile-based framework designed for secure and efficient in-person digital document signing[cite: 9]. [cite_start]By leveraging **NFC Host Card Emulation (HCE)** for proximity-based mutual authentication and **Bluetooth** for fast data transfer, it provides a robust alternative to vulnerable web-based signing systems[cite: 9, 13, 15].

> [cite_start]**Reference:** This project is an implementation of the framework proposed in the research paper *"SecureXfer: Secure NFC RSA Mobile-based Framework for Digital Signing of Documents"* by researchers at DTU, Delhi[cite: 1, 2, 3].

---

## System Architecture & Workflow

[cite_start]The SecureXfer framework operates through three distinct phases to ensure data integrity and security[cite: 110].



### 1. Registration Phase
[cite_start]Before any signing occurs, both the **Signer** and **Signee** register with a central server[cite: 132]. [cite_start]The server generates and distributes **RSA key pairs** and **digital certificates** to both devices securely[cite: 133].

* [cite_start]**Signer (S)**: Receives private key $S_S$, public key $P_S$, and certificate $C_S$[cite: 116, 123].
* [cite_start]**Signee (D)**: Receives private key $S_D$, public key $P_D$, and certificate $C_D$[cite: 119, 123].



### 2. Mutual Authentication (NFC Handshake)
[cite_start]When the devices tap, they initiate a mutual authentication process over NFC to establish a secure session key (HTK)[cite: 135]. [cite_start]This phase uses **Application Protocol Data Unit (APDU)** commands following the **ISO/IEC 14443** standard[cite: 49, 50].



**The Handshake Protocol:**
1.  [cite_start]**Initiation**: The Signer (HCE Reader) selects the Signee Application (HCE Card) using its Application Identifier (AID)[cite: 128, 189].
2.  **Cert Exchange**: The Signer sends its certificate $C_S$ and a certificate request to the Signee: $R_S = C_S || [cite_start]REQ_{C_D}$[cite: 190, 192].
3.  [cite_start]**Signee Challenge**: The Signee extracts $P_S$ and generates a random number $N_1$[cite: 194]. It sends $N_1$ encrypted with $P_S$ as a challenge, along with its certificate $C_D$: $R_D = C_D || [cite_start]E_{P_S}(N_1)$[cite: 195, 197].
4.  [cite_start]**Signer Response**: The Signer decrypts $N_1$, increments it ($N_1+1$), generates a new random challenge $N_2$, and sends both back encrypted with $P_D$: $R_S = E_{P_D}(N_1+1 || N_2)$[cite: 199, 200, 202].
5.  [cite_start]**Final Verification**: The Signee decrypts $R_S$, verifies $N_1+1$, and increments $N_2$ ($N_2+1$)[cite: 204]. [cite_start]It sends this back encrypted with $P_S$: $R_D = E_{P_S}(N_2+1)$[cite: 181, 183].
6.  [cite_start]**Key Establishment**: The Signer verifies $N_2+1$, generates a symmetric **Session Key (HTK)**, and sends it encrypted with $P_D$: $R_S = E_{P_D}(HTK)$[cite: 185, 186, 188].

### 3. Digital Signing Phase (Bluetooth)
[cite_start]After authentication, the devices switch to Bluetooth for faster transfer of large document files[cite: 58].



1.  [cite_start]**Document Sharing**: The Signee encrypts the document with the HTK and shares it via Bluetooth[cite: 206]. [cite_start]The Signer reassembles the fragmented packets into the final document[cite: 175, 207].
2.  [cite_start]**Signature Generation**: The Signer computes a document hash $H_{DOC}$ and encrypts it with its private key $S_S$ to create the signature[cite: 208, 209]. [cite_start]This is further encrypted with HTK and sent to the Signee: $R_S = E_{HTK}(E_{S_S}(H_{DOC}))$[cite: 210, 212].
3.  [cite_start]**Signature Verification**: The Signee decrypts the signature using HTK and integrates it with the document[cite: 214]. [cite_start]Verification is done by comparing the document hash with the decrypted signature using $P_S$[cite: 215, 216].

---

## Security Features

[cite_start]The framework is designed to mitigate several critical security threats[cite: 236]:

* [cite_start]**Mutual Authentication**: Both parties must prove identity using RSA certificates before any data exchange begins[cite: 238].
* [cite_start]**MiTM Protection**: NFC's physical proximity (~4cm) ensures data is only transmitted to the tapped device, preventing remote interception[cite: 23, 239].
* [cite_start]**Data Privacy**: Every piece of data is encrypted with secure keys throughout the process to maintain confidentiality[cite: 241].
* [cite_start]**Relay Attack Defense**: Secure symmetric channels and mutual authentication protocols prevent attackers from intercepting or replaying the connection[cite: 243].

---

## Performance Evaluation

[cite_start]Based on experimental analysis for a **1MB document** size[cite: 229, 230]:

* [cite_start]**Mutual Authentication Time**: 0.800 seconds[cite: 230].
* [cite_start]**Bluetooth File Transfer**: 11.00 seconds[cite: 230].
* [cite_start]**Total Signing Time**: 12.500 seconds[cite: 230].
* [cite_start]**Computation Cost**: SecureXfer (2.42 ms) provides a balanced approach, outperforming competitors like DSign (30.30 ms) and PDFGuard (3.66 ms)[cite: 261, 269].

### Storage Requirements
[cite_start]The approximate storage required per device is **1664 bytes**, including public/private keys (256 bytes each), certificates (1024 bytes), and symmetric session keys (128 bytes)[cite: 221, 223, 249, 250].

---

## Getting Started

### Prerequisites
* [cite_start]Two Android devices with NFC and Bluetooth capabilities[cite: 92].
* Android Studio Flamingo or later.
* [cite_start]Registration with a SecureXfer server to obtain RSA certificates[cite: 127].

### Usage
1.  [cite_start]**Signer Role**: Set the device to **HCE Reader** mode[cite: 128].
2.  [cite_start]**Signee Role**: Set the device to **HCE Card** mode[cite: 130].
3.  [cite_start]**Action**: Tap the two devices together to initiate the NFC handshake[cite: 174].
4.  [cite_start]**Completion**: Once the mutual authentication succeeds, the Bluetooth transfer and digital signature integration will proceed automatically[cite: 206].

---

## Citation
```bibtex
@article{securexfer2024,
  title={SecureXfer: Secure NFC RSA Mobile-based Framework for Digital Signing of Documents},
  author={Jaiswal, Harshit and Kumar, Rishav and Sharma, Himanshu and Sethia, Divyashikha},
  institution={DTU, Delhi, India},
  year={2024}
}
