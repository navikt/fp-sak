package no.nav.foreldrepenger.domene.person.pdl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.GeografiskTilknytningResponseProjection;
import no.nav.pdl.HentGeografiskTilknytningQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;

@ApplicationScoped
public class TilknytningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilknytningTjeneste.class);

    private PdlKlientLogCause pdlKlient;

    TilknytningTjeneste() {
        // CDI
    }

    @Inject
    public TilknytningTjeneste(PdlKlientLogCause pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public String hentGeografiskTilknytning(AktørId aktørId) {

        var queryGT = new HentGeografiskTilknytningQueryRequest();
        queryGT.setIdent(aktørId.getId());
        var projectionGT = new GeografiskTilknytningResponseProjection()
                .gtType().gtBydel().gtKommune().gtLand();

        var geografiskTilknytning = pdlKlient.hentGT(queryGT, projectionGT);

        if (geografiskTilknytning == null || geografiskTilknytning.getGtType() == null)
            return null;
        // Alle Utland + Udefinert til samme enhet.
        return switch (geografiskTilknytning.getGtType()) {
            case BYDEL -> geografiskTilknytning.getGtBydel();
            case KOMMUNE -> geografiskTilknytning.getGtKommune();
            case UTLAND, UDEFINERT -> null;
        };
    }

    public Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
                .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection);

        var kode = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
            .findFirst().orElse(null);
        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
            return Diskresjonskode.KODE6;
        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7 : Diskresjonskode.UDEFINERT;
    }

}
