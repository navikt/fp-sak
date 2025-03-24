package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class VurderUttakDokumentasjonAksjonspunktUtleder {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private AktivitetskravDokumentasjonUtleder aktivitetskravDokumentasjonUtleder;

    @Inject
    public VurderUttakDokumentasjonAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                       AktivitetskravDokumentasjonUtleder aktivitetskravDokumentasjonUtleder) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.aktivitetskravDokumentasjonUtleder = aktivitetskravDokumentasjonUtleder;
    }

    VurderUttakDokumentasjonAksjonspunktUtleder() {
        //CDI
    }

    public boolean utledAksjonspunktFor(UttakInput input) {
        var dokumentasjonVurderingBehov = utledDokumentasjonVurderingBehov(input);
        return dokumentasjonVurderingBehov.stream().anyMatch(DokumentasjonVurderingBehov::måVurderes);
    }

    public List<DokumentasjonVurderingBehov> utledDokumentasjonVurderingBehov(UttakInput input) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        var yfPerioder = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .map(yfa -> yfa.getGjeldendeFordeling().getPerioder())
            .orElse(List.of());
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var aktivitetskravArbeidPerioder = fpGrunnlag.getAktivitetskravGrunnlag()
            .map(e -> e.getAktivitetskravPerioderMedArbeidEnitet()
                .map(AktivitetskravArbeidPerioderEntitet::getAktivitetskravArbeidPeriodeListe)
                .orElse(List.of()))
            .orElse(List.of());

        var aktivitetskravArbeidTimeline = new LocalDateTimeline<>(aktivitetskravArbeidPerioder.stream()
            .map(e -> new LocalDateSegment<>(e.getPeriode().getFomDato(), e.getPeriode().getTomDato(), Boolean.TRUE))
            .collect(Collectors.toSet()), StandardCombinators::alwaysTrueForMatch);
        var oppgittPeriodeTimeline = new LocalDateTimeline<>(
            yfPerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).collect(Collectors.toSet()));

        var combined = oppgittPeriodeTimeline.combine(aktivitetskravArbeidTimeline,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval,
                OppgittPeriodeBuilder.fraEksisterende(datoSegment.getValue()).medPeriode(datoInterval.getFomDato(), datoSegment.getTom()).build()),
            LocalDateTimeline.JoinStyle.LEFT_JOIN).stream().map(LocalDateSegment::getValue).toList();

        return combined.stream().map(p -> dokumentasjonVurderingBehov(p, input)).flatMap(Optional::stream).toList();
    }

    private Optional<DokumentasjonVurderingBehov> dokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode, UttakInput input) {
        var tidligereVurdering = oppgittPeriode.getDokumentasjonVurdering();
        var familiehendelse = finnGjeldendeFamiliehendelse(input);
        var behandlingReferanse = input.getBehandlingReferanse();
        var utsettelseDokBehov = UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, familiehendelse,
            finnPerioderMedPleiepengerInnleggelse(input));
        if (utsettelseDokBehov.isPresent()) {
            return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, utsettelseDokBehov.get(), tidligereVurdering));
        }
        var overføringDokBehov = OverføringDokumentasjonUtleder.utledBehov(oppgittPeriode);
        if (overføringDokBehov.isPresent()) {
            return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, overføringDokBehov.get(), tidligereVurdering));
        }
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingReferanse.behandlingId());
        var aktKravBehov = aktivitetskravDokumentasjonUtleder.utledBehov(input, oppgittPeriode, ytelseFordelingAggregat);
        if (aktKravBehov.isPresent()) {
            return utledAktivitetskravBehov(oppgittPeriode, input, tidligereVurdering, aktKravBehov.get());
        }
        var tidligOppstartFarBehov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, ytelseFordelingAggregat);
        return tidligOppstartFarBehov.map(behov -> new DokumentasjonVurderingBehov(oppgittPeriode, behov, tidligereVurdering));
    }

    private static Optional<DokumentasjonVurderingBehov> utledAktivitetskravBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                                  UttakInput input,
                                                                                  DokumentasjonVurdering tidligereVurdering,
                                                                                  DokumentasjonVurderingBehov.Behov aktKravBehov) {
        if (tidligereVurdering == null) {
            ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
            var aktivitetskravGrunnlag = ytelsespesifiktGrunnlag.getAktivitetskravGrunnlag();
            if (aktivitetskravGrunnlag.isPresent() && skalGjøreRegisterVurdering(oppgittPeriode, input.getYtelsespesifiktGrunnlag())) {
                var registerVurdering = vurderMorsArbeid(oppgittPeriode, aktivitetskravGrunnlag.get());
                return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, aktKravBehov, null, registerVurdering));
            }
        }
        return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, aktKravBehov, tidligereVurdering));
    }

    private static boolean skalGjøreRegisterVurdering(OppgittPeriodeEntitet oppgittPeriode, ForeldrepengerGrunnlag fpGrunnlag) {
        return oppgittPeriode.erAktivitetskravMedMorArbeid() && fpGrunnlag.isMottattMorsArbeidDokument();
    }

    private static RegisterVurdering vurderMorsArbeid(OppgittPeriodeEntitet oppgittPeriode,
                                                      AktivitetskravGrunnlagEntitet aktivitetskravGrunnlag) {
        return aktivitetskravGrunnlag.mor75StillingOgIngenPermisjoner(
            oppgittPeriode.getTidsperiode()) ? RegisterVurdering.MORS_AKTIVITET_GODKJENT : RegisterVurdering.MORS_AKTIVITET_IKKE_GODKJENT;
    }

    private static List<PleiepengerInnleggelseEntitet> finnPerioderMedPleiepengerInnleggelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var pleiepengerGrunnlag = ytelsespesifiktGrunnlag.getPleiepengerGrunnlag();
        if (pleiepengerGrunnlag.isPresent()) {
            var perioderMedInnleggelse = pleiepengerGrunnlag.get().getPerioderMedInnleggelse();
            if (perioderMedInnleggelse.isPresent()) {
                return perioderMedInnleggelse.get().getInnleggelser();
            }
        }
        return List.of();
    }

    private static LocalDate finnGjeldendeFamiliehendelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeFamilieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        return gjeldendeFamilieHendelse.getFamilieHendelseDato();
    }
}
