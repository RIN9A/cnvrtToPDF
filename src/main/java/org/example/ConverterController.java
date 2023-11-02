package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.example.documentserver.managers.jwt.JWTutil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;

import java.util.UUID;

@CrossOrigin("*")
@Controller
public class ConverterController {

    @Value("${files.docservice.url.site}")
    private String docserviceSite;

    @Value("${files.docservice.url.api}")
    private String docserviceApiUrl;

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Value("${files.docservice.url.converter}")
    private String docServiceUrlConverter;
    @Value("${files.docservice.timeout}")
    private String docserviceTimeout;

    private int convertTimeout;
    @Autowired
    private ObjectMapper objectMapper;



    @PostMapping("/converter")
    public ConvertedDocument converter(@RequestBody final ConvertRequest request) {

        String link = request.getLink();
        String fileType = request.getInputType();
        String outputType = request.getOutputType();
        String key = UUID.randomUUID().toString();
        PayloadConvert payload = new PayloadConvert();
        payload.setAsync(true);
        payload.setFileType(fileType);
        payload.setKey(key);
        payload.setOutputType(outputType);
        payload.setTitle("Converted Document.pdf");
        payload.setUrl(String.valueOf(URI.create(link)));
        String token = generateToken(payload);
        ResponseConvert response = postConvertRequest(token);
        return new ConvertedDocument(response.getFileUrl());


    }

    private String generateToken(final PayloadConvert payload) {
        String token = "";
        try {
            token = new JWTutil().createToken(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }
    private ResponseConvert postConvertRequest(final String token) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(token))
                .uri(URI.create(docServiceUrlConverter))
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        ResponseConvert convertResponse = null;
        if (response != null) {
            convertResponse = new Gson().fromJson(response.body(), ResponseConvert.class);
        }

        return convertResponse;
    }
}



