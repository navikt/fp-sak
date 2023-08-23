package no.nav.foreldrepenger.domene.vedtak.ekstern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask("iverksetteVedtak.oppgaveArena")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveArenaTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderOppgaveArenaTask.class);

    private VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderOppgaveArenaTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOppgaveArenaTask(VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vurdereOmArenaYtelseSkalOpphøre = vurdereOmArenaYtelseSkalOpphøre;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var aktørId = new AktørId(prosessTaskData.getAktørId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        vurdereOmArenaYtelseSkalOpphøre.opprettOppgaveHvisArenaytelseSkalOpphøre(behandlingId, aktørId, skjæringstidspunkt);
        LOG.info("VurderOppgaveArenaTask: Vurderer for behandling: {}", behandlingId);
    }
}
