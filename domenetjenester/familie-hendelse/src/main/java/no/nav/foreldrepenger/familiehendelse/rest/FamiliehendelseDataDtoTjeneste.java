package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;

/**
 * Bygger et sammen satt resultat av avklarte data for en Familiehendelse (fødsel, adopsjon, omsorgsovertagelse)
 */
public class FamiliehendelseDataDtoTjeneste {

    // TODO (OJR) Bør denne hardkodast her? FC: NOPE
    private static final Integer ANTALL_UKER_I_SVANGERSKAP = 40;

    FamiliehendelseDataDtoTjeneste() {
        // for CDI proxy
    }

    public static Optional<FamiliehendelseDto> mapFra(Behandling behandling, Optional<FamilieHendelseGrunnlagEntitet> grunnlag, Optional<LocalDate> vedtaksdato) {
        return grunnlag.flatMap(g -> FamiliehendelseDataDtoTjeneste.mapHendelseGrunnlag(g, behandling, vedtaksdato));
    }

    public static FamilieHendelseGrunnlagDto mapGrunnlagFra(Behandling behandling, Optional<FamilieHendelseGrunnlagEntitet> grunnlag, Optional<LocalDate> vedtaksdato) {
        FamilieHendelseGrunnlagDto dto = new FamilieHendelseGrunnlagDto();
        if (grunnlag.isPresent()) {
            FamilieHendelseGrunnlagEntitet grunnlaget = grunnlag.get();
            dto.setOppgitt(mapHendelse(grunnlaget.getSøknadVersjon(), behandling, vedtaksdato));
            grunnlaget.getBekreftetVersjon().ifPresent(versjon -> dto.setRegister(mapHendelse(versjon, behandling, vedtaksdato)));

            mapHendelseGrunnlag(grunnlaget, behandling, vedtaksdato).ifPresent(dto::setGjeldende);
        }
        return dto;
    }

    private static FamiliehendelseDto mapHendelse(FamilieHendelseEntitet hendelse, Behandling behandling, Optional<LocalDate> vedtaksdato) {
        if (hendelse.getGjelderFødsel()) {
            return lagFodselDto(hendelse, behandling, vedtaksdato);
        } else if (FamilieHendelseType.ADOPSJON.equals(hendelse.getType())) {
            return lagAdopsjonDto(hendelse);
        } else if (FamilieHendelseType.OMSORG.equals(hendelse.getType())) {
            return lagOmsorgDto(hendelse);
        }
        return null;
    }

    private static FamiliehendelseDto lagOmsorgDto(FamilieHendelseEntitet hendelse) {
        AvklartDataOmsorgDto dto = new AvklartDataOmsorgDto(SøknadType.fra(hendelse));
        Map<Integer, LocalDate> fødselsdatoer = tilFødselsMap(hendelse);
        mapOmsorg(hendelse, dto, fødselsdatoer);

        return dto;
    }

    private static void mapOmsorg(FamilieHendelseEntitet hendelse, AvklartDataOmsorgDto dto, Map<Integer, LocalDate> fødselsdatoer) {
        hendelse.getAdopsjon().ifPresent(adopsjon -> {
            dto.setOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelseDato());
            dto.setForeldreansvarDato(adopsjon.getForeldreansvarDato());
            dto.setVilkarType(adopsjon.getOmsorgovertakelseVilkår());
            dto.setFødselsdatoer(fødselsdatoer);
            dto.setAntallBarnTilBeregning(hendelse.getAntallBarn());
        });
    }

    private static FamiliehendelseDto lagAdopsjonDto(FamilieHendelseEntitet hendelse) {
        AvklartDataAdopsjonDto dto = new AvklartDataAdopsjonDto();
        Map<Integer, LocalDate> fødselsdatoer = tilFødselsMap(hendelse);

        mapAdopsjon(hendelse, dto, fødselsdatoer);

        return dto;
    }

    private static void mapAdopsjon(FamilieHendelseEntitet hendelse, AvklartDataAdopsjonDto dto, Map<Integer, LocalDate> fødselsdatoer) {
        hendelse.getAdopsjon().ifPresent(adopsjon -> {
            dto.setEktefellesBarn(adopsjon.getErEktefellesBarn());
            dto.setMannAdoptererAlene(adopsjon.getAdoptererAlene());
            dto.setOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelseDato());
            dto.setAnkomstNorge(adopsjon.getAnkomstNorgeDato());
            dto.setAdopsjonFodelsedatoer(fødselsdatoer);
            dto.setFødselsdatoer(fødselsdatoer);
        });
    }

    private static Map<Integer, LocalDate> tilFødselsMap(FamilieHendelseEntitet hendelse) {
        return hendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
    }

    private static FamiliehendelseDto lagFodselDto(FamilieHendelseEntitet hendelse, Behandling behandling, Optional<LocalDate> vedtaksdato) {
        AvklartDataFodselDto dto = new AvklartDataFodselDto();
        mapTerminbekreftelse(hendelse, behandling, dto, vedtaksdato);
        mapFødsler(hendelse, dto);
        dto.setMorForSykVedFodsel(hendelse.erMorForSykVedFødsel());
        dto.setSkjæringstidspunkt(hendelse.getSkjæringstidspunkt());
        return dto;
    }

    private static void mapFødsler(FamilieHendelseEntitet hendelse, AvklartDataFodselDto dto) {
        List<AvklartBarnDto> barn = hendelse.getBarna().stream().map(barna ->
            new AvklartBarnDto(barna.getFødselsdato(), barna.getDødsdato().orElse(null))).collect(Collectors.toList());
        dto.setAvklartBarn(barn);
    }

    private static void mapTerminbekreftelse(FamilieHendelseEntitet hendelse, Behandling behandling, AvklartDataFodselDto dto, Optional<LocalDate> vedtaksdato) {
        hendelse.getTerminbekreftelse().ifPresent(terminbekreftelse -> {
            dto.setTermindato(terminbekreftelse.getTermindato());
            dto.setUtstedtdato(terminbekreftelse.getUtstedtdato());
            dto.setAntallBarnTermin(hendelse.getAntallBarn());
            finnUkerUtISvangerskapet(terminbekreftelse, vedtaksdato.orElse(null)).ifPresent(dto::setVedtaksDatoSomSvangerskapsuke);
        });
    }

    private static Optional<FamiliehendelseDto> mapHendelseGrunnlag(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling, Optional<LocalDate> vedtaksdato) {
        if (grunnlag.getGjeldendeVersjon().getGjelderFødsel()) {
            return lagFodselDto(grunnlag, behandling, vedtaksdato);
        } else if (FamilieHendelseType.ADOPSJON.equals(grunnlag.getGjeldendeVersjon().getType())) {
            return lagAdopsjonDto(grunnlag);
        } else if (FamilieHendelseType.OMSORG.equals(grunnlag.getGjeldendeVersjon().getType())) {
            return lagOmsorgDto(grunnlag);
        }
        return Optional.empty();
    }

    private static Optional<FamiliehendelseDto> lagFodselDto(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling, Optional<LocalDate> vedtaksdato) {
        AvklartDataFodselDto dto = new AvklartDataFodselDto();
        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(hendelse -> {
            mapTerminbekreftelse(hendelse, behandling, dto, vedtaksdato);
            mapFødsler(hendelse, dto);
            if (grunnlag.getHarOverstyrteData() && grunnlag.getOverstyrtVersjon().get().getType().equals(FamilieHendelseType.FØDSEL)) {
                final boolean brukAntallBarnFraTps = harValgtSammeSomBekreftet(grunnlag);
                dto.setBrukAntallBarnFraTps(brukAntallBarnFraTps);
                dto.setErOverstyrt(grunnlag.getHarOverstyrteData());
            }
            dto.setMorForSykVedFodsel(hendelse.erMorForSykVedFødsel());
            dto.setSkjæringstidspunkt(hendelse.getSkjæringstidspunkt());
        });
        return Optional.of(dto);
    }

    private static Optional<Long> finnUkerUtISvangerskapet(TerminbekreftelseEntitet terminbekreftelse, LocalDate originalVedtaksDato) {
        LocalDate termindato = terminbekreftelse.getTermindato();

        if (originalVedtaksDato != null && termindato != null) {
            LocalDate termindatoMinusUkerISvangerskap = termindato.minusWeeks(ANTALL_UKER_I_SVANGERSKAP);
            return Optional.of(ChronoUnit.WEEKS.between(termindatoMinusUkerISvangerskap, originalVedtaksDato) + 1);
        } else {
            return Optional.empty();
        }
    }

    private static boolean harValgtSammeSomBekreftet(FamilieHendelseGrunnlagEntitet grunnlag) {
        final Optional<FamilieHendelseEntitet> bekreftet = grunnlag.getBekreftetVersjon();
        final FamilieHendelseEntitet overstyrt = grunnlag.getOverstyrtVersjon().get(); // NOSONAR

        boolean antallBarnLike = false;
        boolean fødselsdatoLike = false;
        if (bekreftet.isPresent()) {
            antallBarnLike = Objects.equals(bekreftet.get().getAntallBarn(), overstyrt.getAntallBarn());
            fødselsdatoLike = Objects.equals(bekreftet.get().getFødselsdato(), overstyrt.getFødselsdato());
        }
        return (antallBarnLike && fødselsdatoLike) || (!bekreftet.isPresent() && overstyrt.getBarna().isEmpty());
    }

    private static Optional<FamiliehendelseDto> lagAdopsjonDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        AvklartDataAdopsjonDto dto = new AvklartDataAdopsjonDto();

        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(bekreftet -> mapAdopsjon(bekreftet, dto, tilFødselsMap(bekreftet)));
        return Optional.of(dto);
    }

    private static Optional<FamiliehendelseDto> lagOmsorgDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        AvklartDataOmsorgDto dto = new AvklartDataOmsorgDto(SøknadType.fra(grunnlag.getGjeldendeVersjon()));
        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(bekreftet -> mapOmsorg(bekreftet, dto, tilFødselsMap(bekreftet)));
        return Optional.of(dto);
    }
}
