package no.nav.foreldrepenger.domene.registerinnhenting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class MorsAktivitetInnhenter {
    private static final Logger LOG = LoggerFactory.getLogger(MorsAktivitetInnhenter.class);
    private YtelsesFordelingRepository ytelseFordelingTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste;
    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;

    public MorsAktivitetInnhenter() {
        //CDI
    }

    @Inject
    public MorsAktivitetInnhenter(YtelsesFordelingRepository ytelseFordelingTjeneste,
                                  RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                  PersonopplysningTjeneste personopplysningTjeneste,
                                  ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste,
                                  AktivitetskravArbeidRepository aktivitetskravArbeidRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.abakusArbeidsforholdTjeneste = abakusArbeidsforholdTjeneste;
        this.aktivitetskravArbeidRepository = aktivitetskravArbeidRepository;
    }

    public void innhentMorsAktivitet(Behandling behandling) {
        var behandlingId = behandling.getId();
        var ytelseaggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var perioderAktivitetskravArbeid = hentPerioderMedAktivitetskrav(ytelseaggregat, behandling.getFagsakYtelseType());
        var morsAktivitetsGrunnlag = aktivitetskravArbeidRepository.hentGrunnlag(behandlingId);

        if (perioderAktivitetskravArbeid.isEmpty() && morsAktivitetsGrunnlag.isEmpty()) {
            return;
        }

        var annenPartAktørId = hentAnnenPartAktørId(BehandlingReferanse.fra(behandling));
        if (annenPartAktørId == null) {
            LOG.info("MorsAktivitetInnhenter: Finner ingen annen part for behandling med aktivitetskrav arbeid for behandlingId: {}", behandlingId);
            return;
        }
        var morAktvitet = finnMorsAktivitet(behandling, perioderAktivitetskravArbeid, annenPartAktørId, morsAktivitetsGrunnlag);
        if (morAktvitet == null) {
            LOG.info("MorsAktivitetInnhenter: Finner ingen aktivitet på mor for behandlingId: {}", behandlingId);
            return;
        }
        aktivitetskravArbeidRepository.lagreAktivitetskravArbeidPerioder(behandlingId, morAktvitet.perioderEntitet(), morAktvitet.fraDato(), morAktvitet.tilDato());
    }

    public MorAktivitet finnMorsAktivitet(Behandling behandling,
                                          List<OppgittPeriodeEntitet> perioderAktivitetskravArbeid,
                                          AktørId annenPartAktørId,
                                          Optional<AktivitetskravGrunnlagEntitet> gjeldendeAktivitetsgrunnlag) {

        var nyMinsteAktvitetskravDato = minsteFraDatoMinusIntervall(perioderAktivitetskravArbeid).orElse(Tid.TIDENES_ENDE);
        var nyHøyesteAktivitetskravDato = hentHøyesteFraDatoPlusIntervall(perioderAktivitetskravArbeid).orElse(null);

        if (nyHøyesteAktivitetskravDato == null && gjeldendeAktivitetsgrunnlag.isEmpty()) {
            return null;
        }

        var nyGrunnlagFraDato = utledNyGrunnlagFraDato(nyMinsteAktvitetskravDato, gjeldendeAktivitetsgrunnlag.orElse(null));
        var nyGrunnlagTilDato = nyHøyesteAktivitetskravDato != null ? nyHøyesteAktivitetskravDato : gjeldendeAktivitetsgrunnlag.get().getPeriode().getFomDato();

        LOG.info("MorsAktivitetInnhenter: Henter mors aktivitet for behandlingId: {} for periode:{} - {}", behandling.getId(), nyGrunnlagFraDato, nyHøyesteAktivitetskravDato);
        var arbeidsforholdInfo = abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(annenPartAktørId, nyGrunnlagFraDato, nyGrunnlagTilDato,
            behandling.getFagsakYtelseType());

        if (arbeidsforholdInfo.isEmpty()) {
            return null;
        }

        var mapAvOrgnrOgAvtaler = arbeidsforholdInfo.stream().collect(Collectors.groupingBy(this::aktivitetskravNøkkel));
        var perioderBuilder = lagAktivitetskravPerioderBuilder(mapAvOrgnrOgAvtaler, nyGrunnlagFraDato, nyHøyesteAktivitetskravDato);

        return new MorAktivitet(nyGrunnlagFraDato, nyHøyesteAktivitetskravDato, perioderBuilder);
    }

    private LocalDate utledNyGrunnlagFraDato(LocalDate nyMinsteAktvitetskravDato, AktivitetskravGrunnlagEntitet gjeldendeAktivitetsgrunnlag) {
        var fraDatoGjeldendeGrunnlag = gjeldendeAktivitetsgrunnlag != null ? gjeldendeAktivitetsgrunnlag.getPeriode().getFomDato() : Tid.TIDENES_ENDE;

        if (nyMinsteAktvitetskravDato.isBefore(fraDatoGjeldendeGrunnlag)) {
            return nyMinsteAktvitetskravDato;
        } else if (fraDatoGjeldendeGrunnlag.isBefore(nyMinsteAktvitetskravDato)) {
            return fraDatoGjeldendeGrunnlag;
        } else {
            return fraDatoGjeldendeGrunnlag;
        }
    }

    public record MorAktivitet(LocalDate fraDato, LocalDate tilDato, AktivitetskravArbeidPerioderEntitet perioderEntitet) {
    }

    private static LocalDateTimeline<BigDecimal> permisjonTidslinje(List<ArbeidsforholdMedPermisjon> arbeidsforholdInfo) {
        //sørger for at hull blir 0% og at de permisjonene som overlapper  per arbeidsforhold summeres
        return new LocalDateTimeline<>(arbeidsforholdInfo.stream()
            .flatMap(a -> a.permisjoner().stream())
            .map(permisjon -> new LocalDateSegment<>(permisjon.periode().getFomDato(), permisjon.periode().getTomDato(), permisjon.prosent()))
            .toList(), bigDesimalSum());
    }

    private static LocalDateTimeline<AktivitetskravPeriodeGrunnlag> lagTidslinjeMed0(LocalDate fraDato, LocalDate tilDato) {
        return new LocalDateTimeline<>(fraDato, tilDato, new AktivitetskravPeriodeGrunnlag(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, BigDecimal> bigDesimalSum() {
        return StandardCombinators::sum;
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, AktivitetskravPeriodeGrunnlag> bigDecimalTilAktivitetskravVurderingGrunnlagCombinator() {
        return (localDateInterval, stillingsprosent, permisjonsprosent) -> new LocalDateSegment<>(localDateInterval,
            new AktivitetskravPeriodeGrunnlag(stillingsprosent != null ? stillingsprosent.getValue() : BigDecimal.ZERO,
                permisjonsprosent != null ? permisjonsprosent.getValue() : BigDecimal.ZERO));
    }

    private static Optional<LocalDate> hentHøyesteFraDatoPlusIntervall(List<OppgittPeriodeEntitet> aktuellePerioder) {
        return aktuellePerioder.stream()
            .map(OppgittPeriodeEntitet::getTom)
            .max(LocalDate::compareTo)
            .map(maxDato -> maxDato.plusWeeks(2));
    }

    private static Optional<LocalDate> minsteFraDatoMinusIntervall(List<OppgittPeriodeEntitet> aktuellePerioder) {
        return aktuellePerioder.stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(LocalDate::compareTo)
            .map(minDato -> minDato.minusWeeks(2));
    }

    private static List<OppgittPeriodeEntitet> hentPerioderMedAktivitetskrav(YtelseFordelingAggregat ytelseaggregat, FagsakYtelseType ytelseType) {
        return ytelseaggregat.getGjeldendeFordeling()
            .getPerioder()
            .stream()
            .filter(p -> FagsakYtelseType.FORELDREPENGER.equals(ytelseType) && UttakPeriodeType.FELLESPERIODE.equals(p.getPeriodeType()) && MorsAktivitet.ARBEID.equals(p.getMorsAktivitet()))
            .toList();
    }

    private AktivitetskravArbeidPerioderEntitet lagAktivitetskravPerioderBuilder(Map<String, List<ArbeidsforholdMedPermisjon>> mapAvOrgnrOgAvtaler,
                                                                                         LocalDate fraDato,
                                                                                         LocalDate tilDato) {
        List<AktivitetskravArbeidPeriodeEntitet.Builder> builderListe = new ArrayList<>();
        mapAvOrgnrOgAvtaler.forEach((key, value) -> {
            var stillingsprosentTidslinje = stillingsprosentTidslinje(value);
            var permisjonProsentTidslinje = permisjonTidslinje(value);
            var grunnlagTidslinje = stillingsprosentTidslinje.crossJoin(permisjonProsentTidslinje,
                    bigDecimalTilAktivitetskravVurderingGrunnlagCombinator())
                .crossJoin(lagTidslinjeMed0(fraDato, tilDato), StandardCombinators::coalesceLeftHandSide)
                .compress();

            builderListe.addAll(grunnlagTidslinje.stream().map(segment -> opprettBuilder(segment, key)).toList());
        });

        var perioderBuilder = new AktivitetskravArbeidPerioderEntitet.Builder();

        builderListe.forEach(perioderBuilder::leggTil);

        return perioderBuilder.build();
    }

    private AktivitetskravArbeidPeriodeEntitet.Builder opprettBuilder(LocalDateSegment<AktivitetskravPeriodeGrunnlag> segment, String orgNummer) {
        return new AktivitetskravArbeidPeriodeEntitet.Builder().medOrgNummer(orgNummer)
            .medPeriode(segment.getFom(), segment.getTom())
            .medSumStillingsprosent(segment.getValue().sumStillingsprosent())
            .medSumPermisjonsprosent(segment.getValue().SumPermisjonsprosent());
    }

    private String aktivitetskravNøkkel(ArbeidsforholdMedPermisjon arbeidsforholdInfo) {
        return arbeidsforholdInfo.arbeidsgiver().getOrgnr();
    }

    private LocalDateTimeline<BigDecimal> stillingsprosentTidslinje(List<ArbeidsforholdMedPermisjon> arbeidsforholdInfo) {
        return new LocalDateTimeline<>(arbeidsforholdInfo.stream()
            .flatMap(a -> a.aktivitetsavtaler().stream())
            .map(aktivitetAvtale -> new LocalDateSegment<>(aktivitetAvtale.periode().getFomDato(), aktivitetAvtale.periode().getTomDato(),
                aktivitetAvtale.stillingsprosent()))
            .toList(), bigDesimalSum());
    }

    private AktørId hentAnnenPartAktørId(BehandlingReferanse behandlingReferanse) {
        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(behandlingReferanse.saksnummer())
            .map(Behandling::getAktørId)
            .orElseGet(() -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(behandlingReferanse.behandlingId()).orElse(null));
    }

    private record AktivitetskravPeriodeGrunnlag(BigDecimal sumStillingsprosent, BigDecimal SumPermisjonsprosent) {
    }
}
