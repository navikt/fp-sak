package no.nav.foreldrepenger.dokumentbestiller.kafka;

public class DokumentbestillerKafkaTaskProperties {

    public static final String TASKTYPE = "dokumentbestiller.kafka.bestilldokument";

    public static final String BEHANDLING_ID = "behandlingId";
    public static final String DOKUMENT_MAL_TYPE = "dokumentMalType";
    public static final String REVURDERING_VARSLING_ÅRSAK = "revurderingVarslingAarsak";
    public static final String HISTORIKK_AKTØR = "historikkAktoer";
    public static final String BESTILLING_UUID = "bestillingUuid";
    public static final String BEHANDLENDE_ENHET_NAVN = "behandlendeEnhetNavn";


    private DokumentbestillerKafkaTaskProperties() {
        // Skal ikke konstrueres
    }
}
