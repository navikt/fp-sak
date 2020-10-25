package no.nav.foreldrepenger.domene.person.pdl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.GeografiskTilknytningResponseProjection;
import no.nav.pdl.GtType;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;

@ApplicationScoped
public class TilknytningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilknytningTjeneste.class);

    private PdlKlient pdlKlient;

    TilknytningTjeneste() {
        // CDI
    }

    @Inject
    public TilknytningTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {

        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .geografiskTilknytning(new GeografiskTilknytningResponseProjection().gtType().gtBydel().gtKommune().gtLand())
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection, Tema.FOR);

        var diskresjon = getDiskresjonskode(person);
        var tilknytning = getTilknytning(person);
        return new GeografiskTilknytning(tilknytning, diskresjon);
    }

    public Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection, Tema.FOR);

        return getDiskresjonskode(person);
    }

    private Diskresjonskode getDiskresjonskode(Person person) {
        var kode = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
            .findFirst().orElse(null);
        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
            return Diskresjonskode.KODE6;
        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7 : Diskresjonskode.UDEFINERT;
    }

    private String getTilknytning(Person person) {
        if (person.getGeografiskTilknytning() == null || person.getGeografiskTilknytning().getGtType() == null)
            return null;
        var gtType = person.getGeografiskTilknytning().getGtType();
        if (GtType.BYDEL.equals(gtType))
            return person.getGeografiskTilknytning().getGtBydel();
        if (GtType.KOMMUNE.equals(gtType))
            return person.getGeografiskTilknytning().getGtKommune();
        if (GtType.UTLAND.equals(gtType))
            return person.getGeografiskTilknytning().getGtLand();
        return null;
    }

}
