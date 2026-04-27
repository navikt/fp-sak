package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class DokumentBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private MellomlagringRepository mellomlagringRepository;

    public DokumentBestiller() {
        // CDI
    }

    @Inject
    public DokumentBestiller(BehandlingRepository behandlingRepository,
                             ProsessTaskTjeneste taskTjeneste,
                             DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                             MellomlagringRepository mellomlagringRepository) {
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.mellomlagringRepository = mellomlagringRepository;
    }

    public void bestillDokument(DokumentBestilling bestilling) {
        var behandling = behandlingRepository.hentBehandling(bestilling.behandlingUuid());
        var resolved = resolveMellomlagring(behandling, bestilling);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, resolved);
        opprettBestillBrevTask(behandling, resolved);
    }

    private DokumentBestilling resolveMellomlagring(Behandling behandling, DokumentBestilling bestilling) {
        var mellomlagringType = MellomlagringType.fraDokumentMalType(bestilling.dokumentMal());
        if (mellomlagringType == null) {
            return bestilling;
        }
        var mellomlagring = mellomlagringRepository.hentMellomlagring(behandling.getId(), mellomlagringType);
        if (mellomlagring.isEmpty()) {
            return bestilling;
        }

        return DokumentBestilling.builder()
            .medBehandlingUuid(bestilling.behandlingUuid())
            .medSaksnummer(bestilling.saksnummer())
            .medDokumentMal(DokumentMalType.FRITEKST_HTML)
            .medJournalførSom(bestilling.dokumentMal())
            .medRevurderingÅrsak(bestilling.revurderingÅrsak())
            .medBestillingUuid(bestilling.bestillingUuid())
            .build();
    }

    private void opprettBestillBrevTask(Behandling behandling, DokumentBestilling bestilling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BestillDokumentTask.class);

        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_UUID, String.valueOf(behandling.getUuid()));
        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_ID, String.valueOf(behandling.getId()));
        prosessTaskData.setProperty(BestillDokumentTask.BESTILLING_UUID, String.valueOf(bestilling.bestillingUuid()));

        // Optionals
        Optional.ofNullable(bestilling.revurderingÅrsak()).ifPresent(a -> prosessTaskData.setProperty(BestillDokumentTask.REVURDERING_ÅRSAK, a.name()));
        prosessTaskData.setPayload(bestilling.fritekst());

        // Brukes kun i logging
        prosessTaskData.setSaksnummer(behandling.getSaksnummer().getVerdi());
        taskTjeneste.lagre(prosessTaskData);
    }
}
