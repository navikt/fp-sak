package no.nav.foreldrepenger.mottak.hendelser.oversetter;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødfødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseOversetter;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødfødselHendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.DØDFØDSEL_HENDELSE)
public class DødfødselForretningshendelseOversetter implements ForretningshendelseOversetter<DødfødselForretningshendelse> {

    @Override
    public DødfødselForretningshendelse oversett(ForretningshendelseDto forretningshendelse) {
        DødfødselHendelse dødfødselHendelse = JsonMapper.fromJson(forretningshendelse.getPayloadJson(), DødfødselHendelse.class);
        List<AktørId> aktørIdListe = dødfødselHendelse.getAktørIdListe().stream().map(AktørId::new).collect(Collectors.toList());
        return new DødfødselForretningshendelse(aktørIdListe, dødfødselHendelse.getDødfødselsdato());
    }
}
