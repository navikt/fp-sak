package no.nav.foreldrepenger.mottak.hendelser.oversetter;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseOversetter;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødHendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.DØD_HENDELSE)
public class DødForretningshendelseOversetter implements ForretningshendelseOversetter<DødForretningshendelse> {

    @Override
    public DødForretningshendelse oversett(ForretningshendelseDto forretningshendelse) {
        DødHendelse dødHendelse = JsonMapper.fromJson(forretningshendelse.getPayloadJson(), DødHendelse.class);
        List<AktørId> aktørIdListe = dødHendelse.getAktørIdListe().stream().map(AktørId::new).collect(Collectors.toList());
        return new DødForretningshendelse(aktørIdListe, dødHendelse.getDødsdato());
    }
}
