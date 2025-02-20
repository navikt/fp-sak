package no.nav.foreldrepenger.domene.migrering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "beregningsgrunnlag.migrer.enkeltsak", prioritet = 4)
public class MigrerBeregningSakTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MigrerBeregningSakTask.class);

    private BeregningMigreringTjeneste beregningMigreringTjeneste;

    MigrerBeregningSakTask() {
        // CDI
    }

    @Inject
    public MigrerBeregningSakTask(BeregningMigreringTjeneste beregningMigreringTjeneste) {
        this.beregningMigreringTjeneste = beregningMigreringTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Starter task migrer beregningsgrunnlag enkeltsak.");
        beregningMigreringTjeneste.migrerSak(new Saksnummer(prosessTaskData.getSaksnummer()));
        LOG.info("Avslutter task migrer beregningsgrunnlag enkeltsak.");
    }
}
