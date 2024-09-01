package no.nav.foreldrepenger.domene.uttak.svp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.svangerskapspenger.domene.søknad.AvklarteDatoer;

@ApplicationScoped
class AvklarteDatoerTjeneste {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private PersonopplysningerForUttak personopplysninger;

    @Inject
    AvklarteDatoerTjeneste(UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                           PersonopplysningerForUttak personopplysninger) {
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.personopplysninger = personopplysninger;
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
        var dødsdatoBrukerOptional = personopplysninger.søkersDødsdato(ref);

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
            .ifPresent(avklarteDatoerBuilder::medFørsteLovligeUttaksdato);

        return avklarteDatoerBuilder.build();
    }

    private Optional<LocalDate> finnMuligDødsdatoBarn(List<Barn> barna) {
        var levendeBarn = barna.stream().filter(barn -> barn.getDødsdato().isEmpty()).toList();
        if (levendeBarn.isEmpty()) {
            return barna.stream()
                .map(Barn::getDødsdato)
                .flatMap(Optional::stream)
                .max(LocalDate::compareTo);
        }
        return Optional.empty();
    }
}
