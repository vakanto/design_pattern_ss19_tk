package ha07.Warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.RequestBodyEntity;
import org.fulib.yaml.EventSource;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.*;

public class ShopProxy {

    private EventSource eventSource;
    private ScheduledExecutorService executorService;

    public ShopProxy(){
        this.eventSource=new EventSource();
        executorService = Executors.newSingleThreadScheduledExecutor();

    }

    public void addProductToShop(LinkedHashMap<String,String> event) throws IOException, UnirestException {

        JSONObject eventJson = new JSONObject(event);
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode eventJsonNode =  new JsonNode(eventJson.toString());

        String yaml = EventSource.encodeYaml(event);

        sendRequest(yaml);
    }

    public void sendRequest(String yaml) throws UnirestException {

        try {
            /**RequestBodyEntity response = Unirest.post("http://127.0.0.1:5001/postEvent")
                    .header("accept", "application/json")
                    .body(eventJson);**/


            /**.asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> response) {
                    int responseCode=response.getCode();
                    JsonNode body = response.getBody();
                }

                @Override
                public void failed(UnirestException e) {
                    System.out.println("The request failed");
                }

                @Override
                public void cancelled() {
                    System.out.println("Request was cancelled.");
                }
            });**/

            URL url = new URL("http://127.0.0.1:5001/postEvent");
            URLConnection connection = url.openConnection();
            HttpURLConnection http =(HttpURLConnection)connection;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            byte[] output = yaml.getBytes(StandardCharsets.UTF_8);
            int length = output.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type","application/yaml; charset=UTF-8");
            http.connect();

            try(OutputStream os = http.getOutputStream()){
                os.write(output);
            }

            InputStream inputStream = http.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = bufferedReader.readLine();

            bufferedReader.close();


        } catch (Exception e) {
            executorService.schedule(()-> {
                try {
                    sendRequest(yaml);
                } catch (UnirestException ex) {
                    ex.printStackTrace();
                }
            }, 10, TimeUnit.SECONDS);
        }
    }
}
