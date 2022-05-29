package com.thisjustin.amongifier.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import com.thisjustin.amongifier.Amongifier;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@RestController
public class AmongifierController {

    @GetMapping("")
    public String defaultPage() {
        return "You shouldn't be here!";
    }

    @GetMapping("/amongifier/version")
    public String version() {
        return "idk bro, 1.0.0 ?";
    }

    @PostMapping("/amongifier/add")
    public String amongify(@RequestBody String base64Image) {
        String imagePortion = base64Image.substring(base64Image.indexOf(",") + 1);
        byte[] imageBytes = DatatypeConverter.parseBase64Binary(imagePortion);
        Amongifier a = new Amongifier();
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

            img = a.format(img);
            BufferedImage amongified = a.amongify(img);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(amongified, "png", os);
            String s = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
            //System.out.println(s);
            return s;
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "Fail: " + sw.toString();
        }
    }
}
