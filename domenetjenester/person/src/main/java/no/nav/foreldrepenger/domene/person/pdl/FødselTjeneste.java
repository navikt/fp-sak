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
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;

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
            .map(r -> fraForelderBarnRelasjon(ytelseType, r))
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
        var foreldersRolle = utledRolle(forelderBarnRelasjon.getMinRolleForPerson());
        return foreldersRolle.equals(rolleType);
    }

    private FødtBarnInfo fraForelderBarnRelasjon(FagsakYtelseType ytelseType, ForelderBarnRelasjon forelderBarnRelasjon) {
        var barnIdent = forelderBarnRelasjon.getRelatertPersonsIdent();
        if (barnIdent == null) {
            return null;
        }
        var request = new HentPersonQueryRequest();
        request.setIdent(barnIdent);
        var projection = new PersonResponseProjection()
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status());
        var barn = pdlKlient.hentPerson(ytelseType, request, projection);

        var fødselsdato = PersonMappers.mapFødselsdato(barn);
        var dødssdato = LokalPersonMapper.mapDødsdato(barn);
        var pdlStatusOpphørt = barn.getFolkeregisterpersonstatus().stream()
            .map(Folkeregisterpersonstatus::getStatus)
            .map(PersonstatusType::fraFregPersonstatus)
            .anyMatch(PersonstatusType.UTPE::equals);

        // Opphørte personer kan mangle fødselsdato mm. Håndtere dette + gi feil hvis fødselsdato mangler i andre tilfelle
        if (pdlStatusOpphørt && fødselsdato.isEmpty()) {
            return null;
        }

        return new FødtBarnInfo.Builder()
                .medIdent(new PersonIdent(barnIdent))
                .medFødselsdato(fødselsdato.orElseThrow())
                .medDødsdato(dødssdato)
                .medForelderRolle(utledRolle(forelderBarnRelasjon.getMinRolleForPerson()))
                .build();
    }

    private static RelasjonsRolleType utledRolle(ForelderBarnRelasjonRolle rolle) {
        return switch (rolle) {
            case MOR -> RelasjonsRolleType.MORA;
            case FAR -> RelasjonsRolleType.FARA;
            case MEDMOR -> RelasjonsRolleType.MEDMOR;
            case BARN -> RelasjonsRolleType.UDEFINERT;
        };
    }


}
