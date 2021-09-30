package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("oppgavebehandling.opprettOppgaveSakSendtTilbake")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveForBehandlingSendtTilbakeTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveForBehandlingSendtTilbakeTask.class);
    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveForBehandlingSendtTilbakeTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveForBehandlingSendtTilbakeTask(OppgaveTjeneste oppgaveTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var beskrivelse = "Sak har blitt sendt tilbake fra beslutter";
        var oppgaveId = oppgaveTjeneste.opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(behandlingId,
            beskrivelse, true, 0);
        if (oppgaveId != null) {
            LOG.info("Oppgave opprettet i GSAK for å behandle sak sendt tilbake. Oppgavenummer: {}", oppgaveId); //NOSONAR
        }
    }
}
