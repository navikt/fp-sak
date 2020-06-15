package no.nav.foreldrepenger.domene.person.tps;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;

@ApplicationScoped
public class MultiAktørTjeneste {

    private AktørConsumerMedCache aktørConsumer;

    public MultiAktørTjeneste() {
    }

    @Inject
    public MultiAktørTjeneste(AktørConsumerMedCache aktørConsumer) {
        this.aktørConsumer = aktørConsumer;
    }


    public Set<AktørId> hentAlleAktørIdFor(AktørId aktørId) {
        var resultat = new HashSet<AktørId>();
        resultat.add(aktørId);
        aktørConsumer.hentPersonIdentForAktørId(aktørId.getId())
            .flatMap(aktørConsumer::hentAktørIdForPersonIdent).map(AktørId::new)
            .ifPresent(resultat::add);
        return resultat;
    }

    public AktørId hentGjeldendeAktørIdFor(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId())
            .flatMap(aktørConsumer::hentAktørIdForPersonIdent).map(AktørId::new)
            .orElse(aktørId);
    }

    public Optional<AktørId> hentGjeldendeAktørIdHvisUlik(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId())
            .flatMap(aktørConsumer::hentAktørIdForPersonIdent).map(AktørId::new)
            .filter(a -> !a.equals(aktørId));
    }
}
