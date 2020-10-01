package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;


/**
 * for avledning av kode for enum som ikke er mappet direkte på navn der både ny (@JsonValue) og gammel (@JsonProperty kode + kodeverk) kan
 * bli sendt. Brukes til eksisterende kode er konvertert til @JsonValue på alle grensesnitt.
 *
 * <h3>Eksempel - {@link BehandlingType}</h3>
 * <b>Gammel</b>: {"kode":"BT-004","kodeverk":"BEHANDLING_TYPE"}
 * <p>
 * <b>Ny</b>: "BT-004"
 * <p>
 *
 * @deprecated endre grensesnitt til @JsonValue istdf @JsonProperty + @JsonCreator
 */
@Deprecated(since = "2020-09-17")
public class TempAvledeKode {

    @SuppressWarnings("rawtypes")
    public static String getVerdi(Class<? extends Enum> enumCls, Object node, String key) {
        // TODO logge hvilke enum so sendes inn på gammelt format og hvor hen
        String kode;
        if (node instanceof String) {
            kode = (String) node;
        } else {
            if (node instanceof JsonNode) {
                kode = ((JsonNode) node).get(key).asText();
            } else if (node instanceof TextNode) {
                kode = ((TextNode) node).asText();
            } else if (node instanceof Map) {
                kode = (String) ((Map) node).get(key);
            } else {
                throw new IllegalArgumentException("Støtter ikke node av type: " + node.getClass() + " for enum:" + enumCls.getName());
            }
        }
        return kode;
    }

}
