package no.nav.foreldrepenger.domene.vedtak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksettFelles;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class OpprettProsessTaskIverksettImpl extends OpprettProsessTaskIverksettFelles {

    OpprettProsessTaskIverksettImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettImpl(ProsessTaskRepository prosessTaskRepository,
                                         OppgaveTjeneste oppgaveTjeneste) {
        super(prosessTaskRepository, oppgaveTjeneste);
    }
}
