package no.nav.foreldrepenger.web.server.abac;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/*
 * Kjøres i egen task for å sikre at tilgangskontroll henter committed data.
 */
@Dependent
@ProsessTask(value = "tilgangskontroll.invalidersak")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class InvaliderSakPersonCacheTask implements ProsessTaskHandler {

    static final String INVALIDER_SAK = "invaliderSak";

    private final InvaliderSakPersonCacheKlient klient;


    @Inject
    public InvaliderSakPersonCacheTask(InvaliderSakPersonCacheKlient klient) {
        this.klient = klient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = Optional.ofNullable(prosessTaskData.getPropertyValue(INVALIDER_SAK)).orElseThrow();
        klient.invaliderSakCache(saksnummer);
    }

}
