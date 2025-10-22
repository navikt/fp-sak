package no.nav.foreldrepenger.web.server.abac;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class InvaliderSakPersonCacheObserver  {

    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public InvaliderSakPersonCacheObserver(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    InvaliderSakPersonCacheObserver() {
        // CDI
    }

    public void observerPersonerEndretEvent(@Observes SakensPersonerEndretEvent event) {
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var prosessTaskData = ProsessTaskData.forProsessTask(InvaliderSakPersonCacheTask.class);
        prosessTaskData.setProperty(InvaliderSakPersonCacheTask.INVALIDER_SAK, event.saksnummer().getVerdi());
        prosessTaskData.setGruppe("invalidtask-" + event.saksnummer().getVerdi() + "-" + System.currentTimeMillis());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }
}
