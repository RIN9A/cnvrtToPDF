package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.example.documentserver.managers.jwt.JWTutil;

import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class FileController {
    @Value("${files.docservice.url.site}")
    private String docserviceSite;
    @Value("${files.docservice.url.converter}")
    private String docServiceUrlConverter;

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        JWTutil jwTutil = new JWTutil();
        String fileName = fileStorageService.storeFile(file);
        String fileType = Objects.requireNonNull(file.getOriginalFilename()).substring(fileName.lastIndexOf(".")).replace(".","");
        String outputType ="pdf";
        String key = UUID.randomUUID().toString();
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(fileName)
                .toUriString();


        PayloadConvert payload = new PayloadConvert();
        payload.setAsync(false);
        payload.setFiletype(fileType);
        payload.setKey(key);
        payload.setOutputtype(outputType);
        payload.setTitle(file.getOriginalFilename());
        payload.setUrl(fileDownloadUri);
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("url", payload.getUrl());
        map.put("outputtype", payload.getOutputtype());
        map.put("filetype", payload.getFiletype());
        map.put("title", payload.getTitle());
        map.put("key", payload.getKey());
        map.put("async", payload.isAsync());

        // add token to the body if it is enabled
        String token = jwTutil.createToken(map);
        payload.setToken(token);

        Map<String, Object> payloadMap = new HashMap<String, Object>();
        payloadMap.put("payload", map);
        String headerToken = jwTutil.createToken(payloadMap);

        ResponseConvert response = postConvertRequest(headerToken,payload);
        String newFileUri = response.getUrl();

        String newFileType = "." + response.getFileType();

        URL url = new URL(newFileUri);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        InputStream stream = connection.getInputStream();
        if (stream == null) {
            connection.disconnect();
            throw new RuntimeException("Input stream is null");
        }
        File newFile = fileStorageService.createFile(stream);
        String fileDownloadUriNew = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/downloadFile/")
                    .path(newFile.getName())
                    .toUriString();

        return new UploadFileResponse(newFile.getName(), fileDownloadUriNew,
                "application/pdf", file.getSize());
    }


    @SneakyThrows
    private ResponseConvert postConvertRequest(final String token, PayloadConvert body) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String bodyString = objectMapper.writeValueAsString(body);
        ResponseConvert responseConvert = new ResponseConvert("", "");
        URL url = null;
        java.net.HttpURLConnection connection = null;
        InputStream response = null;
        String jsonString = null;
        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);
        url = new URL(docserviceSite + docServiceUrlConverter);
        connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setFixedLengthStreamingMode(bodyByte.length);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization","Bearer " + token );
        connection.connect();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);  // write bytes to the output stream
            os.flush();  // force write data to the output stream that can be cached in the current thread
        }

        int statusCode = connection.getResponseCode();
        if (statusCode != HttpStatus.OK.value()) {  // checking status code
            connection.disconnect();
            throw new RuntimeException("Convertation service returned status: " + statusCode);
        }


        response = connection.getInputStream();  // get the input stream
        jsonString = convertStreamToString(response);
        JSONObject jsonObject = convertStringToJSON(jsonString);

            String respUri = (String) jsonObject.get("fileUrl");
            String fileType = (String) jsonObject.get("filetype");
            responseConvert.setUrl(respUri);
            responseConvert.setFileType(fileType);

        connection.disconnect();
        return responseConvert;

    }


    public JSONObject convertStringToJSON(final String jsonString) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);  // parse json string
        JSONObject jsonObj = (JSONObject) obj;  // and turn it into a json object

        return jsonObj;
    }


    public String convertStreamToString(final InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);  // create an object to get incoming stream
        StringBuilder stringBuilder = new StringBuilder();  // create a string builder object

        // create an object to read incoming streams
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();  // get incoming streams by lines

        while (line != null) {
            stringBuilder.append(line);  // concatenate strings using the string builder
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
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
    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}