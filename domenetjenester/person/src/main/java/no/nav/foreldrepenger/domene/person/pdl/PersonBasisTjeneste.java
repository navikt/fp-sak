package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.ForenkletPersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;

@ApplicationScoped
public class PersonBasisTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersonBasisTjeneste.class);

    private PdlKlient pdlKlient;

    PersonBasisTjeneste() {
        // CDI
    }

    @Inject
    public PersonBasisTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public void hentBasisPersoninfo(AktørId aktørId, PersonIdent personIdent, PersoninfoBasis fraTPS) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                    .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
                    .foedsel(new FoedselResponseProjection().foedselsdato())
                    .doedsfall(new DoedsfallResponseProjection().doedsdato())
                    .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus())
                    .kjoenn(new KjoennResponseProjection().kjoenn())
                    .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
            var person = pdlKlient.hentPerson(query, projection, Tema.FOR);
            var fødselsdato = person.getFoedsel().stream()
                .map(Foedsel::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var dødssdato = person.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var fraPDL = new PersoninfoBasis.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
                .medNavn(person.getNavn().stream().map(PersonBasisTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse(null))
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødssdato)
                .medDiskresjonsKode(getDiskresjonskode(person))
                .medNavBrukerKjønn(mapKjønn(person))
                .medPersonstatusType(PersonstatusType.UDEFINERT)
                .build();
            var pdlStatus = person.getFolkeregisterpersonstatus().stream()
                .map(Folkeregisterpersonstatus::getForenkletStatus)
                .map(ForenkletPersonstatusType::fraKode)
                .findFirst().orElse(ForenkletPersonstatusType.UDEFINERT);
            var tpsStatus = ForenkletPersonstatusType.fraPersonstatusType(fraTPS.getPersonstatus());

            if (Objects.equals(fraPDL, mapFraTPS(fraTPS)) && Objects.equals(pdlStatus, tpsStatus)) {
                LOG.info("FPSAK PDL BASIS: like svar");
            } else {
                LOG.info("FPSAK PDL BASIS: avvik {}", finnAvvik(fraTPS, fraPDL, pdlStatus));
            }
        } catch (Exception e) {
            LOG.info("FPSAK PDL BASIS error", e);
        }
    }

    private String getDiskresjonskode(Person person) {
        var kode = person.getAdressebeskyttelse().stream()
                .map(Adressebeskyttelse::getGradering)
                .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
                .findFirst().orElse(null);
        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
            return Diskresjonskode.KODE6.getKode();
        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7.getKode() : null;
    }

    private static String mapNavn(Navn navn) {
        if (navn.getForkortetNavn() != null)
            return navn.getForkortetNavn();
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

    private static NavBrukerKjønn mapKjønn(Person person) {
        var kode = person.getKjoenn().stream()
            .map(Kjoenn::getKjoenn)
            .filter(Objects::nonNull)
            .findFirst().orElse(KjoennType.UKJENT);
        if (KjoennType.MANN.equals(kode))
            return NavBrukerKjønn.MANN;
        return KjoennType.KVINNE.equals(kode) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.UDEFINERT;
    }

    private static PersoninfoBasis mapFraTPS(PersoninfoBasis tps) {
        var diskKode = Optional.ofNullable(tps.getDiskresjonskode())
            .map(Diskresjonskode::finnForKodeverkEiersKode)
            .filter(k -> Diskresjonskode.KODE6.equals(k) || Diskresjonskode.KODE7.equals(k))
            .map(Diskresjonskode::getKode).orElse(null);
        return new PersoninfoBasis.Builder()
            .medAktørId(tps.getAktørId())
            .medPersonIdent(tps.getPersonIdent())
            .medNavn(tps.getNavn())
            .medFødselsdato(tps.getFødselsdato())
            .medDødsdato(tps.getDødsdato())
            .medNavBrukerKjønn(tps.getKjønn())
            .medDiskresjonsKode(diskKode)
            .medPersonstatusType(PersonstatusType.UDEFINERT)
            .build();
    }

    private String finnAvvik(PersoninfoBasis tps, PersoninfoBasis pdl, ForenkletPersonstatusType pdlStatus) {
        String navn = Objects.equals(tps.getNavn(), pdl.getNavn()) ? "" : " navn ";
        String kjonn = Objects.equals(tps.getKjønn(), pdl.getKjønn()) ? "" : " kjønn ";
        String fdato = Objects.equals(tps.getFødselsdato(), pdl.getFødselsdato()) ? "" : " fødsel ";
        String ddato = Objects.equals(tps.getDødsdato(), pdl.getDødsdato()) ? "" : " død ";
        String disk = Objects.equals(tps.getDiskresjonskode(), pdl.getDiskresjonskode()) ? "" : " disk ";
        String status = Objects.equals(ForenkletPersonstatusType.fraPersonstatusType(tps.getPersonstatus()), pdlStatus) ? "" : " status tps " + tps.getPersonstatus().getKode() + " PDL " + pdlStatus.getKode();
        return "Avvik" + navn + kjonn + fdato + ddato + disk + status;
    }

}
