package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;

/**
 * Bygger et sammen satt resultat av avklarte data for en Familiehendelse (fødsel, adopsjon, omsorgsovertagelse)
 */
public class FamiliehendelseDataDtoTjeneste {

    private static final Integer ANTALL_UKER_I_SVANGERSKAP = 40;

    FamiliehendelseDataDtoTjeneste() {
        // for CDI proxy
    }

    public static Optional<FamiliehendelseDto> mapFra(Optional<FamilieHendelseGrunnlagEntitet> grunnlag,
                                                      Optional<LocalDate> vedtaksdato,
                                                      Behandling behandling) {
        return grunnlag.flatMap(g -> FamiliehendelseDataDtoTjeneste.mapHendelseGrunnlag(g, vedtaksdato, behandling));
    }

    public static FamilieHendelseGrunnlagDto mapGrunnlagFra(Optional<FamilieHendelseGrunnlagEntitet> grunnlag,
                                                            Optional<LocalDate> vedtaksdato,
                                                            Behandling behandling) {
        var dto = new FamilieHendelseGrunnlagDto();
        if (grunnlag.isPresent()) {
            var grunnlaget = grunnlag.get();
            dto.setOppgitt(mapHendelse(grunnlaget.getSøknadVersjon(), vedtaksdato, behandling));
            grunnlaget.getBekreftetVersjon().ifPresent(versjon -> dto.setRegister(mapHendelse(versjon, vedtaksdato, behandling)));

            mapHendelseGrunnlag(grunnlaget, vedtaksdato, behandling).ifPresent(dto::setGjeldende);
        }
        return dto;
    }

    private static FamiliehendelseDto mapHendelse(FamilieHendelseEntitet hendelse, Optional<LocalDate> vedtaksdato, Behandling behandling) {
        if (hendelse.getGjelderFødsel()) {
            return lagFodselDto(hendelse, vedtaksdato, behandling);
        }
        if (FamilieHendelseType.ADOPSJON.equals(hendelse.getType())) {
            return lagAdopsjonDto(hendelse);
        }
        if (FamilieHendelseType.OMSORG.equals(hendelse.getType())) {
            return lagOmsorgDto(hendelse);
        }
        return null;
    }

    private static FamiliehendelseDto lagOmsorgDto(FamilieHendelseEntitet hendelse) {
        var dto = new AvklartDataOmsorgDto(SøknadType.fra(hendelse));
        var fødselsdatoer = tilFødselsMap(hendelse);
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
        var dto = new AvklartDataAdopsjonDto();
        var fødselsdatoer = tilFødselsMap(hendelse);

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
        return hendelse.getBarna().stream().collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
    }

    private static FamiliehendelseDto lagFodselDto(FamilieHendelseEntitet hendelse, Optional<LocalDate> vedtaksdato, Behandling behandling) {
        var dto = new AvklartDataFodselDto();
        mapTerminbekreftelse(hendelse, dto, vedtaksdato);
        mapFødsler(hendelse, dto);
        dto.setMorForSykVedFodsel(hendelse.erMorForSykVedFødsel());
        dto.setSkjæringstidspunkt(hendelse.getSkjæringstidspunkt());
        return dto;
    }

    private static void mapFødsler(FamilieHendelseEntitet hendelse, AvklartDataFodselDto dto) {
        var barn = hendelse.getBarna().stream().map(barna -> new AvklartBarnDto(barna.getFødselsdato(), barna.getDødsdato().orElse(null))).toList();
        dto.setAvklartBarn(barn);
    }

    private static void mapTerminbekreftelse(FamilieHendelseEntitet hendelse, AvklartDataFodselDto dto, Optional<LocalDate> vedtaksdato) {
        hendelse.getTerminbekreftelse().ifPresent(terminbekreftelse -> {
            dto.setTermindato(terminbekreftelse.getTermindato());
            dto.setUtstedtdato(terminbekreftelse.getUtstedtdato());
            dto.setAntallBarnTermin(hendelse.getAntallBarn());
            finnUkerUtISvangerskapet(terminbekreftelse, vedtaksdato.orElse(null)).ifPresent(dto::setVedtaksDatoSomSvangerskapsuke);
        });
    }

    private static Optional<FamiliehendelseDto> mapHendelseGrunnlag(FamilieHendelseGrunnlagEntitet grunnlag,
                                                                    Optional<LocalDate> vedtaksdato,
                                                                    Behandling behandling) {
        if (grunnlag.getGjeldendeVersjon().getGjelderFødsel()) {
            return lagFodselDto(grunnlag, vedtaksdato, behandling);
        }
        if (FamilieHendelseType.ADOPSJON.equals(grunnlag.getGjeldendeVersjon().getType())) {
            return lagAdopsjonDto(grunnlag);
        }
        if (FamilieHendelseType.OMSORG.equals(grunnlag.getGjeldendeVersjon().getType())) {
            return lagOmsorgDto(grunnlag);
        }
        return Optional.empty();
    }

    private static Optional<FamiliehendelseDto> lagFodselDto(FamilieHendelseGrunnlagEntitet grunnlag,
                                                             Optional<LocalDate> vedtaksdato,
                                                             Behandling behandling) {
        var dto = new AvklartDataFodselDto();
        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(hendelse -> {
            mapTerminbekreftelse(hendelse, dto, vedtaksdato);
            mapFødsler(hendelse, dto);
            if (grunnlag.getHarOverstyrteData() && grunnlag.getOverstyrtVersjon().get().getType().equals(FamilieHendelseType.FØDSEL)) {
                dto.setBrukAntallBarnFraTps(harValgtSammeSomBekreftet(grunnlag));
            }
            dto.setDokumentasjonForligger(mapDokumentasjonForligger(grunnlag, behandling));
            dto.setMorForSykVedFodsel(hendelse.erMorForSykVedFødsel());
            dto.setSkjæringstidspunkt(hendelse.getSkjæringstidspunkt());
        });
        return Optional.of(dto);
    }

    private static Boolean mapDokumentasjonForligger(FamilieHendelseGrunnlagEntitet familieHendelse, Behandling behandling) {
        var harUtførtAP = behandling.harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        if (!harUtførtAP) {
            return null;
        }
        return familieHendelse.getOverstyrtVersjon().filter(o -> !o.getBarna().isEmpty()).isPresent();
    }


    private static Optional<Long> finnUkerUtISvangerskapet(TerminbekreftelseEntitet terminbekreftelse, LocalDate originalVedtaksDato) {
        var termindato = terminbekreftelse.getTermindato();

        if (originalVedtaksDato != null && termindato != null) {
            var termindatoMinusUkerISvangerskap = termindato.minusWeeks(ANTALL_UKER_I_SVANGERSKAP);
            return Optional.of(ChronoUnit.WEEKS.between(termindatoMinusUkerISvangerskap, originalVedtaksDato) + 1);
        }
        return Optional.empty();
    }

    private static boolean harValgtSammeSomBekreftet(FamilieHendelseGrunnlagEntitet grunnlag) {
        var bekreftet = grunnlag.getBekreftetVersjon();
        var overstyrt = grunnlag.getOverstyrtVersjon().orElseThrow();

        var antallBarnLike = false;
        var fødselsdatoLike = false;
        if (bekreftet.isPresent()) {
            antallBarnLike = Objects.equals(bekreftet.get().getAntallBarn(), overstyrt.getAntallBarn());
            fødselsdatoLike = Objects.equals(bekreftet.get().getFødselsdato(), overstyrt.getFødselsdato());
        }
        return antallBarnLike && fødselsdatoLike || bekreftet.isEmpty() && overstyrt.getBarna().isEmpty();
    }

    private static Optional<FamiliehendelseDto> lagAdopsjonDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        var dto = new AvklartDataAdopsjonDto();

        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(bekreftet -> mapAdopsjon(bekreftet, dto, tilFødselsMap(bekreftet)));
        return Optional.of(dto);
    }

    private static Optional<FamiliehendelseDto> lagOmsorgDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        var dto = new AvklartDataOmsorgDto(SøknadType.fra(grunnlag.getGjeldendeVersjon()));
        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(bekreftet -> mapOmsorg(bekreftet, dto, tilFødselsMap(bekreftet)));
        return Optional.of(dto);
    }
}
