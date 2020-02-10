package no.nav.foreldrepenger.dokumentbestiller.kafka;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.dokumentbestilling.kodeverk.FagsakYtelseType;
import no.nav.vedtak.felles.dokumentbestilling.v1.DokumentbestillingV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(DokumentbestillerKafkaTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DokumentBestillerKafkaTask implements ProsessTaskHandler {

    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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
        if (fpsakYtelseKode.gjelderEngangsstønad()) {
            return FagsakYtelseType.ENGANGSTØNAD;
        } else if (fpsakYtelseKode.gjelderForeldrepenger()) {
            return FagsakYtelseType.FORELDREPENGER;
        } else if (fpsakYtelseKode.gjelderSvangerskapspenger()) {
            return FagsakYtelseType.SVANGERSKAPSPENGER;
        }
        throw DokumentbestillerKafkaFeil.FACTORY.fantIkkeYtelseType(fpsakYtelseKode.getKode()).toException();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        dokumentbestillingProducer.publiserDokumentbestillingJson(serialiser(mapDokumentbestilling(prosessTaskData)));
    }

    private DokumentbestillingV1 mapDokumentbestilling(ProsessTaskData prosessTaskData) {
        Behandling behandling = behandlingRepository
            .hentBehandling(Long.valueOf(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)));

        DokumentbestillingV1 dokumentbestillingDto = new DokumentbestillingV1();
        dokumentbestillingDto.setArsakskode(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.REVURDERING_VARSLING_ÅRSAK));
        dokumentbestillingDto.setBehandlingUuid(behandling.getUuid());
        dokumentbestillingDto
            .setDokumentbestillingUuid(UUID.fromString(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID)));
        dokumentbestillingDto.setDokumentMal(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE));
        dokumentbestillingDto.setFritekst(JsonMapper.fromJson(prosessTaskData.getPayloadAsString(), String.class));
        dokumentbestillingDto.setHistorikkAktør(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.HISTORIKK_AKTØR));
        dokumentbestillingDto.setYtelseType(mapYtelse(behandling.getFagsakYtelseType()));
        dokumentbestillingDto.setBehandlendeEnhetNavn(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLENDE_ENHET_NAVN));
        return dokumentbestillingDto;
    }

    private String serialiser(DokumentbestillingV1 dto) {
        try {
            return mapper.writeValueAsString(dto);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Klarte ikke å serialisere historikkinnslag for behandling=" + dto.getBehandlingUuid() + ", mal=" + dto.getDokumentMal(), e); // NOSONAR
        }
    }
}
