package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.DoedfoedtBarnResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
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

    public List<FødtBarnInfo> hentFødteBarnInfoFor(FagsakYtelseType ytelseType, RelasjonsRolleType rolleType,
                                                   AktørId bruker, List<LocalDateInterval> intervaller) {
        var request = new HentPersonQueryRequest();
        request.setIdent(bruker.getId());
        var projection = new PersonResponseProjection()
                .doedfoedtBarn(new DoedfoedtBarnResponseProjection().dato())
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle().minRolleForPerson());

        var person = pdlKlient.hentPerson(ytelseType, request, projection);

        List<FødtBarnInfo> alleBarn = new ArrayList<>();
        person.getDoedfoedtBarn().stream()
                .filter(df -> df.getDato() != null)
                .map(FødselTjeneste::fraDødfødsel)
                .forEach(alleBarn::add);
        if (!alleBarn.isEmpty())
            LOG.info("FPSAK PDL FØDSEL dødfødsel registrert");
        person.getForelderBarnRelasjon().stream()
            .filter(b -> ForelderBarnRelasjonRolle.BARN.equals(b.getRelatertPersonsRolle()))
            .filter(b -> relevantForelder(rolleType, b))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .filter(Objects::nonNull)
            .map(i -> fraIdent(ytelseType, i))
            .filter(Objects::nonNull)
            .forEach(alleBarn::add);

        return alleBarn.stream()
                .filter(fBI -> intervaller.stream().anyMatch(i -> i.encloses(fBI.fødselsdato())))
                .toList();
    }

    public List<PersonIdent> hentForeldreTil(FagsakYtelseType ytelseType, PersonIdent barn) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.getIdent());
        var projection = new PersonResponseProjection()
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());

        var person = pdlKlient.hentPerson(ytelseType, request, projection);

        return person.getForelderBarnRelasjon().stream()
                .filter(f -> !ForelderBarnRelasjonRolle.BARN.equals(f.getRelatertPersonsRolle()))
                .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
                .filter(Objects::nonNull)
                .map(PersonIdent::fra)
                .toList();
    }

    private static FødtBarnInfo fraDødfødsel(DoedfoedtBarn barn) {
        var dato = LocalDate.parse(barn.getDato(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new FødtBarnInfo.Builder()
                .medFødselsdato(dato)
                .medDødsdato(dato)
                .build();
    }

    private static boolean relevantForelder(RelasjonsRolleType rolleType, ForelderBarnRelasjon forelderBarnRelasjon) {
        if (rolleType == null || RelasjonsRolleType.UDEFINERT.equals(rolleType)) {
            return true;
        }
        return switch (forelderBarnRelasjon.getMinRolleForPerson()) {
            case MOR -> RelasjonsRolleType.MORA.equals(rolleType);
            case FAR -> RelasjonsRolleType.FARA.equals(rolleType);
            case MEDMOR -> RelasjonsRolleType.MEDMOR.equals(rolleType);
            case BARN -> false;
        };
    }

    private FødtBarnInfo fraIdent(FagsakYtelseType ytelseType, String barnIdent) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barnIdent);
        var projection = new PersonResponseProjection()
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status());
        var barn = pdlKlient.hentPerson(ytelseType, request, projection);

        var fødselsdato = barn.getFoedselsdato().stream()
                .map(Foedselsdato::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = barn.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var pdlStatus = barn.getFolkeregisterpersonstatus().stream()
            .map(Folkeregisterpersonstatus::getStatus)
            .findFirst().map(PersonstatusType::fraFregPersonstatus).orElse(PersonstatusType.UDEFINERT);

        // Opphørte personer kan mangle fødselsdato mm. Håndtere dette + gi feil hvis fødselsdato mangler i andre tilfelle
        if (PersonstatusType.UTPE.equals(pdlStatus) && fødselsdato == null) {
            return null;
        }

        return new FødtBarnInfo.Builder()
                .medIdent(new PersonIdent(barnIdent))
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødssdato)
                .build();
    }

}
