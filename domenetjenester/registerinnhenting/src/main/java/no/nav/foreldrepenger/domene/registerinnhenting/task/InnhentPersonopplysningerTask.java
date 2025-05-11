package no.nav.foreldrepenger.domene.registerinnhenting.task;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("innhentsaksopplysninger.personopplysninger")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentPersonopplysningerTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(InnhentPersonopplysningerTask.class);
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private RegisterdataInnhenter registerdataInnhenter;
    private BehandlingEventPubliserer behandlingEventPubliserer;

    InnhentPersonopplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentPersonopplysningerTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                         RegisterdataInnhenter registerdataInnhenter,
                                         BehandlingEventPubliserer behandlingEventPubliserer) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.registerdataInnhenter = registerdataInnhenter;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var personerFør = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(behandling.getSaksnummer());
        LOG.info("Innhenter personopplysninger for behandling: {}", behandling.getId());
        registerdataInnhenter.innhentPersonopplysninger(behandling);
        notifiserEndringPersoner(behandling, personerFør);
    }

    private void notifiserEndringPersoner(Behandling behandling, Set<AktørId> personerFør) {
        var personerEtter = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(behandling.getSaksnummer());
        if (personerFør.size() != personerEtter.size() || !personerFør.containsAll(personerEtter)) {
            LOG.info("Persongalleri er endret for sak: {}", behandling.getSaksnummer().getVerdi());
            behandlingEventPubliserer.publiserBehandlingEvent(new SakensPersonerEndretEvent(behandling, "Registerdata"));
        }
    }
}
