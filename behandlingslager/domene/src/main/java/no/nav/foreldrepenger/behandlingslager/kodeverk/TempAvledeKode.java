package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(TempAvledeKode.class);

    @SuppressWarnings("rawtypes")
    public static String getVerdi(Class<? extends Enum> enumCls, Object node, String key) {
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
            String kodeverk = "uspesifisert";
            try {
                if (node instanceof JsonNode) {
                    kodeverk = ((JsonNode) node).get("kodeverk").asText();
                } else if (node instanceof TextNode) {
                    kodeverk = ((TextNode) node).asText();
                } else if (node instanceof Map) {
                    kodeverk = (String) ((Map) node).get("kodeverk");
                }
            } catch (Exception e) {
                LOG.info("KODEVERK-OBJEKT: tempavledekode kalt uten at det finnes kodeverk - kode {}", kode);
            }
            LOG.info("KODEVERK-OBJEKT: mottok kodeverdiobjekt som ikke var String - kode {} fra kodeverk {} ", kode, kodeverk);
        }
        return kode;
    }

}
