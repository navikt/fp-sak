package no.nav.foreldrepenger.dokumentbestiller.formidling;

import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapDokumentMal;
import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapRevurderignÅrsak;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.Saksnummer;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("formidling.bestillDokument")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class BestillDokumentTask implements ProsessTaskHandler {

    public static final String REVURDERING_ÅRSAK = "revurderingAarsak";
    public static final String BESTILLING_UUID = "bestillingUuid";
    private static final Logger LOG = LoggerFactory.getLogger(BestillDokumentTask.class);
    private Dokument brev;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private MellomlagringRepository mellomlagringRepository;

    BestillDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public BestillDokumentTask(Dokument brev,
                               BehandlingDokumentRepository behandlingDokumentRepository,
                               MellomlagringRepository mellomlagringRepository) {
        this.brev = brev;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.mellomlagringRepository = mellomlagringRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        brev.bestill(mapTilDto(prosessTaskData));
    }

    private DokumentBestillingDto mapTilDto(ProsessTaskData prosessTaskData) {
        var bestillingUuid = UUID.fromString(prosessTaskData.getPropertyValue(BESTILLING_UUID));
        var bestiltEntitet = behandlingDokumentRepository.hentHvisEksisterer(bestillingUuid)
            .orElseThrow(() -> new IllegalStateException("Fant ikke bestilt dokument for bestillingUuid: " + bestillingUuid));

        var fritekst = hentFritekst(prosessTaskData, bestiltEntitet);
        if (fritekst != null) {
            LOG.info("Fritekst er satt på dokumentbestilling");
        }

        var revurderingÅrsak = mapRevurderignÅrsak(
            Optional.ofNullable(prosessTaskData.getPropertyValue(REVURDERING_ÅRSAK)).map(RevurderingVarslingÅrsak::valueOf).orElse(null));
        return new DokumentBestillingDto(prosessTaskData.getBehandlingUuid(),
            new Saksnummer(prosessTaskData.getSaksnummer()),
            bestillingUuid,
            mapDokumentMal(bestiltEntitet.getDokumentMalType()),
            revurderingÅrsak,
            fritekst,
            mapDokumentMal(bestiltEntitet.getOpprinneligDokumentMal())
        );
    }

    private String hentFritekst(ProsessTaskData prosessTaskData, BehandlingDokumentBestiltEntitet bestiltEntitet) {
        if (DokumentMalType.FRITEKST_HTML.equals(bestiltEntitet.getDokumentMalType()) && bestiltEntitet.getOpprinneligDokumentMal() != null) {
            var mellomlagringType = MellomlagringType.fraDokumentMalType(bestiltEntitet.getOpprinneligDokumentMal());
            if (mellomlagringType != null) {
                return mellomlagringRepository.hentMellomlagring(prosessTaskData.getBehandlingIdAsLong(), mellomlagringType)
                    .map(MellomlagringEntitet::getInnhold)
                    .orElseThrow(() -> new IllegalStateException("Fant ikke mellomlagring for FRITEKST_HTML bestilling"));
            }
        }
        return prosessTaskData.getPayloadAsString();
    }
}
