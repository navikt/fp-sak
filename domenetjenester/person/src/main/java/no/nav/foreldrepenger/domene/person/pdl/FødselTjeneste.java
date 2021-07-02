package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;

@ApplicationScoped
public class FødselTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FødselTjeneste.class);

    private PdlKlientLogCause pdlKlient;

    FødselTjeneste() {
        // CDI
    }

    @Inject
    public FødselTjeneste(PdlKlientLogCause pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public List<FødtBarnInfo> hentFødteBarnInfoFor(AktørId bruker, List<LocalDateInterval> intervaller) {
        var request = new HentPersonQueryRequest();
        request.setIdent(bruker.getId());
        var projection = new PersonResponseProjection()
                .doedfoedtBarn(new DoedfoedtBarnResponseProjection().dato())
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());

        var person = pdlKlient.hentPerson(request, projection);

        List<FødtBarnInfo> alleBarn = new ArrayList<>();
        person.getDoedfoedtBarn().stream()
                .filter(df -> df.getDato() != null)
                .map(FødselTjeneste::fraDødfødsel)
                .forEach(alleBarn::add);
        if (!alleBarn.isEmpty())
            LOG.info("FPSAK PDL FØDSEL dødfødsel registrert");
        person.getForelderBarnRelasjon().stream()
            .filter(b -> ForelderBarnRelasjonRolle.BARN.equals(b.getRelatertPersonsRolle()))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .map(this::fraIdent)
            .forEach(alleBarn::add);

        return alleBarn.stream()
                .filter(fBI -> intervaller.stream().anyMatch(i -> i.encloses(fBI.fødselsdato())))
                .collect(Collectors.toList());
    }

    public List<PersonIdent> hentForeldreTil(PersonIdent barn) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.getIdent());
        var projection = new PersonResponseProjection()
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());

        var person = pdlKlient.hentPerson(request, projection);

        return person.getForelderBarnRelasjon().stream()
                .filter(f -> !ForelderBarnRelasjonRolle.BARN.equals(f.getRelatertPersonsRolle()))
                .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
                .map(PersonIdent::fra)
                .collect(Collectors.toList());
    }

    private static FødtBarnInfo fraDødfødsel(DoedfoedtBarn barn) {
        var dato = LocalDate.parse(barn.getDato(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new FødtBarnInfo(null, dato, dato);
    }

    private FødtBarnInfo fraIdent(String barnIdent) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barnIdent);
        var projection = new PersonResponseProjection()
                .foedsel(new FoedselResponseProjection().foedselsdato())
                .doedsfall(new DoedsfallResponseProjection().doedsdato());

        var barn = pdlKlient.hentPerson(request, projection);

        var fødselsdato = barn.getFoedsel().stream()
                .map(Foedsel::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = barn.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        return new FødtBarnInfo(new PersonIdent(barnIdent), fødselsdato, dødssdato);

    }

}
