import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CrptApi {

    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final String CLIENT_TOKEN = "clientToken";
    private final String USER_NAME = "userName";

    private int requestLimit;
    private final TimeUnit timeUnit;
    private static int counter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        if (requestLimit >= 0) {
            this.requestLimit = requestLimit;
            counter = requestLimit;
        } else {
            throw new IllegalArgumentException("Передано отрицательное число");
        }
    }

    public void runRequest(Document document, String signature) {
        String docJson = getDocJson(document, signature).toString();
        httpRequest(docJson);
    }

    //К сожалению, я не понял, как и где нужно использовать подпись
    @SuppressWarnings("unchecked")
    private JSONObject getDocJson(Document document, String signature) {
        JSONObject doc = new JSONObject();
        if (isNull(document.getDescription())) {
            JSONObject inn = new JSONObject();
            inn.put("participantInn", document.getParticipantInn());
            doc.put("description", inn);
        }
        doc.put("doc_id", document.getDocId());
        doc.put("doc_status", document.getDocStatus());
        doc.put("doc_type", document.getDocType());
        if (isNull(document.getImportRequest())) {
            doc.put("importRequest", document.getImportRequest());
        }
        doc.put("owner_inn", document.getOwnerInn());
        doc.put("participant_inn", document.getParticipantInn());
        doc.put("producer_inn", document.getProducerInn());
        doc.put("production_date", document.getProducerInn());
        doc.put("production_type", document.getProductionType());
        Document.Products product = document.getProducts();
        if (product != null) {
            JSONArray productsList = new JSONArray();
            JSONObject products = new JSONObject();
            if (product.getCertificateDocument() != null) {
                products.put("certificate_document", product.getCertificateDocument());
            } else if (isNull(product.getCertificateDocumentDate())) {
                products.put("certificate_document_date", product.getCertificateDocumentDate());
            } else if (isNull(product.getCertificateDocumentNumber())) {
                products.put("certificate_document_number", product.getCertificateDocumentNumber());
            }
            products.put("owner_inn", document.getOwnerInn());
            products.put("producer_inn", document.getProducerInn());
            products.put("production_date", document.getProductionDate());
            if (!document.getProductionDate().equals(product.getProductionDate())) {
                products.put("production_date", product.getProductionDate());
            }
            products.put("tnved_code", product.tnvedCode);
            if (isNull(product.getUitCode())) {
                products.put("uit_code", product.getUitCode());
            } else if (isNull(product.getUituCode())) {
                products.put("uitu_code", product.getUituCode());
            } else {
                throw new IllegalArgumentException("Одно из полей uit_code/uitu_code " +
                        "является обязательным");
            }
            productsList.add(products);
            doc.put("products", productsList);
        }
        doc.put("reg_date", document.getRegDate());
        doc.put("reg_number", document.getRegNumber());
        return doc;
    }

    private void httpRequest(String json) {
        if (requestLimit != 0) {
            synchronized (this) {
                counter--;
            }
        }
        try {
            if (counter < 0) {
                Thread.sleep(getTime());
                counter = requestLimit;
            }
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(URL);

            StringEntity entity = new StringEntity(json);
            post.addHeader("content-type", "application/json");
            post.addHeader("clientToken", CLIENT_TOKEN);
            post.addHeader("userName", USER_NAME);
            post.setEntity(entity);
            httpClient.execute(post);
            httpClient.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isNull(String check) {
        return check != null;
    }

    public enum TimeUnit {
        SECOND, MINUTE, HOUR
    }

    private long getTime() {
        return switch (timeUnit) {
            case SECOND -> 1000;
            case MINUTE -> 1000 * 60;
            case HOUR -> 1000 * 60 * 60;
        };
    }

    /**
     * Класс описывает объект документа,
     * поле "Перечень продуктов" описано как отдельный класс.
     * Необязательные поля задаются через сеттеры
     */
    public static class Document {
        @Getter
        @Setter
        private String description;
        @Getter
        private final String participantInn;
        @Getter
        private final String docId;
        @Getter
        private final String docStatus;
        @Getter
        private final String docType;
        @Getter
        @Setter
        private String importRequest;
        @Getter
        private final String ownerInn;
        @Getter
        private final String producerInn;
        @Getter
        private final String productionDate;
        @Getter
        private final String productionType;
        @Getter
        private final String regDate;
        @Getter
        private final String regNumber;
        @Getter
        @Setter
        private Products products;

        public Document(String participantInn, String docId, String docStatus,
                        String docType, String ownerInn, String producerInn,
                        String productionDate, String productionType,
                        String regDate, String regNumber) {
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public class Products {
            @Getter
            @Setter
            private CertificateType certificateDocument;
            @Getter
            @Setter
            private String certificateDocumentDate;
            @Getter
            @Setter
            private String certificateDocumentNumber;
            @Getter
            @Setter
            private String productionDate;
            @Getter
            @Setter
            private String tnvedCode;
            @Getter
            @Setter
            private String uitCode;
            @Getter
            @Setter
            private String uituCode;

            public enum CertificateType {
                CONFORMITY_CERTIFICATE, CONFORMITY_DECLARATION
            }
        }
    }
}
