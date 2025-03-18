package no.nav.foreldrepenger.domene.registerinnhenting;

import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType.ANNEN_PERMISJON;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType.PERMITTERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType.UDEFINERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType.UTDANNING;

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
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
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
        if (!FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalStateException("Kan ikke hente mors aktivitet for behandling med fagsakytelse: " + behandling.getFagsakYtelseType());
        }
        if (!behandling.getRelasjonsRolleType().erFarEllerMedMor()) {
            throw new IllegalStateException("Kan ikke hente mors aktivitet for behandling med relasjonstype: " + behandling.getRelasjonsRolleType());
        }
        var behandlingId = behandling.getId();
        var ytelseaggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var perioderAktivitetskravArbeid = hentPerioderMedAktivitetskrav(ytelseaggregat);
        if (perioderAktivitetskravArbeid.isEmpty()) {
            LOG.info("Ingen perioder med aktivitetskrav arbeid for behandlingId: {}", behandlingId);
            return;
        }

        var annenPartAktørId = hentAnnenPartAktørId(BehandlingReferanse.fra(behandling));
        if (annenPartAktørId == null) {
            LOG.info("MorsAktivitetInnhenter: Finner ingen annen part for behandling med aktivitetskrav arbeid for behandlingId: {}", behandlingId);
            return;
        }
        var morsAktivitetsGrunnlag = aktivitetskravArbeidRepository.hentGrunnlag(behandlingId);
        var morAktvitetOpt = finnMorsAktivitet(behandling, perioderAktivitetskravArbeid, annenPartAktørId, morsAktivitetsGrunnlag);

        morAktvitetOpt.ifPresent(
            morAktivitet -> aktivitetskravArbeidRepository.lagreAktivitetskravArbeidPerioder(behandlingId, morAktivitet.perioderEntitet(),
                morAktivitet.fraDato(), morAktivitet.tilDato()));
    }

    Optional<MorAktivitet> finnMorsAktivitet(Behandling behandling,
                                             List<OppgittPeriodeEntitet> perioderAktivitetskravArbeid,
                                             AktørId annenPartAktørId,
                                             Optional<AktivitetskravGrunnlagEntitet> gjeldendeAktivitetsgrunnlag) {

        var nyMinsteAktvitetskravDato = minsteFraDatoMinusIntervall(perioderAktivitetskravArbeid);
        var nyHøyesteAktivitetskravDato = hentHøyesteFraDatoPlusIntervall(perioderAktivitetskravArbeid);

        var fraDatoGjeldendeGrunnlag = gjeldendeAktivitetsgrunnlag.map(grunnlag -> grunnlag.getPeriode().getFomDato()).orElse(Tid.TIDENES_ENDE);
        var tilDatoGjeldendeGrunnlag = gjeldendeAktivitetsgrunnlag.map(grunnlag -> grunnlag.getPeriode().getTomDato()).orElse(Tid.TIDENES_BEGYNNELSE);

        var nyGrunnlagFraDato = nyMinsteAktvitetskravDato.isBefore(fraDatoGjeldendeGrunnlag) ? nyMinsteAktvitetskravDato : fraDatoGjeldendeGrunnlag;
        var nyGrunnlagTilDato = nyHøyesteAktivitetskravDato.isAfter(
            tilDatoGjeldendeGrunnlag) ? nyHøyesteAktivitetskravDato : tilDatoGjeldendeGrunnlag;

        LOG.info("MorsAktivitetInnhenter: Henter mors aktivitet for behandlingId: {} for periode:{} - {}", behandling.getId(), nyGrunnlagFraDato,
            nyGrunnlagTilDato);
        var arbeidsforholdInfo = abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(annenPartAktørId, nyGrunnlagFraDato,
            nyGrunnlagTilDato, behandling.getFagsakYtelseType());

        if (arbeidsforholdInfo.isEmpty()) {
            LOG.info("MorsAktivitetInnhenter: Finner ingen aktivitet på mor for behandlingId: {}", behandling.getId());
            return Optional.empty();
        }

        var mapAvOrgnrOgAvtaler = arbeidsforholdInfo.stream().collect(Collectors.groupingBy(this::aktivitetskravNøkkel));
        var perioderBuilder = lagAktivitetskravPerioderBuilder(mapAvOrgnrOgAvtaler, nyGrunnlagFraDato, nyHøyesteAktivitetskravDato);

        return Optional.of(new MorAktivitet(nyGrunnlagFraDato, nyGrunnlagTilDato, perioderBuilder));
    }

    public record MorAktivitet(LocalDate fraDato, LocalDate tilDato, AktivitetskravArbeidPerioderEntitet perioderEntitet) {
    }

    private static LocalDateTimeline<Permisjon> permisjonTidslinje(List<ArbeidsforholdMedPermisjon> arbeidsforholdInfo) {
        //sørger for at hull blir 0% og at de permisjonene som overlapper  per arbeidsforhold summeres
        return new LocalDateTimeline<>(arbeidsforholdInfo.stream().flatMap(a -> a.permisjoner().stream()).map(permisjon -> {
            var value = new Permisjon(permisjon.prosent(), map(permisjon.type()));
            return new LocalDateSegment<>(permisjon.periode().getFomDato(), permisjon.periode().getTomDato(), value);
        }).toList(), permisjonSum());
    }

    private static AktivitetskravPermisjonType map(PermisjonsbeskrivelseType type) {
        return switch (type) {
            case UDEFINERT -> UDEFINERT;
            case PERMISJON, ANNEN_PERMISJON_LOVFESTET, ANNEN_PERMISJON_IKKE_LOVFESTET, PERMISJON_VED_MILITÆRTJENESTE, VELFERDSPERMISJON -> ANNEN_PERMISJON;
            case UTDANNINGSPERMISJON, UTDANNINGSPERMISJON_LOVFESTET, UTDANNINGSPERMISJON_IKKE_LOVFESTET -> UTDANNING;
            case PERMISJON_MED_FORELDREPENGER -> FORELDREPENGER;
            case PERMITTERING -> PERMITTERING;
        };
    }

    private static LocalDateTimeline<AktivitetskravPeriodeGrunnlag> lagTidslinjeMed0(LocalDate fraDato, LocalDate tilDato) {
        return new LocalDateTimeline<>(fraDato, tilDato,
            new AktivitetskravPeriodeGrunnlag(BigDecimal.ZERO, new Permisjon(BigDecimal.ZERO, UDEFINERT)));
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, BigDecimal> bigDesimalSum() {
        return StandardCombinators::sum;
    }

    private static LocalDateSegmentCombinator<Permisjon, Permisjon, Permisjon> permisjonSum() {
        return (datoInterval, datoSegment, datoSegment2) -> {
            var prosent = datoSegment != null ? datoSegment.getValue().prosent() : BigDecimal.ZERO;
            var prosent2 = datoSegment2 != null ? datoSegment2.getValue().prosent() : BigDecimal.ZERO;
            var sumType = sumType(datoSegment, datoSegment2);
            return new LocalDateSegment<>(datoInterval, new Permisjon(prosent.add(prosent2), sumType));
        };
    }

    private static AktivitetskravPermisjonType sumType(LocalDateSegment<Permisjon> datoSegment,
                                                       LocalDateSegment<Permisjon> datoSegment2) {
        var type1 = datoSegment != null ? datoSegment.getValue().type() : UDEFINERT;
        var type2 = datoSegment2 != null ? datoSegment2.getValue().type() : UDEFINERT;
        if (type1 != UDEFINERT && type2 != UDEFINERT && type1 != type2) {
            return ANNEN_PERMISJON;
        }
        return type1 != UDEFINERT ? type1 : type2;
    }

    private static LocalDateSegmentCombinator<BigDecimal, Permisjon, AktivitetskravPeriodeGrunnlag> bigDecimalTilAktivitetskravVurderingGrunnlagCombinator() {
        return (localDateInterval, stillingsprosent, permisjon) -> {
            var sumStillingsprosent = stillingsprosent != null ? stillingsprosent.getValue() : BigDecimal.ZERO;
            var sumPermisjon = permisjon != null ? permisjon.getValue() : new Permisjon(BigDecimal.ZERO, UDEFINERT);
            return new LocalDateSegment<>(localDateInterval, new AktivitetskravPeriodeGrunnlag(sumStillingsprosent, sumPermisjon));
        };
    }

    private static LocalDate hentHøyesteFraDatoPlusIntervall(List<OppgittPeriodeEntitet> aktuellePerioder) {
        return aktuellePerioder.stream()
            .map(OppgittPeriodeEntitet::getTom)
            .max(LocalDate::compareTo)
            .map(maxDato -> maxDato.plusWeeks(2))
            .orElseThrow();
    }

    private static LocalDate minsteFraDatoMinusIntervall(List<OppgittPeriodeEntitet> aktuellePerioder) {
        return aktuellePerioder.stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(LocalDate::compareTo)
            .map(minDato -> minDato.minusWeeks(2))
            .orElseThrow();
    }

    private static List<OppgittPeriodeEntitet> hentPerioderMedAktivitetskrav(YtelseFordelingAggregat ytelseaggregat) {
        return ytelseaggregat.getGjeldendeFordeling().getPerioder().stream().filter(OppgittPeriodeEntitet::kanAutomatiskAvklareMorsArbeid).toList();
    }

    private AktivitetskravArbeidPerioderEntitet lagAktivitetskravPerioderBuilder(Map<String, List<ArbeidsforholdMedPermisjon>> mapAvOrgnrOgAvtaler,
                                                                                 LocalDate fraDato,
                                                                                 LocalDate tilDato) {
        List<AktivitetskravArbeidPeriodeEntitet.Builder> builderListe = new ArrayList<>();
        mapAvOrgnrOgAvtaler.forEach((orgnr, value) -> {
            var stillingsprosentTidslinje = stillingsprosentTidslinje(value);
            var permisjonProsentTidslinje = permisjonTidslinje(value);
            var grunnlagTidslinje = stillingsprosentTidslinje.crossJoin(permisjonProsentTidslinje,
                    bigDecimalTilAktivitetskravVurderingGrunnlagCombinator())
                .crossJoin(lagTidslinjeMed0(fraDato, tilDato), StandardCombinators::coalesceLeftHandSide)
                .compress();

            builderListe.addAll(grunnlagTidslinje.stream().map(segment -> opprettBuilder(segment, orgnr)).toList());
        });

        var perioderBuilder = new AktivitetskravArbeidPerioderEntitet.Builder();

        builderListe.forEach(perioderBuilder::leggTil);

        return perioderBuilder.build();
    }

    private AktivitetskravArbeidPeriodeEntitet.Builder opprettBuilder(LocalDateSegment<AktivitetskravPeriodeGrunnlag> segment, String orgnr) {
        var value = segment.getValue();
        return new AktivitetskravArbeidPeriodeEntitet.Builder().medOrgNummer(orgnr)
            .medPeriode(segment.getFom(), segment.getTom())
            .medSumStillingsprosent(value.sumStillingsprosent())
            .medPermisjon(value.permisjon().prosent(), value.permisjon().type());
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

    private record AktivitetskravPeriodeGrunnlag(BigDecimal sumStillingsprosent, Permisjon permisjon) {
    }

    private record Permisjon(BigDecimal prosent, AktivitetskravPermisjonType type) {
    }
}
