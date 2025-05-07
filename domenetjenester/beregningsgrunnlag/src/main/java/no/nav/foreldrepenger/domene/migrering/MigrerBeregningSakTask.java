package no.nav.foreldrepenger.domene.migrering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@ProsessTask(value = "beregningsgrunnlag.migrer.enkeltsak", prioritet = 4)
public class MigrerBeregningSakTask implements ProsessTaskHandler {
    public static final String BEHANDLING_UUID = "behandling_uuid";
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
        LOG.info("Starter task migrer beregningsgrunnlag enkeltsak.");
        var behandlingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(BEHANDLING_UUID)).map(UUID::fromString);
        behandlingUuid.ifPresentOrElse(uuid -> {
                LOG.info("Migrerer behandling med uuid {}", uuid);
                beregningMigreringTjeneste.migrerBehandling(behandlingRepository.hentBehandling(uuid));
            },
            () -> {
                var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
                LOG.info("Migrerer sak med saksnummer {}", saksnummer);
                beregningMigreringTjeneste.migrerSak(saksnummer);
            });
        LOG.info("Avslutter task migrer beregningsgrunnlag enkeltsak.");
    }
}
