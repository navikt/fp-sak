package no.nav.foreldrepenger.mottak.hendelser.oversetter;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.fødsel.FødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseOversetter;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.FødselHendelse;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.FØDSEL_HENDELSE)
public class FødselForretningshendelseOversetter implements ForretningshendelseOversetter<FødselForretningshendelse> {

    @Override
    public FødselForretningshendelse oversett(ForretningshendelseDto forretningshendelse) {
        FødselHendelse fødselHendelse = JsonMapper.fromJson(forretningshendelse.getPayloadJson(), FødselHendelse.class);
        List<AktørId> aktørIdListe = fødselHendelse.getAktørIdListe().stream().map(AktørId::new).collect(Collectors.toList());
        return new FødselForretningshendelse(aktørIdListe, fødselHendelse.getFødselsdato());
    }
}
