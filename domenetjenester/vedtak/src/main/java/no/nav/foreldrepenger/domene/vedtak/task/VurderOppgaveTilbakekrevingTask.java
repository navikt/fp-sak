package no.nav.foreldrepenger.domene.vedtak.task;

import static no.nav.foreldrepenger.domene.vedtak.task.VurderOppgaveTilbakekrevingTask.TASKTYPE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveTilbakekrevingTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "oppgavebehandling.vurderOppgaveTilbakekreving";
    private static final Logger log = LoggerFactory.getLogger(VurderOppgaveTilbakekrevingTask.class);
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepository behandlingRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;

    VurderOppgaveTilbakekrevingTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOppgaveTilbakekrevingTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider,
                                           TilbakekrevingRepository tilbakekrevingRepository) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (skalOppretteOppgaveTilbakekreving(behandling)) {
            FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
            String ytelse = finnBeskrivelseForYtelsetype(fagsakYtelseType);
            String beskrivelse = "Feilutbetaling " + ytelse + ".";

            String oppgaveId = oppgaveTjeneste.opprettOppgaveFeilutbetaling(behandlingId, beskrivelse);
            log.info("Opprettet oppgave i GSAK for tilbakebetaling. BehandlingId: {}. OppgaveId: {}.", behandlingId, oppgaveId);
        }
    }

    private String finnBeskrivelseForYtelsetype(FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType.gjelderEngangsstønad()) {
            return "engangsstønad";
        }
        if (fagsakYtelseType.gjelderForeldrepenger()) {
            return "foreldrepenger";
        }
        if (fagsakYtelseType.gjelderSvangerskapspenger()) {
            return "svangerskapspenger";
        }
        throw TaskFeilmeldinger.FACTORY.støtterIkkeFagsakYtelseType(fagsakYtelseType).toException();
    }

    private boolean skalOppretteOppgaveTilbakekreving(Behandling behandling) {
        Optional<TilbakekrevingValg> funnetTilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        return funnetTilbakekrevingValg.map(tilbakekrevingValg ->
            TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD.equals(tilbakekrevingValg.getVidereBehandling()))
            .orElse(false);
    }

}
