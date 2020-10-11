package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.DoedfoedtBarnResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Familierelasjon;
import no.nav.pdl.FamilierelasjonResponseProjection;
import no.nav.pdl.Familierelasjonsrolle;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;

@ApplicationScoped
public class FødselTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FødselTjeneste.class);
    private static final PersonIdent FDAT_GENERISK = new PersonIdent("01012000001");

    private PdlKlient pdlKlient;

    FødselTjeneste() {
        // CDI
    }

    @Inject
    public FødselTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public void hentFødteBarnInfoFor(AktørId bruker, List<FødtBarnInfo> fraTPS, List<LocalDateInterval> intervaller) {
        try {
            var request = new HentPersonQueryRequest();
            request.setIdent(bruker.getId());
            var projection = new PersonResponseProjection()
                .doedfoedtBarn(new DoedfoedtBarnResponseProjection().dato())
                .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());
            var person = pdlKlient.hentPerson(request, projection, Tema.FOR);
            List <FødtBarnInfo> fraPDL = new ArrayList<>();
            person.getDoedfoedtBarn().stream()
                .filter(df -> df.getDato() != null)
                .map(FødselTjeneste::fraDødfødsel)
                .forEach(fraPDL::add);
            person.getFamilierelasjoner().stream()
                .filter(b -> Familierelasjonsrolle.BARN.equals(b.getRelatertPersonsRolle()))
                .map(Familierelasjon::getRelatertPersonsIdent)
                .map(this::fraIdent)
                .forEach(fraPDL::add);
            sammenlignLoggFødsler(fraTPS, fraPDL);
        } catch (Exception e) {
            LOG.info("FPSAK PDL FØDSEL error", e);
        }
    }

    public List<PersonIdent> hentForeldreTil(PersonIdent barn, List<PersonIdent> fraTPS) {
        if(barn.erFdatNummer()) {
            return Collections.emptyList();
        }
        try {
            var request = new HentPersonQueryRequest();
            request.setIdent(barn.getIdent());
            var projection = new PersonResponseProjection()
                .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());
            var person = pdlKlient.hentPerson(request, projection, Tema.FOR);
            var foreldre = person.getFamilierelasjoner().stream()
                .filter(f -> !Familierelasjonsrolle.BARN.equals(f.getRelatertPersonsRolle()))
                .map(Familierelasjon::getRelatertPersonsIdent)
                .map(PersonIdent::fra)
                .collect(Collectors.toList());
            sammenlignLoggForeldre(fraTPS, foreldre);
            return foreldre;
        } catch (Exception e) {
            LOG.info("FPSAK PDL FORELDRE error", e);
        }
        return Collections.emptyList();
    }

    private static FødtBarnInfo fraDødfødsel(DoedfoedtBarn barn) {
        var dato = LocalDate.parse(barn.getDato(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new FødtBarnInfo.Builder()
            .medIdent(FDAT_GENERISK)
            .medFødselsdato(dato)
            .medDødsdato(dato)
            .build();
    }

    private FødtBarnInfo fraIdent(String barnIdent) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barnIdent);
        var projection = new PersonResponseProjection()
            .foedsel(new FoedselResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato());
        var barn = pdlKlient.hentPerson(request, projection, Tema.FOR);
        var fødselsdato = barn.getFoedsel().stream()
            .map(Foedsel::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = barn.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        return new FødtBarnInfo.Builder()
            .medIdent(new PersonIdent(barnIdent))
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødssdato)
            .build();
    }

    private void sammenlignLoggFødsler(List<FødtBarnInfo> fraTPS, List<FødtBarnInfo> fraPDL) {
        boolean like = fraPDL.size() == fraTPS.size() && fraPDL.containsAll(fraTPS) && fraTPS.containsAll(fraPDL);
        if (like) {
            LOG.info("FPSAK PDL FØDSEL: like svar");
        } else {
            LOG.info("FPSAK PDL FØDSEL: ulike svar TPS {} og PDL {}", fraTPS, fraPDL);
        }
    }

    private void sammenlignLoggForeldre(List<PersonIdent> fraTPS, List<PersonIdent> fraPDL) {
        boolean like = fraPDL.size() == fraTPS.size() && fraPDL.containsAll(fraTPS) && fraTPS.containsAll(fraPDL);
        if (like) {
            LOG.info("FPSAK PDL FORELDRE: like svar");
        } else {
            LOG.info("FPSAK PDL FORELDRE: ulike svar TPS {} og PDL {}", fraTPS, fraPDL);
        }
    }
}
