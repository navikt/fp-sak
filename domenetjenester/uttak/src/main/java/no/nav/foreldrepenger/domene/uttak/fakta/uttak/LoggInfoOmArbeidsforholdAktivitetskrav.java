package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class LoggInfoOmArbeidsforholdAktivitetskrav {
    private static final Logger LOG = LoggerFactory.getLogger(LoggInfoOmArbeidsforholdAktivitetskrav.class);
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste;

    LoggInfoOmArbeidsforholdAktivitetskrav() {
        //CDI
    }

    @Inject
    public LoggInfoOmArbeidsforholdAktivitetskrav(RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                                  PersonopplysningTjeneste personopplysningTjeneste,
                                                  ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste) {
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.abakusArbeidsforholdTjeneste = abakusArbeidsforholdTjeneste;
    }

    public void loggInfoOmArbeidsforhold(BehandlingReferanse behandlingReferanse,
                                         YtelseFordelingAggregat ytelseFordelingAggregat,
                                         List<OppgittPeriodeEntitet> aktuellePerioder) {
        var annenPartAktørId = hentAnnenPartAktørId(behandlingReferanse);
        if (annenPartAktørId == null) {
            return;
        }

        var fraDato = minsteFraDatoMinusIntervall(aktuellePerioder);
        var tilDato = hentHøyesteFraDatoPlusIntervall(aktuellePerioder);

        var arbeidsforholdInfo = abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(annenPartAktørId, fraDato, tilDato,
            behandlingReferanse.fagsakYtelseType());

        if (arbeidsforholdInfo.isEmpty()) {
            return;
        }

        var harAnnenForelderRett = ytelseFordelingAggregat.harAnnenForelderRett();

        loggInfoOmArbeidsforhold(fraDato, tilDato, behandlingReferanse.saksnummer(), harAnnenForelderRett, aktuellePerioder, arbeidsforholdInfo);
    }

    static void loggInfoOmArbeidsforhold(LocalDate fraDato, LocalDate tilDato,
                                         Saksnummer saksnummer, boolean harAnnenForelderRett,
                                         List<OppgittPeriodeEntitet> aktuellePerioder,
                                         List<ArbeidsforholdMedPermisjon> arbeidsforholdInfo) {

        var stillingsprosentTidslinje = stillingsprosentTidslinje(arbeidsforholdInfo);
        var permisjonProsentTidslinje = permisjonTidslinje(arbeidsforholdInfo);
        var grunnlagTidslinje = stillingsprosentTidslinje
            .crossJoin(permisjonProsentTidslinje, bigDecimalTilAktivitetskravVurderingGrunnlagCombinator())
            .crossJoin(lagTidslinjeMed0(fraDato, tilDato), StandardCombinators::coalesceLeftHandSide)
            .compress();

        var loggPrefiks = String.format("INFO-AKTIVITETSKRAV: %s (%s)", harAnnenForelderRett ? "BEGGE_RETT" : "BARE_FAR_RETT", saksnummer);

        aktuellePerioder.forEach(periode -> vurderOgLogg(periode, grunnlagTidslinje, loggPrefiks));
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, AktivitetskravVurderingGrunnlag> bigDecimalTilAktivitetskravVurderingGrunnlagCombinator() {
        return (localDateInterval, stillingsprosent, permisjonsprosent) -> new LocalDateSegment<>(localDateInterval,
            new AktivitetskravVurderingGrunnlag(
                stillingsprosent != null ? stillingsprosent.getValue() : BigDecimal.ZERO,
                permisjonsprosent != null ? permisjonsprosent.getValue() : BigDecimal.ZERO)
        );
    }

    private static void vurderOgLogg(OppgittPeriodeEntitet periode, LocalDateTimeline<AktivitetskravVurderingGrunnlag> grunnlagTidslinje, String loggPrefiks) {
        var vurderinger = grunnlagTidslinje.intersection(new LocalDateInterval(periode.getFom(), periode.getTom()))
            .stream()
            .map(segment -> vurderPeriode(segment.getValue()))
            .toList();
        loggPrefiks = String.format("%s for periode: [%s-%s] %s av saksbehandler",
            loggPrefiks,
            periode.getFom(),
            periode.getTom(),
            periode.getDokumentasjonVurdering().erGodkjent() ? "GODKJENT" : "IKKE GODKJENT");
        if (vurderinger.size() == 1) {
            LOG.info("{}, vurderingen: {}", loggPrefiks, vurderinger.getFirst());
        } else {
            LOG.info("{}, flere vurderinger: {}", loggPrefiks, vurderinger);
        }
    }

    private AktørId hentAnnenPartAktørId(BehandlingReferanse behandlingReferanse) {
        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(behandlingReferanse.saksnummer())
            .map(Behandling::getAktørId)
            .orElseGet(() -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(behandlingReferanse.behandlingId()).orElse(null));
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

    private static LocalDateTimeline<BigDecimal> permisjonTidslinje(List<ArbeidsforholdMedPermisjon> arbeidsforholdInfo) {
        //sørger for at hull blir 0% og at de permisjonene som overlapper  per arbeidsforhold summeres
        return new LocalDateTimeline<>(arbeidsforholdInfo.stream()
            .flatMap(a -> a.permisjoner().stream())
            .map(permisjon -> new LocalDateSegment<>(permisjon.periode().getFomDato(), permisjon.periode().getTomDato(), permisjon.prosent()))
            .toList(), bigDesimalSum());
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, BigDecimal> bigDesimalSum() {
        return StandardCombinators::sum;
    }

    private static LocalDateTimeline<BigDecimal> stillingsprosentTidslinje(List<ArbeidsforholdMedPermisjon> arbeidsforholdInfo) {
        return new LocalDateTimeline<>(arbeidsforholdInfo.stream()
            .flatMap(a -> a.aktivitetsavtaler().stream())
            .map(aktivitetAvtale -> new LocalDateSegment<>(aktivitetAvtale.periode().getFomDato(), aktivitetAvtale.periode().getTomDato(),
                aktivitetAvtale.stillingsprosent()))
            .toList(), bigDesimalSum());
    }

    private static LocalDateTimeline<AktivitetskravVurderingGrunnlag> lagTidslinjeMed0(LocalDate fraDato, LocalDate tilDato) {
        return new LocalDateTimeline<>(fraDato, tilDato, new AktivitetskravVurderingGrunnlag(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private static AktivitetskravVurdering vurderPeriode(AktivitetskravVurderingGrunnlag aktivitetskravVurderingGrunnlag) {
        var sumStillingsprosent = aktivitetskravVurderingGrunnlag.sumStillingsprosent();
        var sumPermisjonsprosent = aktivitetskravVurderingGrunnlag.sumPermisjonsProsent();
        var arbeidsprosent = sumStillingsprosent.subtract(sumPermisjonsprosent);

        if (sumStillingsprosent.compareTo(BigDecimal.valueOf(75)) >= 0 && sumPermisjonsprosent.compareTo(BigDecimal.ZERO) > 0) {
            return new AktivitetskravVurdering(VurderingForklaring.OK_OVER_75_PROSENT_MED_PERMISJON, sumStillingsprosent, sumPermisjonsprosent, arbeidsprosent);
        } else if (sumStillingsprosent.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return new AktivitetskravVurdering(VurderingForklaring.OK_OVER_75_PROSENT, sumStillingsprosent, sumPermisjonsprosent, arbeidsprosent);
        } else if (sumStillingsprosent.compareTo(BigDecimal.ZERO) <= 0 && sumPermisjonsprosent.compareTo(BigDecimal.ZERO) > 0) {
            return new AktivitetskravVurdering(VurderingForklaring.IKKE_OK_0_PROSENT_MED_PERMISJON, sumStillingsprosent, sumPermisjonsprosent, arbeidsprosent);
        } else if (sumStillingsprosent.compareTo(BigDecimal.ZERO) <= 0) {
            return new AktivitetskravVurdering(VurderingForklaring.IKKE_OK_0_PROSENT, sumStillingsprosent, sumPermisjonsprosent, arbeidsprosent);
        } else {
            return new AktivitetskravVurdering(VurderingForklaring.IKKE_OK_UNDER_75_PROSENT, sumStillingsprosent, sumPermisjonsprosent, arbeidsprosent);
        }
    }

    private enum VurderingForklaring {
        OK_OVER_75_PROSENT,
        OK_OVER_75_PROSENT_MED_PERMISJON,
        IKKE_OK_UNDER_75_PROSENT,
        IKKE_OK_0_PROSENT,
        IKKE_OK_PERMISJON,
        IKKE_OK_0_PROSENT_MED_PERMISJON
    }

    private record AktivitetskravVurderingGrunnlag(BigDecimal sumStillingsprosent, BigDecimal sumPermisjonsProsent) {
    }

    private record AktivitetskravVurdering(VurderingForklaring forklaring, BigDecimal sumStillingsprosent, BigDecimal sumPermisjonsprosent,
                                           BigDecimal arbeidsprosent) {
        @Override
        public String toString() {
            return forklaring + " stillingsprosent=" + sumStillingsprosent + " permisjonsprosent=" + sumPermisjonsprosent + " arbeidsprosent=" + arbeidsprosent;
        }
    }
}



