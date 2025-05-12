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
        // TODO: Sjekk overstyring av antall barn hvis det er registrert noe i freg
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
