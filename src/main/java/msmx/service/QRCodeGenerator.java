package msmx.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

public class QRCodeGenerator {

    public static String generateQRCodeImage(String text, String uploadDir)
            throws WriterException, IOException {
        return generateQRCodeImageWithTitle(text, null, uploadDir);
    }

    public static String generateQRCodeImageWithTitle(String text, String title, String uploadDir)
            throws WriterException, IOException {
        return generateQRCodeImageWithTitleAndLogo(text, title, uploadDir, null);
    }
    
    public static String generateQRCodeImageWithTitleAndLogo(String text, String title, String uploadDir, String logoPath)
            throws WriterException, IOException {
        
        int qrSize = 400;
        int titleHeight = 60;
        int totalSize = 400;
        int totalHeight = totalSize + titleHeight;
        int padding = 20;
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
        hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
        
        BufferedImage combinedImage = new BufferedImage(totalSize, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = combinedImage.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, totalSize, totalHeight);
        
        if (title != null && !title.isEmpty()) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(236, 72, 153));
            Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 18);
            g2d.setFont(titleFont);
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int textWidth = fontMetrics.stringWidth(title);
            int textX = (totalSize - textWidth) / 2;
            int textY = (titleHeight + fontMetrics.getAscent()) / 2;
            g2d.drawString(title, textX, textY);
            
            g2d.setColor(new Color(236, 72, 153, 80));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(padding, titleHeight - 8, totalSize - padding, titleHeight - 8);
        }
        
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        int qrX = (totalSize - qrSize) / 2;
        int qrY = titleHeight;
        g2d.drawImage(qrImage, qrX, qrY, null);
        
        BufferedImage logoImage = null;
        
        if (logoPath != null && !logoPath.isEmpty()) {
            System.out.println("尝试加载logo: " + logoPath);
            
            logoImage = loadLogoFromFileSystem(logoPath, uploadDir);
            
            if (logoImage == null) {
                logoImage = loadLogoFromClasspath(logoPath);
            }
            
            if (logoImage != null) {
                System.out.println("成功加载logo图片");
            }
        }
        
        if (logoImage == null) {
            System.out.println("使用默认logo");
            logoImage = createDefaultLogo();
        }
        
        int logoSize = 50;
        int logoX = (totalSize - logoSize) / 2;
        int logoY = titleHeight + (qrSize - logoSize) / 2;
        
        BufferedImage scaledLogo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D logoG2d = scaledLogo.createGraphics();
        logoG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        logoG2d.drawImage(logoImage, 0, 0, logoSize, logoSize, null);
        logoG2d.dispose();
        
        g2d.drawImage(scaledLogo, logoX, logoY, null);
        
        g2d.dispose();
        
        String fileName = UUID.randomUUID().toString() + ".png";
        Path path = FileSystems.getDefault().getPath(uploadDir, fileName);
        ImageIO.write(combinedImage, "PNG", path.toFile());
        
        return fileName;
    }
    
    private static BufferedImage loadLogoFromFileSystem(String logoPath, String uploadDir) {
        try {
            String fullLogoPath = logoPath;
            
            if (logoPath.startsWith("/uploads/")) {
                fullLogoPath = uploadDir + "/" + logoPath.substring("/uploads/".length());
            } else if (logoPath.startsWith("uploads/")) {
                fullLogoPath = uploadDir + "/" + logoPath.substring("uploads/".length());
            } else if (!logoPath.contains(":")) {
                fullLogoPath = uploadDir + "/" + logoPath;
            }
            
            File logoFile = new File(fullLogoPath);
            System.out.println("检查文件路径: " + logoFile.getAbsolutePath());
            
            if (logoFile.exists()) {
                System.out.println("文件存在，尝试读取");
                BufferedImage img = ImageIO.read(logoFile);
                if (img != null) {
                    System.out.println("成功读取图片，尺寸: " + img.getWidth() + "x" + img.getHeight());
                    return img;
                } else {
                    System.out.println("图片读取为null，可能是格式不支持");
                }
            } else {
                System.out.println("文件不存在");
            }
        } catch (Exception e) {
            System.out.println("从文件系统加载logo失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private static BufferedImage loadLogoFromClasspath(String logoPath) {
        try {
            String resourcePath = logoPath;
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }
            
            if (resourcePath.startsWith("/uploads/")) {
                resourcePath = "/static" + resourcePath;
            }
            
            System.out.println("尝试从classpath加载: " + resourcePath);
            
            InputStream is = QRCodeGenerator.class.getResourceAsStream(resourcePath);
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    System.out.println("从classpath成功读取图片，尺寸: " + img.getWidth() + "x" + img.getHeight());
                    return img;
                } else {
                    System.out.println("classpath图片读取为null");
                }
                is.close();
            } else {
                System.out.println("classpath资源不存在: " + resourcePath);
            }
        } catch (Exception e) {
            System.out.println("从classpath加载logo失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private static BufferedImage createDefaultLogo() {
        int size = 50;
        BufferedImage logo = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = logo.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gradient = new GradientPaint(0, 0, new Color(236, 72, 153), size, size, new Color(168, 85, 247));
        g2d.setPaint(gradient);
        g2d.fillOval(0, 0, size, size);
        
        g2d.setColor(Color.WHITE);
        
        int centerX = size / 2;
        int centerY = size / 2;
        
        int heartW = 12;
        int heartH = 12;
        int[] heartX = {
            centerX,
            centerX - heartW, centerX - heartW*9/8, centerX - heartW*3/4,
            centerX, centerX + heartW*3/4, centerX + heartW*9/8, centerX + heartW, centerX
        };
        int[] heartY = {
            centerY - heartH*3/4,
            centerY - heartH*7/6, centerY - heartH*5/4 - 1, centerY - heartH,
            centerY - heartH/3, centerY - heartH, centerY - heartH*5/4 - 1, centerY - heartH*7/6, centerY - heartH*3/4
        };
        g2d.fillPolygon(heartX, heartY, heartX.length);
        
        int stick = 3;
        int bodyH = 8;
        
        int leftX1 = centerX - heartW/2 - stick/2;
        g2d.fillRect(leftX1, centerY + heartH/3, stick, bodyH);
        g2d.fillOval(leftX1 - 3, centerY + heartH/3 - 6, 8, 8);
        
        int rightX1 = centerX + heartW/2 - stick/2;
        g2d.fillRect(rightX1, centerY + heartH/3, stick, bodyH);
        g2d.fillOval(rightX1 - 3, centerY + heartH/3 - 6, 8, 8);
        
        g2d.dispose();
        return logo;
    }
}