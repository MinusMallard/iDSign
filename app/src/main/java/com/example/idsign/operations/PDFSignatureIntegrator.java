package com.example.idsign.operations;

import android.util.Log;

import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.ExternalBlankSignatureContainer;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;



public class PDFSignatureIntegrator {

    static String TAG = "Integrating Signatures";


    public static void embedSignatureWithField(String pdfFilePath, String signedPdfFilePath, byte[] signatureBytes, String signerName, String signingTimestamp) throws IOException, GeneralSecurityException, GeneralSecurityException {
        PdfReader reader = new PdfReader(pdfFilePath);
        PdfSigner signer = new PdfSigner(reader, new FileOutputStream(signedPdfFilePath), new StampingProperties().useAppendMode());

        // Prepare the visual signature appearance
        PdfSignatureAppearance appearance = signer.getSignatureAppearance()
                .setReason("Document signed by " + signerName)
                .setLocation("Location")
                .setPageRect(new Rectangle(50, 50, 200, 100)) // Adjust the rectangle for the visual signature location
                .setPageNumber(1)
                .setLayer2Text("Signed by: " + signerName + "\nTimestamp: " + signingTimestamp)
                .setLayer2Font(PdfFontFactory.createFont())
                .setSignatureGraphic(null);  // Optionally add a graphic image of the signature

        signer.setFieldName("sigField");

        // Embed the signature bytes into the document metadata
        String encodedSignature = Base64.getEncoder().encodeToString(signatureBytes);
        PdfDocumentInfo pdfInfo = signer.getDocument().getDocumentInfo();
        pdfInfo.setMoreInfo("DigitalSignature", encodedSignature);  // Embedding signature in metadata

        // Create an external blank signature container, i.e., only the visual signature without changing the hash
        ExternalBlankSignatureContainer external = new ExternalBlankSignatureContainer(PdfName.Adobe_PPKLite, PdfName.Adbe_pkcs7_detached);
        signer.signExternalContainer(external, 8192);

        reader.close();
    }

    public static byte[] extractSignature(String signedPdfFilePath) throws IOException {
        PdfReader reader = new PdfReader(signedPdfFilePath);
        PdfDocument pdfDoc = new PdfDocument(reader);
        String encodedSignature = pdfDoc.getDocumentInfo().getMoreInfo("DigitalSignature");

        if (encodedSignature == null) {
            Log.e(TAG, "No signature found in the PDF metadata.");
            return null;
        }

        byte[] signatureBytes = Base64.getDecoder().decode(encodedSignature);
        reader.close();
        return signatureBytes;
    }

//    public static void signPDF(String src, String dest, byte[] signature, String identity) throws IOException, DocumentException {
//        PdfReader reader = null;
//        PdfStamper stamper = null;
//        FileOutputStream os = null;
//
//        try {
//            Log.d(TAG, "Going to Create Reader and Stamper");
//
//            // Create reader and stamper for existing PDF
//            reader = new PdfReader(src);
//            os = new FileOutputStream(dest);
////            stamper = new PdfStamper(reader, os, '\0', true);
//            stamper = PdfStamper.createSignature(reader, os, '\0', null, true);
//            Log.d(TAG, "Stamper Created Successfully");
//
//            // Create a new signature dictionary
//            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
//            appearance.setReason("Document signed digitally");
//            appearance.setLocation("Location");
//            Log.d(TAG, "Signature Appearance Set Successfully");
//
//            // Add visible signature text
//            addVisibleSignature(stamper, identity);
//            Log.d(TAG, "Visible Signature Marker Added");
//
//            // Convert Date to Calendar
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(new Date());
//
//            Log.d(TAG, "Creating Custom Signature Dictionary");
//            // Add custom signature dictionary
//            PdfDictionary dic = new PdfDictionary();
//            dic.put(new PdfName("Contents"), new PdfString(signature).setHexWriting(true));
//            dic.put(new PdfName("Identity"), new PdfString(identity));
//            dic.put(new PdfName("Date"), new PdfString(new PdfDate(calendar).toString()));
//            dic.put(new PdfName("Filter"), new PdfName("Adobe.PPKLite"));
//            dic.put(new PdfName("SubFilter"), new PdfName("adbe.pkcs7.detached"));
//            Log.d(TAG, "Signature Dictionary Created");
//
//            appearance.setCryptoDictionary(dic);
////            appearance.preClose(new HashMap<PdfName, Integer>());
//
//            // EXTRA
//            HashMap<PdfName, Integer> exc = new HashMap<>();
//
//            exc.put(PdfName.CONTENTS, (signature.length + 2) * 2 + 2);
//
//            appearance.preClose(exc);
//            // ***********
//            Log.d(TAG, "Signature Dictionary Set");
//
//            // EXTRA
//
//
//            // Post close the appearance to finalize the signature
//
//            PdfSignatureAppearance.ExternalSignatrueContainer external = new PdfSignatureAppearance.ExternalSignatureContainer() {
//
//                public void modifySigningDictionary(PdfDictionary signDic) {
//
//                }
//
//                public byte[] sign(InputStream is) {
//
//                    return signature;
//
//                }
//
//            };
//
//            appearance.setExternalSignatureContainer(external);
//
//            appearance.close(new FileOutputStream(dest), null);
//
//        } catch (IOException | DocumentException e) {
//            Log.e(TAG, "Exception Occurred", e);
//            throw e;
//        } finally {
//            if (stamper != null) {
//                try {
//                    stamper.close();
//                    Log.d(TAG, "Stamper Closed");
//                } catch (Exception e) {
//                    Log.e(TAG, "Error Closing Stamper", e);
//                }
//            }
//            if (reader != null) {
//                try {
//                    reader.close();
//                    Log.d(TAG, "Reader Closed");
//                } catch (Exception e) {
//                    Log.e(TAG, "Error Closing Reader", e);
//                }
//            }
//            if (os != null) {
//                try {
//                    os.close();
//                    Log.d(TAG, "File Output Stream Closed");
//                } catch (Exception e) {
//                    Log.e(TAG, "Error Closing File Output Stream", e);
//                }
//            }
//        }
//    }
//    private static void addVisibleSignature(PdfStamper stamper, String identity) throws DocumentException, IOException {
//        // Create a visible signature field
//        PdfFormField sigField = PdfFormField.createSignature(stamper.getWriter());
//        sigField.setFieldName("Signature");
//        Rectangle rect = new Rectangle(36, 748, 144, 780);
//        sigField.setWidget(rect, PdfAnnotation.HIGHLIGHT_INVERT);
//        sigField.setFlags(PdfAnnotation.FLAGS_PRINT);
//        sigField.setFieldFlags(PdfFormField.FF_REQUIRED);
//        stamper.addAnnotation(sigField, 1);
//
//        // Add text marker with signer information
//        ColumnText.showTextAligned(stamper.getOverContent(1),
//                Element.ALIGN_LEFT,
//                new Phrase("Signed by: " + identity + "\nDate: " + new Date().toString()),
//                36, 732, 0);
//    }
}
