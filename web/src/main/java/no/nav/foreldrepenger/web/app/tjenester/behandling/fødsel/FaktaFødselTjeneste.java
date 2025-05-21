package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;

@ApplicationScoped
public class FaktaFødselTjeneste {

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    FaktaFødselTjeneste() {
        // For CDI proxy
    }

    @Inject
    public FaktaFødselTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    public void overstyrFaktaOmFødsel(BehandlingReferanse ref, OverstyringFaktaOmFødselDto dto) {
        // TODO: Legg til håndtering av om verdien kan endres
        // TODO: Implementer overstyring av fakta om fødsel
        // TODO: Husk å overstyre antall barn også, ved å bruke dto.getAntallBarn()
        // TODO: Sjekk overstyring av antall barn hvis det er registrert noe i freg
        // TODO: Lagres antall barn som en faktisk verdi, eller lagrer vi bare barna og regner ut antallet?

        // TODO: Case: Født i utlandet, ikke registrert i FREG. Barn dør like etter fødsel. Dødsdato må overstyres

        // TODO: Finnes det noen caser hvor det er registrert i freg og man likevel skal få lov til å overstyre?
    }

    public FødselDto hentFaktaOmFødsel(Long behandlingId) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);
        var terminbekreftelse = familieHendelse.getSøknadVersjon().getTerminbekreftelse();

        return new FødselDto(new FødselDto.Søknad(getBarn(familieHendelse.getSøknadVersjon()),
            terminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null),
            terminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null)),
            new FødselDto.Register(familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList)),
            new FødselDto.Gjeldende(mapTermindato(familieHendelse), mapBarn(familieHendelse)));
    }

    private static FødselDto.Gjeldende.Termindato mapTermindato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElse(null);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
        var kilde = Objects.equals(overstyrtTermindato, søknadTermindato) ? Kilde.SØKNAD : Kilde.SAKSBEHANDLER;
        return new FødselDto.Gjeldende.Termindato(kilde, kilde == Kilde.SØKNAD ? søknadTermindato : overstyrtTermindato, true);
    }

    private FødselDto.Gjeldende.Barn mapBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var kilde = getKildeForBarn(familieHendelse);
        var barn = switch (kilde) {
            case SAKSBEHANDLER -> getBarn(familieHendelse.getOverstyrtVersjon().orElse(null));
            case FOLKEREGISTER -> getBarn(familieHendelse.getBekreftetVersjon().orElse(null));
            case SØKNAD -> getBarn(familieHendelse.getSøknadVersjon());
        };
        return new FødselDto.Gjeldende.Barn(kilde, barn, kilde != Kilde.FOLKEREGISTER);
    }

    private List<AvklartBarnDto> getBarn(FamilieHendelseEntitet familieHendelse) {
        return familieHendelse == null ? Collections.emptyList() : familieHendelse.getBarna()
            .stream()
            .map(barnEntitet -> new AvklartBarnDto(barnEntitet.getFødselsdato(), barnEntitet.getDødsdato().orElse(null)))
            .toList();
    }

    private static Kilde getKildeForBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        // TODO: Sjekk populering
        var overstyrteBarn = familieHendelse.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        var bekreftedeBarn = familieHendelse.getBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();

        /**
         * Hvis det finnes overstyrte barn som er flere enn bekreftede, så er det alltid overstyrt som gjelder
         * Hvis det finnes bekreftede barn så er det bekreftet som gjelder, så lenge ikke punkt ovenfor treffer
         * Ellers er det bare søknad som gjelder? Eller?
         */


        if (overstyrteBarn.size() > bekreftedeBarn.size()) {
            return Kilde.SAKSBEHANDLER;
        }

        if (!bekreftedeBarn.isEmpty()) {
            return Kilde.FOLKEREGISTER;
        }

        return Kilde.SØKNAD;

        // TODO: Tenk litt på om bekreftet og søknad alltid kan være like, og overstyrt er forskjellig
//        if (!harLikeBarn(overstyrteBarn, søknadBarn)) {
//            return Kilde.SBH;
//        }
//
//        return harLikeBarn(bekreftedeBarn, søknadBarn) ? Kilde.SØKNAD : Kilde.FREG;
    }

    private static boolean harLikeBarn(List<UidentifisertBarn> barn1, List<UidentifisertBarn> barn2) {
        // TODO: Sjekk om de er populert, hvis ikke må man ha null-sjekker
        // TODO: Tror vi må ha null-sjekker, her og alle andre steder
        return mapBarnPåFødselsOgDødsdato(barn1).equals(mapBarnPåFødselsOgDødsdato(barn2));
    }

    private static Map<List<Object>, Long> mapBarnPåFødselsOgDødsdato(List<UidentifisertBarn> barn) {
        return barn.stream().collect(Collectors.groupingBy(b -> List.of(b.getFødselsdato(), b.getDødsdato()), Collectors.counting()));
    }
}
