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

        // TODO: Case: Født i utlandet, ikke registrert i FREG. Barn dør like etter fødsel. Dødsdato må overstyres

        // TODO: Undersøk prematur dødfødsel, hva skjer da med freg og denne overstyringen?

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
        // TODO: Termindato kan aldri være i bekreftet/register?
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElse(null);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
        var kilde = Objects.equals(overstyrtTermindato, søknadTermindato) ? Kilde.SØKNAD : Kilde.SBH;
        return new FødselDto.Gjeldende.Termindato(kilde, kilde == Kilde.SØKNAD ? søknadTermindato : overstyrtTermindato, true);
    }

    private FødselDto.Gjeldende.Barn mapBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var kilde = getKildeForBarn(familieHendelse);
        // TODO: Hvis det er fra bekreftet så kan man ikke overstyre, eller? Undersøk om det populeres fra søknad hvis null
        var barn = switch (kilde) {
            case SBH -> getBarn(familieHendelse.getOverstyrtVersjon().orElse(null));
            case FREG -> getBarn(familieHendelse.getBekreftetVersjon().orElse(null));
            case SØKNAD -> getBarn(familieHendelse.getSøknadVersjon());
        };
        return new FødselDto.Gjeldende.Barn(kilde, barn, kilde != Kilde.FREG);
    }

    private List<AvklartBarnDto> getBarn(FamilieHendelseEntitet familieHendelse) {
        return familieHendelse == null ? Collections.emptyList() : familieHendelse.getBarna()
            .stream()
            .map(barnEntitet -> new AvklartBarnDto(barnEntitet.getFødselsdato(), barnEntitet.getDødsdato().orElse(null)))
            .toList();
    }

    private static Kilde getKildeForBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        // TODO: Finnes det en situasjon er det ikke ligger barn i noen av disse? Kan overstyrer gå inn før sbh har løst ap?
        var overstyrteBarn = familieHendelse.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        var bekreftedeBarn = familieHendelse.getBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();
        // TODO: Kan man mangle bekreftede barn, og få ap som man løser og deretter får man inn bekreftede barn?

        // TODO: Tenk litt på om bekreftet og søknad alltid kan være like, og overstyrt er forskjellig
        if (!harLikeBarn(overstyrteBarn, søknadBarn)) {
            return Kilde.SBH;
        }

        return harLikeBarn(bekreftedeBarn, søknadBarn) ? Kilde.SØKNAD : Kilde.FREG;
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
