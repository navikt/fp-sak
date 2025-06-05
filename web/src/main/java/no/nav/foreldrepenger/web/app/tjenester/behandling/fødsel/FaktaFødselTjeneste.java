package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
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

    public FødselDto hentFaktaOmFødsel(Long behandlingId) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);
        var terminbekreftelse = familieHendelse.getSøknadVersjon().getTerminbekreftelse();
        var gjeldendeBarnListe = mapBarn(familieHendelse);

        return new FødselDto(new FødselDto.Søknad(getBarn(familieHendelse.getSøknadVersjon()),
            terminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null),
            terminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null), familieHendelse.getSøknadVersjon().getAntallBarn()),
            new FødselDto.Register(familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList)),
            new FødselDto.Gjeldende(mapTermindato(familieHendelse), mapUtstedtdato(familieHendelse), gjeldendeBarnListe,
                mapGjeldendeAntallBarn(familieHendelse, gjeldendeBarnListe)));
    }

    private int mapGjeldendeAntallBarn(FamilieHendelseGrunnlagEntitet familieHendelse, List<FødselDto.Gjeldende.GjeldendeBarn> gjeldendeBarnListe) {
        return gjeldendeBarnListe.isEmpty() ? familieHendelse.getSøknadVersjon().getAntallBarn() : gjeldendeBarnListe.size();
    }

    private FødselDto.Gjeldende.Utstedtdato mapUtstedtdato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtUtstedtdato = familieHendelse.getOverstyrtVersjon()
            .flatMap(fhe -> fhe.getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato));
        var søknadUtstedtdato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato);

        if (overstyrtUtstedtdato.isEmpty() && søknadUtstedtdato.isEmpty()) {
            return null; // Ingen utstedtdato tilgjengelig
        }

        Kilde kilde = overstyrtUtstedtdato.isEmpty() || Objects.equals(overstyrtUtstedtdato, søknadUtstedtdato) ? Kilde.SØKNAD : Kilde.SAKSBEHANDLER;
        return new FødselDto.Gjeldende.Utstedtdato(kilde, kilde == Kilde.SØKNAD ? søknadUtstedtdato.orElse(null) : overstyrtUtstedtdato.orElse(null));
    }

    private static FødselDto.Gjeldende.Termindato mapTermindato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        if (overstyrtTermindato.isEmpty() && søknadTermindato.isEmpty()) {
            return null; // Ingen termindato tilgjengelig
        }

        var kilde = overstyrtTermindato.isEmpty() || Objects.equals(overstyrtTermindato, søknadTermindato) ? Kilde.SØKNAD : Kilde.SAKSBEHANDLER;
        return new FødselDto.Gjeldende.Termindato(kilde, kilde == Kilde.SØKNAD ? søknadTermindato.orElse(null) : overstyrtTermindato.orElse(null),
            true);
    }

    private List<FødselDto.Gjeldende.GjeldendeBarn> mapBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var gjeldendeBarn = new ArrayList<FødselDto.Gjeldende.GjeldendeBarn>();
        var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();
        var bekreftedeBarn = familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList);
        var overstyrtBarn = familieHendelse.getOverstyrtVersjon().map(this::getBarn).orElseGet(Collections::emptyList);

        if (!bekreftedeBarn.isEmpty()) {
            bekreftedeBarn.stream()
                .map(barn -> new FødselDto.Gjeldende.GjeldendeBarn(Kilde.FOLKEREGISTER, barn, false))
                .forEach(gjeldendeBarn::add);
        }

        if (!overstyrtBarn.isEmpty()) {
            var bekreftedeBarnMap = bekreftedeBarn.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> new BarnNøkkel(b.fødselsdato(), b.dødsdato()), java.util.stream.Collectors.counting()));

            overstyrtBarn.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> new BarnNøkkel(b.fødselsdato(), b.dødsdato())))
                .forEach((nøkkel, barnListe) -> {
                    long antallBekreftede = bekreftedeBarnMap.getOrDefault(nøkkel, 0L);
                    barnListe.stream()
                        .skip(antallBekreftede)
                        .map(barn -> new FødselDto.Gjeldende.GjeldendeBarn(Kilde.SAKSBEHANDLER, barn, true))
                        .forEach(gjeldendeBarn::add);
                });
        }

        if (overstyrtBarn.isEmpty() && bekreftedeBarn.isEmpty() && !søknadBarn.isEmpty()) {
            søknadBarn.stream()
                .map(barn -> new FødselDto.Gjeldende.GjeldendeBarn(Kilde.SØKNAD,
                    new FødselDto.BarnHendelseData(barn.getFødselsdato(), barn.getDødsdato().orElse(null)), true))
                .forEach(gjeldendeBarn::add);
        }

        return gjeldendeBarn;
    }

    private List<FødselDto.BarnHendelseData> getBarn(FamilieHendelseEntitet familieHendelse) {
        return familieHendelse == null ? Collections.emptyList() : familieHendelse.getBarna()
            .stream()
            .map(barnEntitet -> new FødselDto.BarnHendelseData(barnEntitet.getFødselsdato(), barnEntitet.getDødsdato().orElse(null)))
            .toList();
    }

    private record BarnNøkkel(LocalDate fødselsdato, LocalDate dødsdato) {}
}
