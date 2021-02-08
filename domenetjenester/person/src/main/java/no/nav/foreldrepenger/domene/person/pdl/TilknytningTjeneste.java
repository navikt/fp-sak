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
import no.nav.pdl.HentGeografiskTilknytningQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.Pdl;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;

@ApplicationScoped
public class TilknytningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilknytningTjeneste.class);

    private Pdl pdlKlient;

    TilknytningTjeneste() {
        // CDI
    }

    @Inject
    public TilknytningTjeneste(@Jersey Pdl pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {

        var queryGT = new HentGeografiskTilknytningQueryRequest();
        queryGT.setIdent(aktørId.getId());
        var projectionGT = new GeografiskTilknytningResponseProjection()
                .gtType().gtBydel().gtKommune().gtLand();

        var geografiskTilknytning = pdlKlient.hentGT(queryGT, projectionGT);

        var diskresjon = hentDiskresjonskode(aktørId);
        var tilknytning = getTilknytning(geografiskTilknytning);
        return new GeografiskTilknytning(tilknytning, diskresjon);
    }

    public Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
                .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection);

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

    private String getTilknytning(no.nav.pdl.GeografiskTilknytning gt) {
        if (gt == null || gt.getGtType() == null)
            return null;
        var gtType = gt.getGtType();
        if (GtType.BYDEL.equals(gtType))
            return gt.getGtBydel();
        if (GtType.KOMMUNE.equals(gtType))
            return gt.getGtKommune();
        if (GtType.UTLAND.equals(gtType))
            return gt.getGtLand();
        return null;
    }

}
