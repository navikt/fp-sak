package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

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
        var kilde = getKildeForTermindato(familieHendelse);
        // TODO: Termindato kan aldri være i bekreftet/register?
        var termindato = switch (kilde) {
            case SBH -> familieHendelse.getOverstyrtVersjon().get().getTermindato().orElse(null);
            case SØKNAD -> familieHendelse.getSøknadVersjon().getTermindato().orElse(null);
            default -> null;
        };

        //        overstyrtTermindato.ifPresent(ot -> {
        //            bekreftetTermindato.ifPresent(bt -> {
        //                return new FødselDto.Gjeldende.Termindato(ot.equals(bt) ? Kilde.SBH : Kilde.FREG, ot, true);
        //            });
        //            if (søknadTermindato.isPresent()) {
        //                søknadTermindato.get().map(
        //            } return new FødselDto.Gjeldende.Termindato(Kilde.SBH, overstyrt.get().getTermindato().orElse(null), true);
        //
        //        });
        return new FødselDto.Gjeldende.Termindato(kilde, termindato, true);
    }

    private static Kilde getKildeForTermindato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElse(null);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);

        return Objects.equals(overstyrtTermindato, søknadTermindato) ? Kilde.SØKNAD : Kilde.FREG;
    }

    private FødselDto.Gjeldende.Barn mapBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var kilde = getKildeForBarn(familieHendelse);
        // TODO: Hvis det er fra bekreftet så kan man ikke overstyre, eller? Undersøk om det populeres fra søknad hvis null
        // TODO: Sjekk om man kan løse .get() feilen på en måte
        var barn = switch (kilde) {
            case SBH -> getBarn(familieHendelse.getOverstyrtVersjon().get());
            case FREG -> getBarn(familieHendelse.getBekreftetVersjon().get());
            case SØKNAD -> getBarn(familieHendelse.getSøknadVersjon());
        };
        var kanOverstyre = kilde != Kilde.SBH;
        return new FødselDto.Gjeldende.Barn(kilde, barn, kanOverstyre);
    }

    // TODO: Gjør metoder static
    private List<AvklartBarnDto> getBarn(FamilieHendelseEntitet familieHendelse) {
        return familieHendelse.getBarna().stream()
            .map(barnEntitet -> new AvklartBarnDto(barnEntitet.getFødselsdato(), barnEntitet.getDødsdato().orElse(null)))
            .toList();
    }

    private static Kilde getKildeForBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrteBarn = familieHendelse.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        var bekreftedeBarn = familieHendelse.getBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();

        if (!harLikeBarn(overstyrteBarn, bekreftedeBarn)) {
            return Kilde.SBH;
        }

        return harLikeBarn(bekreftedeBarn, søknadBarn) ? Kilde.SØKNAD : Kilde.FREG;
    }

    private static boolean harLikeBarn(List<UidentifisertBarn> barn1, List<UidentifisertBarn> barn2) {
        if (barn1.size() != barn2.size()) {
            return false;
        }
        return IntStream.range(0, barn1.size()).allMatch(i -> {
            var barnA = barn1.get(i);
            var barnB = barn2.get(i);
            return barnA.getFødselsdato().equals(barnB.getFødselsdato()) && barnA.getDødsdato().equals(barnB.getDødsdato());
        });
    }
}
