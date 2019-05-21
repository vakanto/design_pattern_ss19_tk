package ha05.customer_client;

import javafx.application.Platform;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.util.LinkedList;

public class CommunicationProxy implements MqttCallback {

    private  String SENDING_CHANNEL = "toTransport";
    private  String RECEIVE_CHANNEL = "toCharlie";

    private MqttClient client;
    private LinkedList<JSONObject> messages;
    private Customer_Client customer_client;


    public CommunicationProxy(String receiveChannel, String sendingChannel, Customer_Client customer_client){
        try {
            this.customer_client=customer_client;
            client =  new MqttClient("tcp://127.0.0.1:2000", MqttClient.generateClientId());
            client.setCallback(this);
            client.connect();
            client.subscribe(RECEIVE_CHANNEL);
            System.out.println("Client is connected: " + client.isConnected());
        } catch (MqttException e) {
            e.printStackTrace();
        }
        messages=new LinkedList<>();
        //offer_screen=new Offer_Screen();

        if(receiveChannel != null){
            RECEIVE_CHANNEL=receiveChannel;
        }
        if(sendingChannel != null){
            SENDING_CHANNEL = sendingChannel;
        }

        System.out.println("Client Proxy sends on: " + SENDING_CHANNEL);
        System.out.println("Client Proxy receives on: " + RECEIVE_CHANNEL);
    }

    public boolean sendMessage(JSONObject object) {
        MqttMessage message = new MqttMessage();
        message.setPayload(object.toString().getBytes());
        try {
            if(client.isConnected()==false){
            client.connect();
            }
            client.publish(SENDING_CHANNEL, message);
            System.out.println("Proxy sends on " + SENDING_CHANNEL + message);
            //client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost.");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Message arrived at Proxy.");
        JSONObject jsonObject = new JSONObject(message.toString());
        messages.add(jsonObject);
        customer_client.messageArrived(jsonObject);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public JSONObject getLastMessage(){
        if(!messages.isEmpty()) {
            return messages.pop();
        }
        else{
            return null;
        }
    }
}