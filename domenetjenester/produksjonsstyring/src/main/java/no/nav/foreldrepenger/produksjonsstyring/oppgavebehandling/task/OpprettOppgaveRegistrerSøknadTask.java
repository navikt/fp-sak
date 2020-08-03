package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.REGISTRER_SØKNAD;
import static no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveRegistrerSøknadTask.TASKTYPE;

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
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveRegistrerSøknadTask extends GenerellProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveRegistrerSøknad";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveRegistrerSøknadTask.class);
    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveRegistrerSøknadTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveRegistrerSøknadTask(OppgaveTjeneste oppgaveTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        String oppgaveId = oppgaveTjeneste.opprettBasertPåBehandlingId(behandlingId, REGISTRER_SØKNAD);
        log.info("Oppgave opprettet i GSAK for å registrere søknad. Oppgavenummer: {}", oppgaveId); // NOSONAR
    }
}
