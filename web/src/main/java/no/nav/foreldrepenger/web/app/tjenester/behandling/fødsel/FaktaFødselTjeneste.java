package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.modell.FødselStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
public class FaktaFødselTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FaktaFødselTjeneste.class);
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingRepository behandlingRepository;

    FaktaFødselTjeneste() {
        // For CDI proxy
    }

    @Inject
    public FaktaFødselTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste, BehandlingRepository behandlingRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void overstyrFaktaOmFødsel(Long behandlingId, OverstyringFaktaOmFødselDto dto) {
        LOG.info("Overstyrer fakta rundt fødsel for behandlingId {} til {}", behandlingId, dto);
        var oppdatere = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);

        validerFødselsdataForOverstyring(dto);

        if (dto.getBarn() == null) {
            var søknadAntallBarn = familieHendelse.getSøknadVersjon().getAntallBarn();
            var overstyrtAntallBarn = familieHendelse.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn);
            oppdatere.medTerminType().tilbakestillBarn().medAntallBarn(overstyrtAntallBarn.orElse(søknadAntallBarn));
        }

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

        var søknadData = new FødselDto.Søknad(getBarn(familieHendelse.getSøknadVersjon()),
            terminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null),
            terminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null), familieHendelse.getSøknadVersjon().getAntallBarn());

        var registerData = new FødselDto.Register(familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList));

        var gjeldendeData = new FødselDto.Gjeldende(mapTermin(familieHendelse), mapUtstedtdato(familieHendelse), mapAntallBarn(familieHendelse),
            mapBarn(familieHendelse), mapFødselDokumetasjonStatus(familieHendelse, behandlingId));

        return new FødselDto(søknadData, registerData, gjeldendeData);
    }

    private FødselDto.Gjeldende.FødselDokumetasjonStatus mapFødselDokumetasjonStatus(FamilieHendelseGrunnlagEntitet familieHendelse,
                                                                                     Long behandlingId) {
        var harUtførtAP = behandlingRepository.hentBehandling(behandlingId)
            .harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);

        if (!harUtførtAP && familieHendelse.getOverstyrtVersjon().isEmpty()) {
            return FødselDto.Gjeldende.FødselDokumetasjonStatus.IKKE_VURDERT;
        }

        return familieHendelse.getOverstyrtVersjon()
            .filter(o -> !o.getBarna().isEmpty())
            .map(o -> FødselDto.Gjeldende.FødselDokumetasjonStatus.DOKUMENTERT)
            .orElse(FødselDto.Gjeldende.FødselDokumetasjonStatus.IKKE_DOKUMENTERT);
    }

    private static void oppdaterBarnData(OverstyringFaktaOmFødselDto dto, FamilieHendelseBuilder oppdatere) {
        oppdatere.tilbakestillBarn().medAntallBarn(dto.getBarn().size());
        dto.getBarn().forEach(b -> oppdatere.leggTilBarn(b.getFødselsdato(), b.getDødsdato().orElse(null)));
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
        var fødselsdato = dto.getBarn().stream().map(DokumentertBarnDto::getFødselsdato).filter(Objects::nonNull).min(Comparator.naturalOrder());
        if (dto.getTermindato() != null && fødselsdato.isPresent()) {
            var fødselsintervall = FamilieHendelseTjeneste.intervallForTermindato(dto.getTermindato());
            if (!fødselsintervall.encloses(fødselsdato.get())) {
                throw new FunksjonellException("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
            }
        }
    }

    private static void validerDødsdatoerMotFødselsdatoer(OverstyringFaktaOmFødselDto dto) {
        dto.getBarn().forEach(barn -> {
            if (barn.getDødsdato().isPresent() && barn.getDødsdato().get().isBefore(barn.getFødselsdato())) {
                throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
            }
        });
    }

    private static boolean finnesUlikeBarn(OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        var dtoFødselStatus = dto.getBarn().stream().map(barn -> {
            var barnDto = new DokumentertBarnDto(barn.getFødselsdato(), barn.getDødsdato().orElse(null));
            var barnNummer = getBarnNummer(barn, dto.getBarn());
            return new FødselStatus(barnDto, barnNummer);
        }).toList();

        var grunnlagBarnFødselStatus = familieHendelse.getGjeldendeVersjon()
            .getBarna()
            .stream()
            .map(barn -> new FødselStatus(new DokumentertBarnDto(barn.getFødselsdato(), barn.getDødsdato().orElse(null)),
                getBarnNummer(barn, familieHendelse.getGjeldendeVersjon().getBarna())))
            .toList();

        return dtoFødselStatus.size() != grunnlagBarnFødselStatus.size() || harEndringerIBarnMedSammeNummer(dtoFødselStatus,
            grunnlagBarnFødselStatus);
    }

    private static boolean harEndringerIBarnMedSammeNummer(List<FødselStatus> dtoBarn, List<FødselStatus> grunnlagBarn) {
        return dtoBarn.stream()
            .anyMatch(dtoBarnStatus -> grunnlagBarn.stream()
                .filter(grunnlagBarnStatus -> Objects.equals(grunnlagBarnStatus.getBarnNummer(), dtoBarnStatus.getBarnNummer()))
                .anyMatch(
                    grunnlagBarnStatus -> !Objects.equals(dtoBarnStatus.getFødselsdato(), grunnlagBarnStatus.getFødselsdato()) || !Objects.equals(
                        dtoBarnStatus.getDødsdato(), grunnlagBarnStatus.getDødsdato())));
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

    private static FødselDto.Gjeldende.Termin mapTermin(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var overstyrtTermindato = familieHendelse.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato);
        var søknadTermindato = familieHendelse.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        if (overstyrtTermindato.isEmpty() && søknadTermindato.isEmpty()) {
            return null; // Ingen termindato tilgjengelig
        }

        var kilde = bestemKilde(overstyrtTermindato, søknadTermindato);
        var gjeldeneTermindato = utledTermindato(kilde, søknadTermindato, overstyrtTermindato);
        return new FødselDto.Gjeldende.Termin(kilde, gjeldeneTermindato);
    }

    private static Kilde bestemKilde(Optional<LocalDate> overstyrtDato, Optional<LocalDate> søknadDato) {
        return overstyrtDato.isEmpty() || Objects.equals(overstyrtDato, søknadDato) ? Kilde.SØKNAD : Kilde.SAKSBEHANDLER;
    }

    private static LocalDate utledUtstedtDato(Kilde kilde, Optional<LocalDate> søknadDato, Optional<LocalDate> overstyrtDato) {
        return kilde == Kilde.SØKNAD ? søknadDato.orElse(null) : overstyrtDato.orElse(null);
    }

    private static LocalDate utledTermindato(Kilde kilde, Optional<LocalDate> søknadDato, Optional<LocalDate> overstyrtDato) {
        return kilde == Kilde.SØKNAD ? søknadDato.orElse(null) : overstyrtDato.orElse(null);
    }

    private static Kilde bestemKilde(int søknadAntallBarn, Optional<Integer> bekreftetAntallBarn, Optional<Integer> overstyrtAntallBarn) {

        if (overstyrtAntallBarn.isPresent() && !Objects.equals(overstyrtAntallBarn.get(), søknadAntallBarn)) {
            return Kilde.SAKSBEHANDLER;
        }
        if (bekreftetAntallBarn.isPresent() && !Objects.equals(bekreftetAntallBarn.get(), søknadAntallBarn)) {
            return Kilde.FOLKEREGISTER;
        }
        return Kilde.SØKNAD;
    }

    private static FødselDto.Gjeldende.AntallBarn mapAntallBarn(FamilieHendelseGrunnlagEntitet familieHendelse) {
        var søknadAntallBarn = familieHendelse.getSøknadVersjon().getAntallBarn();
        var bekreftetAntallBarn = familieHendelse.getBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn);
        var overstyrtAntallBarn = familieHendelse.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn);

        var kilde = bestemKilde(søknadAntallBarn, bekreftetAntallBarn, overstyrtAntallBarn);

        var antallBarn = switch (kilde) {
            case SØKNAD -> søknadAntallBarn;
            case FOLKEREGISTER -> bekreftetAntallBarn.orElse(søknadAntallBarn);
            case SAKSBEHANDLER -> overstyrtAntallBarn.orElse(søknadAntallBarn);
        };

        return new FødselDto.Gjeldende.AntallBarn(kilde, antallBarn);
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
                .collect(
                    Collectors.groupingBy(FødselDto.BarnHendelseData::barnNummer, Collectors.collectingAndThen(Collectors.toList(), List::getFirst)));

            overstyrtBarn.forEach(barn -> {
                var bekreftetBarn = bekreftedeBarnMap.get(barn.barnNummer());
                if (bekreftetBarn == null || !erSammeBarn(bekreftetBarn, barn)) {
                    gjeldendeBarn.add(new FødselDto.Gjeldende.GjeldendeBarn(Kilde.SAKSBEHANDLER, barn, true));
                }
            });

            return sorterRiktigRekkefølgerPåGjeldendeBarn(gjeldendeBarn);
        }

        if (bekreftedeBarn.isEmpty() && !søknadBarn.isEmpty()) {
            søknadBarn.stream()
                .map(barn -> new FødselDto.Gjeldende.GjeldendeBarn(Kilde.SØKNAD,
                    new FødselDto.BarnHendelseData(barn.getFødselsdato(), barn.getDødsdato().orElse(null), barn.getBarnNummer()), true))
                .forEach(gjeldendeBarn::add);
        }

        return gjeldendeBarn;
    }

    private static boolean erSammeBarn(FødselDto.BarnHendelseData bekreftetBarn, FødselDto.BarnHendelseData overstyrtBarn) {
        return Objects.equals(bekreftetBarn.fødselsdato(), overstyrtBarn.fødselsdato()) && Objects.equals(bekreftetBarn.dødsdato(),
            overstyrtBarn.dødsdato());
    }

    private static List<FødselDto.Gjeldende.GjeldendeBarn> sorterRiktigRekkefølgerPåGjeldendeBarn(ArrayList<FødselDto.Gjeldende.GjeldendeBarn> gjeldendeBarn) {
        var folkeregisterBarn = gjeldendeBarn.stream().filter(b -> b.kilde() == Kilde.FOLKEREGISTER).toList();
        var overstyrteBarn = gjeldendeBarn.stream()
            .filter(b -> b.kilde() != Kilde.FOLKEREGISTER)
            .sorted(Comparator.comparingInt(barn -> barn.barn().barnNummer()))
            .toList();
        return Stream.concat(folkeregisterBarn.stream(), overstyrteBarn.stream()).toList();
    }

    private List<FødselDto.BarnHendelseData> getBarn(FamilieHendelseEntitet familieHendelse) {
        return familieHendelse == null ? Collections.emptyList() : familieHendelse.getBarna()
            .stream()
            .map(barn -> new FødselDto.BarnHendelseData(barn.getFødselsdato(), barn.getDødsdato().orElse(null),
                getBarnNummer(barn, familieHendelse.getBarna())))
            .toList();
    }

    private static <T> int getBarnNummer(T barn, List<T> barnListe) {
        return barnListe.indexOf(barn) + 1;
    }
}
