package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatPeriodeDto;

@ApplicationScoped
public class SvangerskapspengerUttakResultatDtoTjeneste {
    private SvangerskapspengerUttakResultatRepository svpUttakResultatRepository;
    private ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public SvangerskapspengerUttakResultatDtoTjeneste() {
        //For CDI
    }

    @Inject
    public SvangerskapspengerUttakResultatDtoTjeneste(SvangerskapspengerUttakResultatRepository svpUttakResultatRepository,
                                                      ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.svpUttakResultatRepository = svpUttakResultatRepository;
        this.arbeidsgiverDtoTjeneste = arbeidsgiverDtoTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public Optional<SvangerskapspengerUttakResultatDto> mapFra(Behandling behandling) {
        Optional<SvangerskapspengerUttakResultatEntitet> optionalUttakResultat = svpUttakResultatRepository.hentHvisEksisterer(behandling.getId());
        if (optionalUttakResultat.isEmpty()) {
            return Optional.empty();
        }
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId()).map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer).orElse(Collections.emptyList());
        SvangerskapspengerUttakResultatEntitet uttakResultat = optionalUttakResultat.get();

        List<SvangerskapspengerUttakResultatArbeidsforholdDto> arbeidsforholdDtos = new ArrayList<>();
        for (SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforholdEntitet : uttakResultat.getUttaksResultatArbeidsforhold()) {

            List<SvangerskapspengerUttakResultatPeriodeDto> uttakResultatPeriodeDtos = arbeidsforholdEntitet.getPerioder().stream()
                .map(this::mapSvpUttakResultatPeriodeDto).collect(Collectors.toList());

            arbeidsforholdDtos.add(mapSvpUttakResultatArbeidsforholdDto(arbeidsforholdEntitet,
                sortSvpUttakResultatPeriodeDtoer(uttakResultatPeriodeDtos), overstyringer));
        }

        return Optional.of(new SvangerskapspengerUttakResultatDto(arbeidsforholdDtos));
    }

    private List<SvangerskapspengerUttakResultatPeriodeDto> sortSvpUttakResultatPeriodeDtoer(List<SvangerskapspengerUttakResultatPeriodeDto> uttakResultatPeriodeDtos) {
        return uttakResultatPeriodeDtos.stream()
            .sorted(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeDto::getFom))
            .collect(Collectors.toList());
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
                                                                                                  List<SvangerskapspengerUttakResultatPeriodeDto> periodeDtoer, List<ArbeidsforholdOverstyring> overstyringer) {
        return SvangerskapspengerUttakResultatArbeidsforholdDto.builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(perArbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak())
            .medArbeidsgiver(arbeidsgiverDtoTjeneste.mapFra(perArbeidsforhold.getArbeidsgiver(), overstyringer))
            .medArbeidType(perArbeidsforhold.getUttakArbeidType())
            .medPerioder(periodeDtoer)
            .build();
    }
}
