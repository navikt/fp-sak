package no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

public enum OpptjeningIUtlandDokStatus {

    DOKUMENTASJON_VIL_BLI_INNHENTET("DOKUMENTASJON_VIL_BLI_INNHENTET"),
    DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET("DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET");

    private final String value;

    private static final Map<String, OpptjeningIUtlandDokStatus> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.value, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.value);
            }
        }
    }

    OpptjeningIUtlandDokStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<OpptjeningIUtlandDokStatus, String> {

        @Override
        public String convertToDatabaseColumn(OpptjeningIUtlandDokStatus opptjeningIUtlandDokStatus) {
            return opptjeningIUtlandDokStatus == null ? null : opptjeningIUtlandDokStatus.getValue();
        }

        @Override
        public OpptjeningIUtlandDokStatus convertToEntityAttribute(String s) {
            return s == null ? null : KODER.get(s);
        }
    }
}
