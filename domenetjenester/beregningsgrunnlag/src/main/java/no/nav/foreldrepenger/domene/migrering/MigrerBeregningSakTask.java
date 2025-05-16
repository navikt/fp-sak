package no.nav.foreldrepenger.domene.migrering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "beregningsgrunnlag.migrer.enkeltsak", prioritet = 4)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class MigrerBeregningSakTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MigrerBeregningSakTask.class);

    private BeregningMigreringTjeneste beregningMigreringTjeneste;
    private BehandlingRepository behandlingRepository;

    MigrerBeregningSakTask() {
        // CDI
    }

    @Inject
    public MigrerBeregningSakTask(BeregningMigreringTjeneste beregningMigreringTjeneste,
                                  BehandlingRepository behandlingRepository) {
        this.beregningMigreringTjeneste = beregningMigreringTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (prosessTaskData.getBehandlingUuid() != null) {
            LOG.info("Starter task migrer beregningsgrunnlag behandling.");
            var behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingUuid());
            beregningMigreringTjeneste.migrerBehandling(behandling);
            LOG.info("Avslutter task migrer beregningsgrunnlag behandling.");
        } else {
            LOG.info("Starter task migrer beregningsgrunnlag enkeltsak.");
            beregningMigreringTjeneste.migrerSak(new Saksnummer(prosessTaskData.getSaksnummer()));
            LOG.info("Avslutter task migrer beregningsgrunnlag enkeltsak.");
        }
    }
}
