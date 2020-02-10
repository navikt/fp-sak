package no.nav.foreldrepenger.mottak.hendelser.kontrakt;

public class ForretningshendelseDto {
    private String forretningshendelseType;
    private String payloadJson;

    public ForretningshendelseDto(String forretningshendelseType, String payloadJson) {
        this.forretningshendelseType = forretningshendelseType;
        this.payloadJson = payloadJson;
    }

    public String getForretningshendelseType() {
        return forretningshendelseType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
