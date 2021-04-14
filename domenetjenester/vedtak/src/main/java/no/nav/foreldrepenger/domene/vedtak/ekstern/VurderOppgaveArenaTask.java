package no.nav.foreldrepenger.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(VurderOppgaveArenaTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveArenaTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderOppgaveArenaTask.class);

    public static final String TASKTYPE = "iverksetteVedtak.oppgaveArena";

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
        LOG.info("VurderOppgaveArenaTask: Vurderer for behandling: {}", behandlingId); //$NON-NLS-1$
    }
}
