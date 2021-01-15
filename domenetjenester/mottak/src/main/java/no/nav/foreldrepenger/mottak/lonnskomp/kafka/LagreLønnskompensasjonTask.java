package no.nav.foreldrepenger.mottak.lonnskomp.kafka;

import javax.inject.Inject;

import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.lonnskomp.domene.LønnskompensasjonRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ProsessTask(LagreLønnskompensasjonTask.TASKTYPE)
public class LagreLønnskompensasjonTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "oppdater.yf.soknad.mottattdato";
    public static final String SAK = "sakId";

    private LønnskompensasjonRepository repository;
    private AktørTjeneste aktørTjeneste;


    public LagreLønnskompensasjonTask() {
    }

    @Inject
    public LagreLønnskompensasjonTask(LønnskompensasjonRepository repository, AktørTjeneste aktørTjeneste) {
        this.repository = repository;
        this.aktørTjeneste = aktørTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        String sak = data.getPropertyValue(SAK);

        repository.hentSak(sak).stream()
            .filter(v -> v.getAktørId() == null)
            .forEach(v -> aktørTjeneste.hentAktørIdForPersonIdent(new PersonIdent(v.getFnr()))
                .ifPresent(aktørId -> repository.oppdaterFødselsnummer(v.getFnr(), aktørId)));
    }
}
