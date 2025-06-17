package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.*;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;
import no.nav.vedtak.exception.FunksjonellException;

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
        var oppdatere = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);

        validerFødselsdataForOverstyring(dto);

        if (harEndringerIBarnData(dto, familieHendelse)) {
            oppdaterBarnData(dto, oppdatere);
        }

        if (dto.getTermindato() != null) {
            oppdatere.medTerminbekreftelse(oppdatere.getTerminbekreftelseBuilder().medTermindato(dto.getTermindato()));
        }

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatere);
    }

    public FødselDto hentFaktaOmFødsel(Long behandlingId) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);
        var terminbekreftelse = familieHendelse.getSøknadVersjon().getTerminbekreftelse();
        var gjeldendeBarnListe = mapBarn(familieHendelse);

        var søknadData = new FødselDto.Søknad(
                getBarn(familieHendelse.getSøknadVersjon()),
                terminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null),
                terminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null),
                familieHendelse.getSøknadVersjon().getAntallBarn()
        );

        var registerData = new FødselDto.Register(
                familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList)
        );

        var gjeldendeData = new FødselDto.Gjeldende(
                mapTermin(familieHendelse, gjeldendeBarnListe),
                mapUtstedtdato(familieHendelse),
                gjeldendeBarnListe
        );

        return new FødselDto(søknadData, registerData, gjeldendeData);
    }

    private static void oppdaterBarnData(OverstyringFaktaOmFødselDto dto, FamilieHendelseBuilder oppdatere) {
        oppdatere.tilbakestillBarn().medAntallBarn(dto.getAntallBarn());
        dto.getBarn().forEach(b -> oppdatere.leggTilBarn(b.getFodselsdato(), b.getDodsdato().orElse(null)));
    }

    private static boolean harEndringerIBarnData(OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        return dto.getBarn() != null && !dto.getBarn().isEmpty() && finnesUlikeBarn(dto, familieHendelse);
    }

    private static void validerFødselsdataForOverstyring(OverstyringFaktaOmFødselDto dto) {
        if (dto.getBarn() == null || dto.getBarn().isEmpty()) {
            return; // Ingen fødselsdato/barn å validere
        }

        validerDødsdatoerMotFødselsdatoer(dto);
        sjekkGyldigTerminFødsel(dto);
    }

    private static void sjekkGyldigTerminFødsel(OverstyringFaktaOmFødselDto dto) {
        var fødselsdato = dto.getBarn().stream().map(UidentifisertBarnDto::getFodselsdato).min(Comparator.naturalOrder());
        if (dto.getTermindato()!= null && fødselsdato.isPresent()) {
            var fødselsintervall = FamilieHendelseTjeneste.intervallForTermindato(dto.getTermindato());
            if (!fødselsintervall.encloses(fødselsdato.get())) {
                throw new FunksjonellException("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
            }
        }
    }

    private static void validerDødsdatoerMotFødselsdatoer(OverstyringFaktaOmFødselDto dto) {
        dto.getBarn().forEach(barn -> {
            if (barn.getDodsdato().isPresent() && barn.getDodsdato().get().isBefore(barn.getFodselsdato())) {
                throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
            }
        });
    }

    private static boolean finnesUlikeBarn(OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        var dtoBarnNøkler = grupperBarnEtterNøkkel(dto.getBarn().stream()
                .map(b -> new BarnNøkkel(b.getFodselsdato(), b.getDodsdato().orElse(null))));

        var grunnlagBarnNøkler = grupperBarnEtterNøkkel(familieHendelse.getGjeldendeVersjon().getBarna().stream()
                .map(b -> new BarnNøkkel(b.getFødselsdato(), b.getDødsdato().orElse(null))));

        return finnUlikeNøkler(dtoBarnNøkler, grunnlagBarnNøkler);
    }

    private static Map<BarnNøkkel, Long> grupperBarnEtterNøkkel(Stream<BarnNøkkel> barn) {
        return barn.collect(Collectors.groupingBy(n -> n, Collectors.counting()));
    }

    private static boolean finnUlikeNøkler(Map<BarnNøkkel, Long> dtoBarnNøkler, Map<BarnNøkkel, Long> grunnlagBarnNøkler) {
        var alleNøkler = new HashSet<>(dtoBarnNøkler.keySet());
        alleNøkler.addAll(grunnlagBarnNøkler.keySet());

        return alleNøkler.stream()
                .anyMatch(nøkkel -> !Objects.equals(
                        dtoBarnNøkler.getOrDefault(nøkkel, 0L),
                        grunnlagBarnNøkler.getOrDefault(nøkkel, 0L)));
    }

    private FødselDto.Gjeldende.Utstedtdato mapUtstedtdato(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtUtstedtdato = familieHendelse.getOverstyrtVersjon()
                .flatMap(fhe -> fhe.getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato));
        var søknadUtstedtdato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato);

        if (overstyrtUtstedtdato.isEmpty() && søknadUtstedtdato.isEmpty()) {
            return null; // Ingen utstedtdato tilgjengelig
        }

        var kilde = bestemKilde(overstyrtUtstedtdato, søknadUtstedtdato);
        var utstedtDato = utledUtstedtDato(kilde, søknadUtstedtdato, overstyrtUtstedtdato);
        return new FødselDto.Gjeldende.Utstedtdato(kilde, utstedtDato);
    }

    private static FødselDto.Gjeldende.Termin mapTermin(FamilieHendelseGrunnlagEntitet familieHendelse,
                                                        List<FødselDto.Gjeldende.GjeldendeBarn> gjeldendeBarnListe) {
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        if (overstyrtTermindato.isEmpty() && søknadTermindato.isEmpty()) {
            return null; // Ingen termindato tilgjengelig
        }

        var kilde = bestemKilde(overstyrtTermindato, søknadTermindato);
        var gjeldeneTermindato = utledTermindato(kilde, søknadTermindato, overstyrtTermindato);
        var antallBarn = utledAntallBarn(gjeldendeBarnListe, familieHendelse);
        return new FødselDto.Gjeldende.Termin(kilde, gjeldeneTermindato, antallBarn);
    }

    private static Kilde bestemKilde(java.util.Optional<LocalDate> overstyrtDato, java.util.Optional<LocalDate> søknadDato) {
        return overstyrtDato.isEmpty() || Objects.equals(overstyrtDato, søknadDato) ? Kilde.SØKNAD : Kilde.SAKSBEHANDLER;
    }

    private static LocalDate utledUtstedtDato(Kilde kilde, java.util.Optional<LocalDate> søknadDato, java.util.Optional<LocalDate> overstyrtDato) {
        return kilde == Kilde.SØKNAD ? søknadDato.orElse(null) : overstyrtDato.orElse(null);
    }

    private static LocalDate utledTermindato(Kilde kilde, java.util.Optional<LocalDate> søknadDato, java.util.Optional<LocalDate> overstyrtDato) {
        return kilde == Kilde.SØKNAD ? søknadDato.orElse(null) : overstyrtDato.orElse(null);
    }

    private static int utledAntallBarn(List<FødselDto.Gjeldende.GjeldendeBarn> gjeldendeBarnListe, FamilieHendelseGrunnlagEntitet familieHendelse) {
        return gjeldendeBarnListe.isEmpty() ? familieHendelse.getSøknadVersjon().getAntallBarn() : gjeldendeBarnListe.size();
    }

    private List<FødselDto.Gjeldende.GjeldendeBarn> mapBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var gjeldendeBarn = new ArrayList<FødselDto.Gjeldende.GjeldendeBarn>();
        var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();
        var bekreftedeBarn = familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList);
        var overstyrtBarn = familieHendelse.getOverstyrtVersjon().map(this::getBarn).orElseGet(Collections::emptyList);

        if (!bekreftedeBarn.isEmpty()) {
            bekreftedeBarn.stream().map(barn -> new FødselDto.Gjeldende.GjeldendeBarn(Kilde.FOLKEREGISTER, barn, false)).forEach(gjeldendeBarn::add);
        }

        if (!overstyrtBarn.isEmpty()) {
            var bekreftedeBarnMap = bekreftedeBarn.stream()
                    .collect(java.util.stream.Collectors.groupingBy(b -> new BarnNøkkel(b.fødselsdato(), b.dødsdato()),
                            java.util.stream.Collectors.counting()));

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

    private record BarnNøkkel(LocalDate fødselsdato, LocalDate dødsdato) {
    }
}
