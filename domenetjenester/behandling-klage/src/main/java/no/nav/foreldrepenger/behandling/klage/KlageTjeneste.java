package no.nav.foreldrepenger.behandling.klage;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class KlageTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private KlageRepository klageRepository;
    private KlageHistorikkTjeneste klageHistorikkTjeneste;

    KlageTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                         BehandlingRepository behandlingRepository,
                         KlageRepository klageRepository,
                         BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                         ProsessTaskRepository prosessTaskRepository,
                         KlageHistorikkTjeneste klageHistorikkTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.klageHistorikkTjeneste = klageHistorikkTjeneste;
    }

    public Optional<Behandling> opprettKlageBehandling(Fagsak fagsak) {
        Optional<Behandling> behandlingKlagenGjelder = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());

        if (behandlingKlagenGjelder.isEmpty()) {
            return Optional.empty();
        }
        BehandlingType behandlingTypeKlage = BehandlingType.KLAGE;
        OrganisasjonsEnhet enhetKlage = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);

        Behandling klageBehandling = behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingTypeKlage,
            (beh) -> {
                beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingTypeKlage.getBehandlingstidFristUker()));
                beh.setBehandlendeEnhet(enhetKlage);
            });

        klageRepository.leggTilKlageResultat(klageBehandling);
        opprettTaskForÅStarteBehandling(klageBehandling);
        klageHistorikkTjeneste.opprettHistorikkinnslag(klageBehandling);

        return Optional.of(klageBehandling);
    }

    private void opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
