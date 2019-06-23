package ha07_ha08.Shop;

import com.mashape.unirest.http.exceptions.UnirestException;
import ha07_ha08.Shop.Model.Shop;
import ha07_ha08.Shop.Model.ShopOrder;
import ha07_ha08.Shop.Model.ShopProduct;
import org.fulib.yaml.EventFiler;
import org.fulib.yaml.EventSource;
import org.fulib.yaml.Yamler;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.SortedMap;

public class ShopBuilder {

    private EventSource eventSource;
    public Shop shop;
    private WarehouseProxy warehouseProxy;
    private EventFiler eventFiler;

    public ShopBuilder() throws IOException {
        eventSource=new EventSource();
        shop=new Shop()
            .setName("DerLaden");
        warehouseProxy=new WarehouseProxy(this);

        eventFiler = new EventFiler(eventSource)
                .setHistoryFileName("src/main/java/ha07_ha08/database/Shop.yml");

        String history = eventFiler.loadHistory();
        ArrayList<LinkedHashMap<String,String>> warehouseEventsSince=null;

        if(history!=null){
            ArrayList<LinkedHashMap<String,String>> shopEventList = new Yamler().decodeList(history);

            eventFiler.storeHistory();
            //File file = new File("src/main/java/ha07_ha08/database/Shop.yml");
            String whEvents = warehouseProxy.getWarehouseEvents(eventSource.getLastEventTime()+1);
            warehouseEventsSince = new Yamler().decodeList(whEvents);
            this.applyEvents(shopEventList,1);
            //for (LinkedHashMap<String,String> event:shopEventList) {
            //    eventSource.append(event);
            //}
            this.applyEvents(warehouseEventsSince,1);

        }
        eventFiler.startEventLogging();
    }

    public void orderProduct(LinkedHashMap<String, String> event, String product_name, String address, String orderID, int applyLocalFlag){
        LinkedHashMap<String, String> oldEvent = eventSource.getEvent(orderID);
        ShopOrder order = getFromOrders(orderID);

        if(order != null){
            //event allready exists
            return;
        }
        else{
            order =  new ShopOrder()
                    .setId(orderID);
            ShopProduct product = getFromProducts(product_name);
            double oldCount = product.getInStock();
            product.setInStock(oldCount - 1);
            order.withProducts(product);
            }

        if(event==null) {
            event = new LinkedHashMap<>();
            event.put("event_type", "order_product");
            event.put("event_key", orderID);
            event.put("product_name", product_name);
            event.put("address", address);
            event.put("orderID", orderID);
            eventSource.append(event);
        }
        shop.withOrders(order);
        try {
            if(applyLocalFlag==0){
                warehouseProxy.orderProduct(event);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    public String applyEvents(ArrayList<LinkedHashMap<String, String>> events, int applyLocalFlag) {
        for(LinkedHashMap<String,String>event : events){
        if("add_product_to_shop".equals(event.get("event_type"))){
            String numberOfItems = event.get("itemCount");
            double itemCount = Double.parseDouble(numberOfItems);
            addProductToShop(event.get("event_key"), event.get("product_name"), itemCount);
            }
        else if("order_product".equals(event.get("event_type"))){
            orderProduct(event, event.get("product_name"), event.get("address"), event.get("orderID"), applyLocalFlag);
        }
        else if("getEvents".equals(event.get("event_type"))){
            long lastKnownEventTimestamp = Long.parseLong(event.get("timestamp"));
            return sendEvents(lastKnownEventTimestamp);
            }
        }
        return null;
    }

    private String sendEvents(long lastKnownEventTimestamp) {
        SortedMap<Long, LinkedHashMap<String,String>> eventsSince = eventSource.pull(lastKnownEventTimestamp);
        return EventSource.encodeYaml(eventsSince);
    }

    public void addProductToShop(String eventKey, String product_name, double itemCount){
        LinkedHashMap<String, String> event = eventSource.getEvent(eventKey);

        if(event!=null){
            //event allready exists
            return;
        }

        ShopProduct shopProduct = getFromProducts(product_name);
        double inStock = shopProduct.getInStock();

        shopProduct.setInStock(itemCount+inStock);
        shop.withProducts(shopProduct);

        event = new LinkedHashMap<>();
        event.put("event_type","add_product_to_shop");
        event.put("event_key", eventKey);
        event.put("product_name", product_name);
        event.put("itemCount","" + itemCount);
        eventSource.append(event);
    }

    public ShopProduct getFromProducts(String productName) {
        String productId = productName.replaceAll("\\W","");
        for(ShopProduct product : shop.getProducts()){
            if(product.getName().equals(productName)){
                return product;
            }
        }
        ShopProduct shopProduct = new ShopProduct()
                .setName(productName)
                .setId(productId)
                .setShop(this.shop);
        return shopProduct;
    }

    public WarehouseProxy getWarehouseProxy() {
        return warehouseProxy;
    }

    private ShopOrder getFromOrders(String orderID) {
        for (ShopOrder order : shop.getOrders()) {
            if (order.getId().equals(orderID)) {
                return order;
            }
        }
        return null;
    }
}
