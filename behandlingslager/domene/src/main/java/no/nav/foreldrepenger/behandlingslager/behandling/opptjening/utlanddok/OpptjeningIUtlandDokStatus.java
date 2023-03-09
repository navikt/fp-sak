package no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

public enum OpptjeningIUtlandDokStatus {

    DOKUMENTASJON_VIL_BLI_INNHENTET,
    DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET;

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<OpptjeningIUtlandDokStatus, String> {

        @Override
        public String convertToDatabaseColumn(OpptjeningIUtlandDokStatus opptjeningIUtlandDokStatus) {
            return opptjeningIUtlandDokStatus == null ? null : opptjeningIUtlandDokStatus.name();
        }

        @Override
        public OpptjeningIUtlandDokStatus convertToEntityAttribute(String s) {
            return s == null ? null : OpptjeningIUtlandDokStatus.valueOf(s);
        }
    }
}
