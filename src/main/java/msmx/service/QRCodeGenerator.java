package msmx.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

public class QRCodeGenerator {

    public static String generateQRCodeImage(String text, String uploadDir)
            throws WriterException, IOException {
        
        System.out.println("生成二维码内容: " + text);
        System.out.println("保存目录: " + uploadDir);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);

        String fileName = UUID.randomUUID().toString() + ".png";
        Path path = FileSystems.getDefault().getPath(uploadDir, fileName);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        
        System.out.println("二维码已保存: " + path.toAbsolutePath());

        return fileName;
    }
}