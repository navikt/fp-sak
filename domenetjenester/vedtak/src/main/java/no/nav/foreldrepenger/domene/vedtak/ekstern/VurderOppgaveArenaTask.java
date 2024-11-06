package no.nav.foreldrepenger.domene.vedtak.ekstern;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.oppgaveArena", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveArenaTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderOppgaveArenaTask.class);

    private VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    VurderOppgaveArenaTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOppgaveArenaTask(VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  BeregningsresultatRepository beregningsresultatRepository) {
        super();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vurdereOmArenaYtelseSkalOpphøre = vurdereOmArenaYtelseSkalOpphøre;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erRevurdering()) {
            var forrigeBehandlingStartdato = getTidligsteFomDatoMedUtbetaling(behandling.getOriginalBehandlingId().orElseThrow());
            var aktuellBehandlingStartdato = getTidligsteFomDatoMedUtbetaling(behandlingId);
            // Skal bare vurdere oppgave dersom revurdering begynner utbetaling tidligere enn forrige behandling
            if (!aktuellBehandlingStartdato.isBefore(forrigeBehandlingStartdato)) {
                return;
            }
        }
        var aktørId = behandling.getAktørId();
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        vurdereOmArenaYtelseSkalOpphøre.opprettOppgaveHvisArenaytelseSkalOpphøre(behandlingId, aktørId, skjæringstidspunkt);
        LOG.info("VurderOppgaveArenaTask: Vurderer for behandling: {}", behandlingId);
    }

    private LocalDate getTidligsteFomDatoMedUtbetaling(Long behandlingId) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElseGet(List::of).stream()
            .filter(brp -> brp.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
    }
}
