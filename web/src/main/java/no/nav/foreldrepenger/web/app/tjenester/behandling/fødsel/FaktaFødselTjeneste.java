package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    public void overstyrFaktaOmFødsel(Long behandlingId, OverstyringFaktaOmFødselDto dto) {
        // TODO: Legg til håndtering av om verdien kan endres
        // TODO: Implementer overstyring av fakta om fødsel
        // TODO: Husk å overstyre antall barn også, ved å bruke dto.getAntallBarn()
        // TODO: Sjekk overstyring av antall barn hvis det er registrert noe i freg
        // TODO: Lagres antall barn som en faktisk verdi, eller lagrer vi bare barna og regner ut antallet?

        // TODO: Case: Født i utlandet, ikke registrert i FREG. Barn dør like etter fødsel. Dødsdato må overstyres
    }

    public FødselDto hentFaktaOmFødsel(Long behandlingId) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);
        var terminbekreftelse = familieHendelse.getSøknadVersjon().getTerminbekreftelse();

        return new FødselDto(new FødselDto.Søknad(getBarn(familieHendelse.getSøknadVersjon()),
                terminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null),
                terminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null)),
                new FødselDto.Register(familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList)),
                new FødselDto.Gjeldende(mapTermindato(familieHendelse), mapUtstedtdato(familieHendelse), mapBarn(familieHendelse)));
    }

    private FødselDto.Gjeldende.Utstedtdato mapUtstedtdato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtUtstedtdato = familieHendelse.getOverstyrtVersjon()
                .flatMap(fhe -> fhe.getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato));
        var søknadUtstedtdato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato);
        Kilde kilde = ((overstyrtUtstedtdato.isEmpty() && søknadUtstedtdato.isPresent()) || Objects.equals(overstyrtUtstedtdato,
                søknadUtstedtdato)) ? Kilde.SØKNAD : Kilde.SAKSBEHANDLER;
        return new FødselDto.Gjeldende.Utstedtdato(kilde, kilde == Kilde.SØKNAD ? søknadUtstedtdato.orElse(null) : overstyrtUtstedtdato.orElse(null));
    }

    private static FødselDto.Gjeldende.Termindato mapTermindato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElse(null);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
        var kilde = ((overstyrtTermindato == null) || Objects.equals(overstyrtTermindato, søknadTermindato))
                ? Kilde.SØKNAD
                : Kilde.SAKSBEHANDLER;
        return new FødselDto.Gjeldende.Termindato(kilde, kilde == Kilde.SØKNAD ? søknadTermindato : overstyrtTermindato, true);
    }

    private List<FødselDto.Gjeldende.Barn> mapBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var gjeldendeBarn = new ArrayList<FødselDto.Gjeldende.Barn>();
        var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();
        var bekreftedeBarn = familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList);
        var overstyrtBarn = familieHendelse.getOverstyrtVersjon().map(this::getBarn).orElseGet(Collections::emptyList);

        if (!overstyrtBarn.isEmpty()) {
            gjeldendeBarn.addAll(
                    overstyrtBarn.stream()
                            .map(barn -> new FødselDto.Gjeldende.Barn(Kilde.SAKSBEHANDLER, barn, true))
                            .toList()
            );
        }
        if (!bekreftedeBarn.isEmpty()) {
            gjeldendeBarn.addAll(
                    bekreftedeBarn.stream()
                            .map(barn -> new FødselDto.Gjeldende.Barn(Kilde.FOLKEREGISTER, barn, false))
                            .toList()
            );
        }
        if (overstyrtBarn.isEmpty() && bekreftedeBarn.isEmpty() && !søknadBarn.isEmpty()) {
            gjeldendeBarn.addAll(
                    søknadBarn.stream()
                            .map(barn -> new FødselDto.Gjeldende.Barn(Kilde.SØKNAD, new AvklartBarnDto(barn.getFødselsdato(), barn.getFødselsdato()), true))
                            .toList()
            );
        }

        return gjeldendeBarn;
    }

    private List<AvklartBarnDto> getBarn(FamilieHendelseEntitet familieHendelse) {
        return familieHendelse == null ? Collections.emptyList() : familieHendelse.getBarna()
                .stream()
                .map(barnEntitet -> new AvklartBarnDto(barnEntitet.getFødselsdato(), barnEntitet.getDødsdato().orElse(null)))
                .toList();
    }
}
