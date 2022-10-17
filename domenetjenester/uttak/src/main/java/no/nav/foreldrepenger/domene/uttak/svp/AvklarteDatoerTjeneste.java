package no.nav.foreldrepenger.domene.uttak.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.svangerskapspenger.domene.søknad.AvklarteDatoer;
import no.nav.svangerskapspenger.domene.søknad.Ferie;

@ApplicationScoped
class AvklarteDatoerTjeneste {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private PersonopplysningerForUttak personopplysninger;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Inject
    AvklarteDatoerTjeneste(UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                           PersonopplysningerForUttak personopplysninger,
                           InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.personopplysninger = personopplysninger;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    AvklarteDatoerTjeneste() {
        //For CDI
    }

    public AvklarteDatoer finn(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var uttaksgrense = uttaksperiodegrenseRepository.hentHvisEksisterer(behandlingId);

        SvangerskapspengerGrunnlag svpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var termindato = svpGrunnlag.getFamilieHendelse().getTermindato().orElseThrow(() -> new IllegalStateException("Det skal alltid være termindato på svangerskapspenger søknad."));
        var fødselsdatoOptional = svpGrunnlag.getFamilieHendelse().getFødselsdato();
        var dødsdatoBarnOptional = finnMuligDødsdatoBarn(svpGrunnlag.getFamilieHendelse().getBarna());
        var dødsdatoBrukerOptional = personopplysninger.søkersDødsdatoGjeldendePåDato(ref, LocalDate.now());

        var medlemskapOpphørsdatoOptional = input.getMedlemskapOpphørsdato();
        var startdatoNesteSak = svpGrunnlag.nesteSakEntitet().map(NesteSakGrunnlagEntitet::getStartdato);

        var avklarteDatoerBuilder = new AvklarteDatoer.Builder();
        avklarteDatoerBuilder.medTermindato(termindato);
        fødselsdatoOptional.ifPresent(avklarteDatoerBuilder::medFødselsdato);
        dødsdatoBarnOptional.ifPresent(avklarteDatoerBuilder::medBarnetsDødsdato);
        dødsdatoBrukerOptional.ifPresent(avklarteDatoerBuilder::medBrukersDødsdato);
        medlemskapOpphørsdatoOptional.ifPresent(avklarteDatoerBuilder::medOpphørsdatoForMedlemskap);
        startdatoNesteSak.ifPresent(avklarteDatoerBuilder::medStartdatoNesteSak);

        uttaksgrense.map(Uttaksperiodegrense::getMottattDato).map(Søknadsfrister::tidligsteDatoDagytelse)
            .ifPresent(uttakTidligst -> avklarteDatoerBuilder.medFørsteLovligeUttaksdato(uttakTidligst).medFerie(finnFerier(ref, uttakTidligst)));

        return avklarteDatoerBuilder.build();
    }

    private Optional<LocalDate> finnMuligDødsdatoBarn(List<Barn> barna) {
        var levendeBarn = barna.stream().filter(barn -> barn.getDødsdato().isEmpty()).toList();
        if (levendeBarn.isEmpty()) {
            return barna.stream()
                .map(barn -> barn.getDødsdato())
                .filter(Optional::isPresent)
                .map(optionalDødsdato -> optionalDødsdato.get())
                .max(LocalDate::compareTo);
        }
        return Optional.empty();
    }

    private List<Ferie> finnFerier(BehandlingReferanse behandlingRef, LocalDate utledetSkjæringstidspunkt) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingRef, utledetSkjæringstidspunkt);
        var ferier = new ArrayList<Ferie>();
        inntektsmeldinger
            .stream()
            .flatMap(inntektsmelding -> inntektsmelding.getUtsettelsePerioder().stream())
            .filter(utsettelse -> UtsettelseÅrsak.FERIE.equals(utsettelse.getÅrsak()))
            .forEach(utsettelse -> ferier.addAll(Ferie.opprett(utsettelse.getPeriode().getFomDato(), utsettelse.getPeriode().getTomDato())));
        return ferier;
    }

}
