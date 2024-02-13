package no.nav.foreldrepenger.dokumentbestiller.formidling;

import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillBrevDtoMapper.mapDokumentMal;
import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillBrevDtoMapper.mapRevurderignÅrsak;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("dokumentbestiller.bestillbrev")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class BestillBrevTask implements ProsessTaskHandler {

    public static final String DOKUMENT_MAL = "dokumentMal";
    public static final String JOURNALFOER_SOM_DOKUMENT = "journalfoerSom";
    public static final String REVURDERING_ÅRSAK = "revurderingAarsak";
    public static final String BESTILLING_UUID = "bestillingUuid";
    private Brev brev;

    BestillBrevTask() {
        // for CDI proxy
    }

    @Inject
    public BestillBrevTask(Brev brev) {
        this.brev = brev;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        brev.bestill(mapTilDto(prosessTaskData));
    }

    private DokumentBestillingDto mapTilDto(ProsessTaskData prosessTaskData) {
        return new DokumentBestillingDto(prosessTaskData.getBehandlingUuid(),
            UUID.fromString(prosessTaskData.getPropertyValue(BESTILLING_UUID)),
            mapDokumentMal(DokumentMalType.valueOf(prosessTaskData.getPropertyValue(DOKUMENT_MAL))),
            mapRevurderignÅrsak(Optional.ofNullable(prosessTaskData.getPropertyValue(REVURDERING_ÅRSAK)).map(RevurderingVarslingÅrsak::valueOf).orElse(null)),
            prosessTaskData.getPayloadAsString(),
            mapDokumentMal(Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALFOER_SOM_DOKUMENT)).map(DokumentMalType::valueOf).orElse(null))
        );
    }
}
