package no.nav.foreldrepenger.behandlingslager.fagsak;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum EgenskapNøkkel {

    UTLAND_DOKUMENTASJON,
    FAGSAK_MARKERING;


    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<EgenskapNøkkel, String> {
        @Override
        public String convertToDatabaseColumn(EgenskapNøkkel attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public EgenskapNøkkel convertToEntityAttribute(String dbData) {
            return dbData != null ? EgenskapNøkkel.valueOf(dbData) : null;
        }
    }
}
