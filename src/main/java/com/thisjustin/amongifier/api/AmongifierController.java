package com.thisjustin.amongifier.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Base64;
import java.util.HashSet;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import com.thisjustin.amongifier.Amongifier;
import com.thisjustin.amongifier.Helper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.awt.Point;

@RestController
public class AmongifierController {
    private HashSet<String> keys = new HashSet<String>();
    private HashMap<String, AmongifierThread> keyMap = new HashMap<>();

    @GetMapping("")
    public String defaultPage() {
        return "You shouldn't be here!";
    }

    @GetMapping("/amongifier/version")
    public String version() {
        return "idk bro, 1.0.0 ?";
    }

    /**
     * Takes in an image to process, then returns a unique key. It is up to the
     * sender to then ping the key multiple times to get the result.
     * 
     * @param base64Image
     * @return
     */
    @PostMapping("/amongifier/add")
    public String amongify(@RequestBody String responseJSON) {
        JSONParser parser = new JSONParser();
        JSONObject json = new JSONObject();
        try {
            json = (JSONObject) parser.parse(responseJSON);
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        String base64Image = json.get("image").toString();
        int smoothing = Integer.parseInt(json.get("smooth").toString());
        int borderSmoothing = Integer.parseInt(json.get("border").toString());
        double midPointX = Double.parseDouble(json.get("midPointX").toString());
        double midPointY = Double.parseDouble(json.get("midPointY").toString());
        String points = json.get("points").toString();
        boolean forceAspectRatio = json.get("aspectRatio").toString().contains("true");
        boolean mirror = json.get("mirror").toString().contains("true");

        System.out.println("Mid point: (" + midPointX + "," + midPointY + ")");

        String imagePortion = base64Image.substring(base64Image.indexOf(",") + 1);
        byte[] imageBytes = DatatypeConverter.parseBase64Binary(imagePortion);
        Amongifier a = new Amongifier(smoothing, borderSmoothing, forceAspectRatio, midPointX, midPointY);
        String key = generateKey();

        System.out.println("Starting...");

        AmongifierThread thread = new AmongifierThread() {
            public void run() {
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    Point[] borderPoints = Helper.parsePoints(points, img.getWidth(), img.getHeight());
                    System.out.println("Cutting out face...");
                    img = a.cutOutFace(img, borderPoints);
                    System.out.println("Face cut out successfully.");

                    System.out.println("Formatting Image...");
                    img = a.format(img);
                    if (mirror) {
                        img = Helper.horizontalMirror(img);
                    }
                    System.out.println("Image formatted successfully.");
                    BufferedImage amongified = a.amongify(img);

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(amongified, "png", os);
                    String s = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());

                    this.complete = true;
                    this.base64Image = s;
                } catch (IOException e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    this.failed = true;
                    this.errorMessage = sw.toString();
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    this.failed = true;
                    this.errorMessage = sw.toString();
                }
                try {
                    long duration = 10000;
                    Thread.sleep(duration);
                    if (keys.contains(key)) {
                        System.out.println("Key " + key + " idle for " + (duration / 1000) + " seconds. Discarding thread.");
                        clearKey(key);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.a = a;
        thread.start();
        keyMap.put(key, thread);
        return key;
    }

    /**
     * Takes in a key given to a user to check back later with.
     * 
     * @param key
     * @return If complete, returns the image as base64. If incomplete, returns
     *         "Incomplete, check back later." If the key is invalid, returns
     *         "Failed request... Something went wrong!"
     */
    @PostMapping("/amongifier/request")
    public String requestResponse(@RequestBody String keyJSON) {
        JSONParser parser = new JSONParser();
        JSONObject json = new JSONObject();
        try {
            json = (JSONObject) parser.parse(keyJSON);
        } catch (ParseException e1) {
            System.out.println("Illegal request detected: " + keyJSON);
            return "Illegal request.";
        }

        String key = json.get("key").toString();

        AmongifierThread a = keyMap.get(key);
        if (a != null && a.complete) {
            String image = a.base64Image;
            clearKey(key);
            return image;
        } else if (a == null || a.failed) {
            clearKey(key);
            return "Failed|" + a.errorMessage;
        } else {
            return "Incomplete|" + a.a.getStatus();
        }
    }

    /**
     * Returns a unique string key (10-digit number).
     * 
     * @return The damn key
     */
    private String generateKey() {
        String s = "";
        for (int i = 0; i < 10; i++) {
            s += "" + (int) (Math.random() * 10);
        }
        if (keys.contains(s)) {
            s = generateKey();
        }
        keys.add(s);
        return s;
    }

    private void clearKey(String key) {
        keys.remove(key);
        keyMap.remove(key);
    }

    class AmongifierThread extends Thread {

        public boolean complete = false;
        public String base64Image = "";

        public boolean failed = false;
        public String errorMessage = "";

        public Amongifier a;

        public int getPercentDone() {
            return 50;
        }
    }
}