package no.nav.foreldrepenger.domene.uttak.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.svangerskapspenger.domene.søknad.AvklarteDatoer;
import no.nav.svangerskapspenger.domene.søknad.Opphold;
import no.nav.svangerskapspenger.tjeneste.fastsettuttak.SvpOppholdÅrsak;

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
            .ifPresent(uttakTidligst -> avklarteDatoerBuilder.medFørsteLovligeUttaksdato(uttakTidligst).medOpphold(finnOpphold(ref, uttakTidligst, svpGrunnlag.getGrunnlagEntitet().orElse(null) )));

        return avklarteDatoerBuilder.build();
    }

    private Optional<LocalDate> finnMuligDødsdatoBarn(List<Barn> barna) {
        var levendeBarn = barna.stream().filter(barn -> barn.getDødsdato().isEmpty()).toList();
        if (levendeBarn.isEmpty()) {
            return barna.stream()
                .map(Barn::getDødsdato)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(LocalDate::compareTo);
        }
        return Optional.empty();
    }

    private List<Opphold> finnOpphold(BehandlingReferanse behandlingRef, LocalDate utledetSkjæringstidspunkt, SvpGrunnlagEntitet svpGrunnlag) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingRef, utledetSkjæringstidspunkt);
        var opphold = new ArrayList<Opphold>();

        //henter ferier fra inntektsmelding meldt av arbeidsgiver
        inntektsmeldinger
            .stream()
            .flatMap(inntektsmelding -> inntektsmelding.getUtsettelsePerioder().stream())
            .filter(utsettelse -> UtsettelseÅrsak.FERIE.equals(utsettelse.getÅrsak()))
            .forEach(utsettelse -> opphold.addAll(Opphold.opprett(utsettelse.getPeriode().getFomDato(), utsettelse.getPeriode().getTomDato(), SvpOppholdÅrsak.FERIE)));

        if( svpGrunnlag != null) {
            //henter ferier- eller sykepenger-opphold registrert av saksbehandler
            svpGrunnlag.getGjeldendeVersjon().getTilretteleggingListe().stream()
                .map(SvpTilretteleggingEntitet::getAvklarteOpphold)
                .flatMap(Collection::stream)
                .forEach(avklartOpphold -> opphold.addAll(Opphold.opprett(avklartOpphold.getFom(), avklartOpphold.getTom(), mapOppholdÅrsak(avklartOpphold.getOppholdÅrsak()))));
        }
        return opphold;
    }

    private SvpOppholdÅrsak mapOppholdÅrsak(no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak oppholdÅrsak) {
        return switch (oppholdÅrsak) {
            case FERIE -> SvpOppholdÅrsak.FERIE;
            case SYKEPENGER -> SvpOppholdÅrsak.SYKEPENGER;
        };
    }
}
