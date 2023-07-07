package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatPeriodeDto;

@ApplicationScoped
public class SvangerskapspengerUttakResultatDtoTjeneste {
    private SvangerskapspengerUttakResultatRepository svpUttakResultatRepository;

    public SvangerskapspengerUttakResultatDtoTjeneste() {
        //For CDI
    }

    @Inject
    public SvangerskapspengerUttakResultatDtoTjeneste(SvangerskapspengerUttakResultatRepository svpUttakResultatRepository) {
        this.svpUttakResultatRepository = svpUttakResultatRepository;
    }

    public Optional<SvangerskapspengerUttakResultatDto> mapFra(Behandling behandling) {
        var optionalUttakResultat = svpUttakResultatRepository.hentHvisEksisterer(behandling.getId());
        if (optionalUttakResultat.isEmpty()) {
            return Optional.empty();
        }
        var uttakResultat = optionalUttakResultat.get();

        List<SvangerskapspengerUttakResultatArbeidsforholdDto> arbeidsforholdDtos = new ArrayList<>();
        for (var arbeidsforholdEntitet : uttakResultat.getUttaksResultatArbeidsforhold()) {

            var uttakResultatPeriodeDtos = arbeidsforholdEntitet.getPerioder().stream()
                .map(this::mapSvpUttakResultatPeriodeDto).toList();

            arbeidsforholdDtos.add(mapSvpUttakResultatArbeidsforholdDto(arbeidsforholdEntitet,
                sortSvpUttakResultatPeriodeDtoer(uttakResultatPeriodeDtos)));
        }

        return Optional.of(new SvangerskapspengerUttakResultatDto(arbeidsforholdDtos));
    }

    private List<SvangerskapspengerUttakResultatPeriodeDto> sortSvpUttakResultatPeriodeDtoer(List<SvangerskapspengerUttakResultatPeriodeDto> uttakResultatPeriodeDtos) {
        return uttakResultatPeriodeDtos.stream()
            .sorted(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeDto::getFom))
            .toList();
    }

    private SvangerskapspengerUttakResultatPeriodeDto mapSvpUttakResultatPeriodeDto(
        SvangerskapspengerUttakResultatPeriodeEntitet svangerskapspengerUttakResultatPeriodeEntitet) {
        return SvangerskapspengerUttakResultatPeriodeDto.builder()
            .medUtbetalingsgrad(svangerskapspengerUttakResultatPeriodeEntitet.getUtbetalingsgrad())
            .medPeriodeResultatType(svangerskapspengerUttakResultatPeriodeEntitet.getPeriodeResultatType())
            .medPeriodeIkkeOppfyltÅrsak(svangerskapspengerUttakResultatPeriodeEntitet.getPeriodeIkkeOppfyltÅrsak())
            .medfom(svangerskapspengerUttakResultatPeriodeEntitet.getFom())
            .medTom(svangerskapspengerUttakResultatPeriodeEntitet.getTom())
            .build();
    }

    private SvangerskapspengerUttakResultatArbeidsforholdDto mapSvpUttakResultatArbeidsforholdDto(SvangerskapspengerUttakResultatArbeidsforholdEntitet perArbeidsforhold,
                                                                                                  List<SvangerskapspengerUttakResultatPeriodeDto> periodeDtoer) {
        return SvangerskapspengerUttakResultatArbeidsforholdDto.builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(perArbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak())
            .medArbeidsgiver(perArbeidsforhold.getArbeidsgiver() != null ? perArbeidsforhold.getArbeidsgiver().getIdentifikator() : null)
            .medArbeidType(perArbeidsforhold.getUttakArbeidType())
            .medPerioder(periodeDtoer)
            .build();
    }
}
