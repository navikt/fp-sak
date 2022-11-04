package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.ALENEOMSORG_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.BARE_SØKER_RETT_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.HV_OVELSE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_ANNEN_FORELDER_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_BARN_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_SØKER_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_DOKUMENTERT_AKTIVITET;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_DOKUMENTERT_IKKE_AKTIVITET;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.NAV_TILTAK_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_SØKER_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.FRILANS;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Dokumentasjon;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppgittPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.PeriodeUtenOmsorg;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.SamtidigUttaksprosent;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Søknad;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Søknadstype;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

@ApplicationScoped
public class SøknadGrunnlagBygger {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public SøknadGrunnlagBygger(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    SøknadGrunnlagBygger() {
        // CDI
    }

    public Søknad.Builder byggGrunnlag(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        return new Søknad.Builder()
            .type(type(input.getYtelsespesifiktGrunnlag()))
            .dokumentasjon(dokumentasjon(ytelseFordelingAggregat))
            .oppgittePerioder(oppgittePerioder(input, ytelseFordelingAggregat))
            .mottattTidspunkt(input.getSøknadOpprettetTidspunkt());
    }

    private List<OppgittPeriode> oppgittePerioder(UttakInput input, YtelseFordelingAggregat ytelseFordelingAggregat) {
        var oppgittePerioder = splitPåDokumentasjon(ytelseFordelingAggregat, input.getBehandlingReferanse().relasjonRolle());
        validerIkkeOverlappOppgittePerioder(oppgittePerioder);

        return oppgittePerioder.stream()
            .map(op -> byggOppgittperiode(op, new UttakYrkesaktiviteter(input).tilAktivitetIdentifikatorer()))
            .collect(Collectors.toList());
    }

    private List<OppgittPeriodeEntitet> splitPåDokumentasjon(YtelseFordelingAggregat ytelseFordelingAggregat, RelasjonsRolleType relasjonsRolleType) {
        var gjeldendePerioder = ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        var opTimeline = new LocalDateTimeline<>(gjeldendePerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());
        var dokPerioder = ytelseFordelingAggregat.getPerioderUttakDokumentasjon().map(d -> d.getPerioder()).orElse(List.of());
        var dokTimeline = new LocalDateTimeline<>(
            dokPerioder.stream().map(dp -> new LocalDateSegment<>(dp.getPeriode().getFomDato(), dp.getPeriode().getTomDato(), dp)).toList());
        var aktKravPerioder = ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder().map(d -> d.getPerioder()).orElse(List.of());
        var aktKravTimeline = new LocalDateTimeline<>(aktKravPerioder.stream()
            .map(akp -> new LocalDateSegment<>(akp.getTidsperiode().getFomDato(), akp.getTidsperiode().getTomDato(), akp)).toList());

        return opTimeline.combine(dokTimeline, (interval, op, dok) -> {
            var eksisterendeDokVurdering = op.getValue().getDokumentasjonVurdering();
            var dokumentasjonVurdering = eksisterendeDokVurdering != null || dok == null ? eksisterendeDokVurdering : utledDokumentasjonVurdering(dok.getValue());
            var nyOp = OppgittPeriodeBuilder.fraEksisterende(op.getValue())
                .medPeriode(interval.getFomDato(), interval.getTomDato())
                .medDokumentasjonVurdering(dokumentasjonVurdering)
                .build();
            return new LocalDateSegment<>(interval, nyOp);
        }, JoinStyle.LEFT_JOIN).combine(aktKravTimeline, (interval, op, aktkravDok) -> {
            var eksisterendeDokVurdering = op.getValue().getDokumentasjonVurdering();
            var dokumentasjonVurdering = eksisterendeDokVurdering != null || aktkravDok == null ? eksisterendeDokVurdering : utledDokumentasjonVurdering(aktkravDok.getValue());
            var nyOp = OppgittPeriodeBuilder.fraEksisterende(op.getValue())
                .medPeriode(interval.getFomDato(), interval.getTomDato())
                .medDokumentasjonVurdering(dokumentasjonVurdering)
                .build();
            return new LocalDateSegment<>(interval, nyOp);
        }, JoinStyle.LEFT_JOIN).toSegments().stream()
            .map(s -> {
                var op = s.getValue();
                if (RelasjonsRolleType.erFarEllerMedmor(relasjonsRolleType) && op.getDokumentasjonVurdering() == null
                    && tidligOppstartFedrekvoteAvklart(op)) {
                    return OppgittPeriodeBuilder.fraEksisterende(op)
                        .medDokumentasjonVurdering(DokumentasjonVurdering.TIDLIG_OPPSTART_FEDREKVOTE_DOKUMENTERT)
                        .build();
                }
                return op;
            })
            .toList();
    }

    private boolean tidligOppstartFedrekvoteAvklart(OppgittPeriodeEntitet op) {
        return op.getPeriodeType().equals(UttakPeriodeType.FEDREKVOTE)
            && Set.of(UttakPeriodeVurderingType.PERIODE_OK, UttakPeriodeVurderingType.PERIODE_OK_ENDRET).contains(op.getPeriodeVurderingType());
    }

    private DokumentasjonVurdering utledDokumentasjonVurdering(AktivitetskravPeriodeEntitet aktKravPeriode) {
        return switch (aktKravPeriode.getAvklaring()) {
            case I_AKTIVITET -> MORS_AKTIVITET_DOKUMENTERT_AKTIVITET;
            case IKKE_I_AKTIVITET_IKKE_DOKUMENTERT -> MORS_AKTIVITET_IKKE_DOKUMENTERT;
            case IKKE_I_AKTIVITET_DOKUMENTERT -> MORS_AKTIVITET_DOKUMENTERT_IKKE_AKTIVITET;
        };
    }

    private DokumentasjonVurdering utledDokumentasjonVurdering(PeriodeUttakDokumentasjonEntitet dok) {
        return switch (dok.getDokumentasjonType()) {
            case SYK_SØKER -> SYKDOM_SØKER_DOKUMENTERT;
            case INNLAGT_SØKER -> INNLEGGELSE_SØKER_DOKUMENTERT;
            case INNLAGT_BARN -> INNLEGGELSE_BARN_DOKUMENTERT;
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDRE -> INNLEGGELSE_ANNEN_FORELDER_DOKUMENTERT;
            case SYKDOM_ANNEN_FORELDER -> SYKDOM_ANNEN_FORELDER_DOKUMENTERT;
            case IKKE_RETT_ANNEN_FORELDER -> BARE_SØKER_RETT_DOKUMENTERT;
            case ALENEOMSORG_OVERFØRING -> ALENEOMSORG_DOKUMENTERT;
            case HV_OVELSE -> HV_OVELSE_DOKUMENTERT;
            case NAV_TILTAK -> NAV_TILTAK_DOKUMENTERT;
            default -> throw new IllegalArgumentException("Ikke gyldig doktype " + dok.getDokumentasjonType());
        };
    }

    private OppgittPeriode byggOppgittperiode(OppgittPeriodeEntitet oppgittPeriode,
                                              Set<AktivitetIdentifikator> aktiviteter) {
        var oppgittPeriodeType = oppgittPeriode.getPeriodeType();
        var stønadskontotype = map(oppgittPeriodeType);

        final OppgittPeriode periode;
        if (UttakPeriodeType.STØNADSPERIODETYPER.contains(oppgittPeriodeType)) {
            if (oppgittPeriode.isUtsettelse()) {
                periode = byggUtsettelseperiode(oppgittPeriode);
            } else if (oppgittPeriode.isOverføring()) {
                periode = byggOverføringPeriode(oppgittPeriode, stønadskontotype);
            } else {
                periode = byggStønadsperiode(oppgittPeriode, stønadskontotype, aktiviteter);
            }
        } else if (UttakPeriodeType.ANNET.equals(oppgittPeriodeType)) {
            periode = byggTilOppholdPeriode(oppgittPeriode);
        } else {
            throw new IllegalArgumentException("Ikke-støttet UttakPeriodeType: " + oppgittPeriodeType);
        }
        return periode;
    }

    private static OppgittPeriode byggStønadsperiode(OppgittPeriodeEntitet oppgittPeriode,
                                                     Stønadskontotype stønadskontotype,
                                                     Set<AktivitetIdentifikator> aktiviter) {

        if (oppgittPeriode.isGradert()) {
            return byggGradertPeriode(oppgittPeriode, stønadskontotype, aktiviter);
        }
        return OppgittPeriode.forVanligPeriode(stønadskontotype, oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            samtidigUttaksprosent(oppgittPeriode), oppgittPeriode.isFlerbarnsdager(),
            oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode), map(oppgittPeriode.getMorsAktivitet()),
            map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static LocalDate tidligstMottattDato(OppgittPeriodeEntitet oppgittPeriode) {
        //Historiske behandlinger har ikke satt tidligstMottattDato - 22.4.2021
        return oppgittPeriode.getTidligstMottattDato().orElse(oppgittPeriode.getMottattDato());
    }

    private static SamtidigUttaksprosent samtidigUttaksprosent(OppgittPeriodeEntitet oppgittPeriode) {
        if (oppgittPeriode.getSamtidigUttaksprosent() == null) {
            return null;
        }
        //Ligger søknader fra tidligere i prod med samtidig uttak og 0%. Tolker som 100%
        if (oppgittPeriode.getSamtidigUttaksprosent().equals(no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent.ZERO)) {
            return SamtidigUttaksprosent.HUNDRED;
        }
        return new SamtidigUttaksprosent(oppgittPeriode.getSamtidigUttaksprosent().decimalValue());
    }

    private static OppgittPeriode byggGradertPeriode(OppgittPeriodeEntitet oppgittPeriode,
                                                     Stønadskontotype stønadskontotype,
                                                     Set<AktivitetIdentifikator> aktiviter) {
        var gradertAktivitet = finnGraderteAktiviteter(oppgittPeriode, aktiviter);
        if (gradertAktivitet.isEmpty()) {
            throw new IllegalStateException("Forventer minst en gradert aktivitet ved gradering i søknadsperioden");
        }

        return OppgittPeriode.forGradering(stønadskontotype, oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            oppgittPeriode.getArbeidsprosent(), samtidigUttaksprosent(oppgittPeriode), oppgittPeriode.isFlerbarnsdager(),
            gradertAktivitet, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode),
            map(oppgittPeriode.getMorsAktivitet()), UttakEnumMapper.map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static Set<AktivitetIdentifikator> finnGraderteAktiviteter(OppgittPeriodeEntitet oppgittPeriode, Set<AktivitetIdentifikator> aktiviter) {
        if (oppgittPeriode.getGraderingAktivitetType() == FRILANS) {
            return aktivieterMedType(aktiviter, AktivitetType.FRILANS);
        }
        if (oppgittPeriode.getGraderingAktivitetType() == SELVSTENDIG_NÆRINGSDRIVENDE) {
            return aktivieterMedType(aktiviter, AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
        }
        return aktivieterMedType(aktiviter, AktivitetType.ARBEID).stream()
            .filter(aktivitetIdentifikator -> Objects.equals(oppgittPeriode.getArbeidsgiver().getIdentifikator(),
                Optional.ofNullable(aktivitetIdentifikator.getArbeidsgiverIdentifikator()).map(ai -> ai.value()).orElse(null)))
            .collect(Collectors.toSet());
    }

    private static Set<AktivitetIdentifikator> aktivieterMedType(Set<AktivitetIdentifikator> aktiviter, AktivitetType aktivitetType) {
        return aktiviter.stream().filter(aktivitetIdentifikator -> aktivitetIdentifikator.getAktivitetType().equals(aktivitetType)).collect(Collectors.toSet());
    }

    private static OppgittPeriode byggOverføringPeriode(OppgittPeriodeEntitet oppgittPeriode,
                                                        Stønadskontotype stønadskontotype) {
        var overføringÅrsak = map((OverføringÅrsak) oppgittPeriode.getÅrsak());

        return OppgittPeriode.forOverføring(stønadskontotype, oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            overføringÅrsak, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode), map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static OppgittPeriode byggUtsettelseperiode(OppgittPeriodeEntitet oppgittPeriode) {
        var utsettelseÅrsak = map((UtsettelseÅrsak) oppgittPeriode.getÅrsak());

        return OppgittPeriode.forUtsettelse(oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            utsettelseÅrsak, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode),
            map(oppgittPeriode.getMorsAktivitet()), map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static OppgittPeriode byggTilOppholdPeriode(OppgittPeriodeEntitet oppgittPeriode) {
        if (oppgittPeriode.isOpphold()) {
            var årsak = oppgittPeriode.getÅrsak();
            var oppholdÅrsak = (OppholdÅrsak) årsak;
            var mappedÅrsak = map(oppholdÅrsak);
            return OppgittPeriode.forOpphold(oppgittPeriode.getFom(), oppgittPeriode.getTom(),
                mappedÅrsak, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode));
        }
        throw new IllegalArgumentException("Ikke-støttet årsakstype: " + oppgittPeriode.getÅrsak());
    }

    private static void validerIkkeOverlappOppgittePerioder(List<OppgittPeriodeEntitet> søknadPerioder) {
        var size = søknadPerioder.size();
        for (var i = 0; i < size; i++) {
            var periode1 = søknadPerioder.get(i);

            var p1 = new SimpleLocalDateInterval(periode1.getFom(), periode1.getTom());
            for (var j = i + 1; j < size; j++) {
                var periode2 = søknadPerioder.get(j);
                var p2 = new SimpleLocalDateInterval(periode2.getFom(), periode2.getTom());
                if (p1.overlapper(p2)) {
                    throw new IllegalStateException("Støtter ikke å ha overlappende søknadsperioder, men fikk overlapp mellom periodene " + p1 + " og " + p2);
                }
            }
        }
    }

    private static Søknadstype type(ForeldrepengerGrunnlag fpGrunnlag) {
        var hendelser = fpGrunnlag.getFamilieHendelser();
        if (hendelser.gjelderTerminFødsel() && hendelser.erSøktTermin()) {
            return Søknadstype.TERMIN;
        }
        if (hendelser.gjelderTerminFødsel()) {
            return Søknadstype.FØDSEL;
        }
        return Søknadstype.ADOPSJON;
    }

    private Dokumentasjon.Builder dokumentasjon(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var builder = new Dokumentasjon.Builder();
        ytelseFordelingAggregat.getPerioderUtenOmsorg().ifPresent(puo -> leggTilPerioderUtenOmsorg(builder, puo.getPerioder()));
        return builder;
    }

    private void leggTilPerioderUtenOmsorg(Dokumentasjon.Builder builder, List<PeriodeUtenOmsorgEntitet> perioder) {
        for (var periodeUtenOmsorg : perioder) {
            builder.periodeUtenOmsorg(new PeriodeUtenOmsorg(periodeUtenOmsorg.getPeriode().getFomDato(),
                periodeUtenOmsorg.getPeriode().getTomDato()));
        }
    }
}
