package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

/**
 * Bygger et sammen satt resultat av avklarte data for en Familiehendelse (fødsel, adopsjon, omsorgsovertagelse)
 */
@ApplicationScoped
public class FamiliehendelseDataDtoTjeneste {

    // TODO (OJR) Bør denne hardkodast her? FC: NOPE
    private static final Integer ANTALL_UKER_I_SVANGERSKAP = 40;

    private BehandlingRepositoryProvider repositoryProvider;

    FamiliehendelseDataDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FamiliehendelseDataDtoTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.repositoryProvider = behandlingRepositoryProvider;
    }

    Optional<FamiliehendelseDto> mapFra(Behandling behandling) {
        return mapFraType(behandling);
    }

    FamilieHendelseGrunnlagDto mapGrunnlagFra(Behandling behandling) {
        FamilieHendelseGrunnlagDto dto = new FamilieHendelseGrunnlagDto();
        Optional<FamilieHendelseGrunnlagEntitet> grunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregatHvisEksisterer(behandling.getId());
        if (grunnlag.isPresent()) {
            FamilieHendelseGrunnlagEntitet grunnlaget = grunnlag.get();
            dto.setOppgitt(mapHendelse(grunnlaget.getSøknadVersjon(), behandling));
            grunnlaget.getBekreftetVersjon().ifPresent(versjon -> dto.setRegister(mapHendelse(versjon, behandling)));

            mapHendelseGrunnlag(grunnlaget, behandling).ifPresent(dto::setGjeldende);
        }
        return dto;
    }

    private FamiliehendelseDto mapHendelse(FamilieHendelseEntitet hendelse, Behandling behandling) {
        if (hendelse.getGjelderFødsel()) {
            return lagFodselDto(hendelse, behandling);
        } else if (FamilieHendelseType.ADOPSJON.equals(hendelse.getType())) {
            return lagAdopsjonDto(hendelse);
        } else if (FamilieHendelseType.OMSORG.equals(hendelse.getType())) {
            return lagOmsorgDto(hendelse);
        }
        return null;
    }

    private FamiliehendelseDto lagOmsorgDto(FamilieHendelseEntitet hendelse) {
        AvklartDataOmsorgDto dto = new AvklartDataOmsorgDto(SøknadType.fra(hendelse));
        mapOmsorg(hendelse, dto);
        dto.setAntallBarnTilBeregning(hendelse.getAntallBarn());

        return dto;
    }

    private void mapOmsorg(FamilieHendelseEntitet hendelse, AvklartDataOmsorgDto dto) {
        hendelse.getAdopsjon().ifPresent(adopsjon -> {
            dto.setOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelseDato());
            dto.setForeldreansvarDato(adopsjon.getForeldreansvarDato());
            dto.setVilkarType(adopsjon.getOmsorgovertakelseVilkår());
        });
    }

    private FamiliehendelseDto lagAdopsjonDto(FamilieHendelseEntitet hendelse) {
        AvklartDataAdopsjonDto dto = new AvklartDataAdopsjonDto();
        Map<Integer, LocalDate> fødselsdatoer = tilFødselsMap(hendelse);

        mapAdopsjon(hendelse, dto, fødselsdatoer);

        return dto;
    }

    private void mapAdopsjon(FamilieHendelseEntitet hendelse, AvklartDataAdopsjonDto dto, Map<Integer, LocalDate> fødselsdatoer) {
        hendelse.getAdopsjon().ifPresent(adopsjon -> {
            dto.setEktefellesBarn(adopsjon.getErEktefellesBarn());
            dto.setMannAdoptererAlene(adopsjon.getAdoptererAlene());
            dto.setOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelseDato());
            dto.setAnkomstNorge(adopsjon.getAnkomstNorgeDato());
            dto.setAdopsjonFodelsedatoer(fødselsdatoer);
        });
    }

    private Map<Integer, LocalDate> tilFødselsMap(FamilieHendelseEntitet hendelse) {
        return hendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
    }

    private FamiliehendelseDto lagFodselDto(FamilieHendelseEntitet hendelse, Behandling behandling) {
        AvklartDataFodselDto dto = new AvklartDataFodselDto();
        mapTerminbekreftelse(hendelse, behandling, dto);
        mapFødsler(hendelse, dto);
        dto.setMorForSykVedFodsel(hendelse.erMorForSykVedFødsel());
        dto.setSkjæringstidspunkt(hendelse.getSkjæringstidspunkt());
        return dto;
    }

    private void mapFødsler(FamilieHendelseEntitet hendelse, AvklartDataFodselDto dto) {
        List<AvklartBarnDto> barn = hendelse.getBarna().stream().map(barna ->
            new AvklartBarnDto(barna.getFødselsdato(), barna.getDødsdato().orElse(null))).collect(Collectors.toList());
        dto.setAvklartBarn(barn);
    }

    private void mapTerminbekreftelse(FamilieHendelseEntitet hendelse, Behandling behandling, AvklartDataFodselDto dto) {
        hendelse.getTerminbekreftelse().ifPresent(terminbekreftelse -> {
            dto.setTermindato(terminbekreftelse.getTermindato());
            dto.setUtstedtdato(terminbekreftelse.getUtstedtdato());
            dto.setAntallBarnTermin(hendelse.getAntallBarn());
            finnUkerUtISvangerskapet(terminbekreftelse, behandling.getOriginalVedtaksDato()).ifPresent(dto::setVedtaksDatoSomSvangerskapsuke);
        });
    }

    private Optional<FamiliehendelseDto> mapFraType(Behandling behandling) {
        final Optional<FamilieHendelseGrunnlagEntitet> grunnlagOpt1 = repositoryProvider.getFamilieHendelseRepository().hentAggregatHvisEksisterer(behandling.getId());

        if (grunnlagOpt1.isPresent()) {
            FamilieHendelseGrunnlagEntitet grunnlag = grunnlagOpt1.get();

            return mapHendelseGrunnlag(grunnlag, behandling);
        }
        return Optional.empty();
    }

    private Optional<FamiliehendelseDto> mapHendelseGrunnlag(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling) {
        if (grunnlag.getGjeldendeVersjon().getGjelderFødsel()) {
            return lagFodselDto(grunnlag, behandling);
        } else if (FamilieHendelseType.ADOPSJON.equals(grunnlag.getGjeldendeVersjon().getType())) {
            return lagAdopsjonDto(grunnlag);
        } else if (FamilieHendelseType.OMSORG.equals(grunnlag.getGjeldendeVersjon().getType())) {
            return lagOmsorgDto(grunnlag);
        }
        return Optional.empty();
    }

    private Optional<FamiliehendelseDto> lagFodselDto(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling) {
        AvklartDataFodselDto dto = new AvklartDataFodselDto();
        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(hendelse -> {
            mapTerminbekreftelse(hendelse, behandling, dto);
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

    private Optional<Long> finnUkerUtISvangerskapet(TerminbekreftelseEntitet terminbekreftelse, LocalDate originalVedtaksDato) {
        LocalDate termindato = terminbekreftelse.getTermindato();

        if (originalVedtaksDato != null && termindato != null) {
            LocalDate termindatoMinusUkerISvangerskap = termindato.minusWeeks(ANTALL_UKER_I_SVANGERSKAP);
            return Optional.of(ChronoUnit.WEEKS.between(termindatoMinusUkerISvangerskap, originalVedtaksDato) + 1);
        } else {
            return Optional.empty();
        }
    }

    private boolean harValgtSammeSomBekreftet(FamilieHendelseGrunnlagEntitet grunnlag) {
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

    private Optional<FamiliehendelseDto> lagAdopsjonDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        AvklartDataAdopsjonDto dto = new AvklartDataAdopsjonDto();

        Optional<FamilieHendelseEntitet> gjeldendeBekreftetVersjon = grunnlag.getGjeldendeBekreftetVersjon();
        if (gjeldendeBekreftetVersjon.isPresent()) {
            FamilieHendelseEntitet bekreftet = gjeldendeBekreftetVersjon.get();
            Map<Integer, LocalDate> fødselsdatoer = tilFødselsMap(bekreftet);

            mapAdopsjon(bekreftet, dto, fødselsdatoer);
        }
        return Optional.of(dto);
    }

    private Optional<FamiliehendelseDto> lagOmsorgDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        AvklartDataOmsorgDto dto = new AvklartDataOmsorgDto(SøknadType.fra(grunnlag.getGjeldendeVersjon()));
        grunnlag.getGjeldendeBekreftetVersjon().ifPresent(hendelse -> {
            mapOmsorg(hendelse, dto);
            dto.setAntallBarnTilBeregning(hendelse.getAntallBarn());
        });
        return Optional.of(dto);
    }
}
