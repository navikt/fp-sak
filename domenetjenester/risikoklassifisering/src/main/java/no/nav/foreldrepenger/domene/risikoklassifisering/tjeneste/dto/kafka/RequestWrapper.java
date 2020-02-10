package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka;

public class RequestWrapper {

    private String callId;

    private Object request;

    public RequestWrapper(String callId, Object request) {
        this.callId = callId;
        this.request = request;
    }

    public String getCallId() {
        return callId;
    }

    public Object getRequest() {
        return request;
    }
}
