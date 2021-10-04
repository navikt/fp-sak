package no.nav.foreldrepenger.dokumentbestiller.kafka;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.dokumentbestilling.kodeverk.FagsakYtelseType;
import no.nav.vedtak.felles.dokumentbestilling.v1.DokumentbestillingV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("dokumentbestiller.kafka.bestilldokument")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DokumentBestillerKafkaTask implements ProsessTaskHandler {

    public static final String BEHANDLING_ID = "behandlingId";
    public static final String DOKUMENT_MAL_TYPE = "dokumentMalType";
    public static final String REVURDERING_VARSLING_ÅRSAK = "revurderingVarslingAarsak";
    public static final String HISTORIKK_AKTØR = "historikkAktoer";
    public static final String BESTILLING_UUID = "bestillingUuid";
    public static final String BEHANDLENDE_ENHET_NAVN = "behandlendeEnhetNavn";

    private DokumentbestillingProducer dokumentbestillingProducer;
    private BehandlingRepository behandlingRepository;

    DokumentBestillerKafkaTask() {
        // for CDI proxy
    }

    @Inject
    public DokumentBestillerKafkaTask(DokumentbestillingProducer dokumentbestillingProducer,
                                      BehandlingRepository behandlingRepository) {
        this.dokumentbestillingProducer = dokumentbestillingProducer;
        this.behandlingRepository = behandlingRepository;
    }

    private static FagsakYtelseType mapYtelse(no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType fpsakYtelseKode) {
        if (no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.ENGANGSTØNAD.equals(fpsakYtelseKode)) {
            return FagsakYtelseType.ENGANGSTØNAD;
        }
        if (no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.FORELDREPENGER.equals(fpsakYtelseKode)) {
            return FagsakYtelseType.FORELDREPENGER;
        }
        if (no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.SVANGERSKAPSPENGER.equals(fpsakYtelseKode)) {
            return FagsakYtelseType.SVANGERSKAPSPENGER;
        }
        throw new TekniskException("FP-533280", "Klarte ikke utlede ytelsetype: %s. Kan ikke bestille dokument");
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        dokumentbestillingProducer.publiserDokumentbestillingJson(serialiser(mapDokumentbestilling(prosessTaskData)));
    }

    private DokumentbestillingV1 mapDokumentbestilling(ProsessTaskData prosessTaskData) {
        var behandling = behandlingRepository
            .hentBehandling(Long.valueOf(prosessTaskData.getPropertyValue(BEHANDLING_ID)));

        var dokumentbestillingDto = new DokumentbestillingV1();
        dokumentbestillingDto.setArsakskode(prosessTaskData.getPropertyValue(REVURDERING_VARSLING_ÅRSAK));
        dokumentbestillingDto.setBehandlingUuid(behandling.getUuid());
        dokumentbestillingDto
            .setDokumentbestillingUuid(UUID.fromString(prosessTaskData.getPropertyValue(BESTILLING_UUID)));
        dokumentbestillingDto.setDokumentMal(prosessTaskData.getPropertyValue(DOKUMENT_MAL_TYPE));
        dokumentbestillingDto.setFritekst(StandardJsonConfig.fromJson(prosessTaskData.getPayloadAsString(), String.class));
        dokumentbestillingDto.setHistorikkAktør(prosessTaskData.getPropertyValue(HISTORIKK_AKTØR));
        dokumentbestillingDto.setYtelseType(mapYtelse(behandling.getFagsakYtelseType()));
        dokumentbestillingDto.setBehandlendeEnhetNavn(prosessTaskData.getPropertyValue(BEHANDLENDE_ENHET_NAVN));
        return dokumentbestillingDto;
    }

    private String serialiser(DokumentbestillingV1 dto) {
        return StandardJsonConfig.toJson(dto);
    }
}
