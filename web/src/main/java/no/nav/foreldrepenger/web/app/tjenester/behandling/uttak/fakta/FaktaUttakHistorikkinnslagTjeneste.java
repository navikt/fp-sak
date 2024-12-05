package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.UttakPeriodeEndringDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@ApplicationScoped
public class FaktaUttakHistorikkinnslagTjeneste {

    private Historikkinnslag2Repository historikkinnslagRepository;

    @Inject
    public FaktaUttakHistorikkinnslagTjeneste(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    FaktaUttakHistorikkinnslagTjeneste() {
        //CDI
    }

    public void opprettHistorikkinnslag(Long behandlingId, Long fagsakId,
                                        List<OppgittPeriodeEntitet> eksisterendePerioder,
                                        List<OppgittPeriodeEntitet> oppdatertePerioder,
                                        boolean overstyring,
                                        String begrunnelse) {
        var perioderMedEndringer = lagTekstForPerioderSomErEndret(eksisterendePerioder, oppdatertePerioder);
        var historikkinnslagBuilder = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.FAKTA_UTTAK)
            .addLinje(overstyring ? "Overstyrt vurdering:" : null);
        perioderMedEndringer.forEach(historikkinnslagBuilder::addlinje);
        historikkinnslagBuilder.addLinje(begrunnelse);
        historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
    }

    private List<HistorikkinnslagLinjeBuilder> lagTekstForPerioderSomErEndret(List<OppgittPeriodeEntitet> eksisterendePerioder, List<OppgittPeriodeEntitet> oppdatertePerioder) {
        var eksisterendeSegment = eksisterendePerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList();
        var oppdaterteSegment = oppdatertePerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList();
        var diffTidslinje = new LocalDateTimeline<>(oppdaterteSegment).combine(new LocalDateTimeline<>(eksisterendeSegment),
            this::utledEndringForPerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return diffTidslinje.toSegments().stream()
            .filter(Objects::nonNull)
            .map(LocalDateSegment::getValue)
            .filter(Objects::nonNull)
            .filter(e -> e.tekstFra() != null || e.tekstTil() != null)
            .map(endring -> fraTilEquals(String.format("Perioden %s", endring.intro()), endring.tekstFra(), endring.tekstTil()))
            .toList();

    }

    private LocalDateSegment<Endring> utledEndringForPerioder(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> oppdaterteLhs,
                                                              LocalDateSegment<OppgittPeriodeEntitet> eksisterendeRhs) {
        if (eksisterendeRhs == null) {
            return new LocalDateSegment<>(di, new Endring(format(di), null, tekstPeriodeFull(oppdaterteLhs.getValue())));
        } else if (oppdaterteLhs == null) {
            return new LocalDateSegment<>(di, new Endring(format(di), tekstPeriodeFull(eksisterendeRhs.getValue()), null));
        } else if (!erLikePerioder(oppdaterteLhs.getValue(), eksisterendeRhs.getValue())) {
            return new LocalDateSegment<>(di, new Endring(format(di), tekstPeriodeEndret(oppdaterteLhs.getValue(), eksisterendeRhs.getValue()),
                tekstPeriodeEndret(eksisterendeRhs.getValue(), oppdaterteLhs.getValue())));
        } else {
            return null;
        }
    }


    public static List<UttakPeriodeEndringDto> utledPerioderMedEndring(List<OppgittPeriodeEntitet> før, List<OppgittPeriodeEntitet> etter) {
        var førSegment = før.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList();
        var etterSegment = etter.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList();

        return new LocalDateTimeline<>(etterSegment).combine(new LocalDateTimeline<>(førSegment),
            FaktaUttakHistorikkinnslagTjeneste::utledEndringDto, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .toSegments().stream()
            .filter(Objects::nonNull)
            .map(LocalDateSegment::getValue)
            .filter(Objects::nonNull)
            .toList();
    }

    private static LocalDateSegment<UttakPeriodeEndringDto> utledEndringDto(LocalDateInterval di,
                                                                            LocalDateSegment<OppgittPeriodeEntitet> lhs,
                                                                            LocalDateSegment<OppgittPeriodeEntitet> rhs) {
        var builder = new UttakPeriodeEndringDto.Builder().medPeriode(di.getFomDato(), di.getTomDato());
        if (rhs == null) {
            return new LocalDateSegment<>(di, builder.medTypeEndring(UttakPeriodeEndringDto.TypeEndring.LAGT_TIL).build());
        } else if (lhs == null) {
            return new LocalDateSegment<>(di, builder.medTypeEndring(UttakPeriodeEndringDto.TypeEndring.SLETTET).build());
        } else if (!erLikePerioder(lhs.getValue(), rhs.getValue())) {
            return new LocalDateSegment<>(di, builder.medTypeEndring(UttakPeriodeEndringDto.TypeEndring.ENDRET).build());
        } else {
            return null;
        }
    }

    private String tekstPeriodeFull(OppgittPeriodeEntitet periode) {
        var builder = new StringBuilder(prefixUttakType(periode));
        uttakPeriodeTypeTekst(periode).ifPresent(t -> builder.append(", Konto: ").append(t));
        årsakTekst(periode).ifPresent(t -> builder.append(", Årsak: ").append(t));
        morsAktivitetTekst(periode).ifPresent(t -> builder.append(", Mors aktivitet: ").append(t));
        graderingTekst(periode).ifPresent(t -> builder.append(", Gradering: ").append(t));
        samtidigUttakTekst(periode).ifPresent(t -> builder.append(", Samtidig uttak%: ").append(t));
        flerbarnsTekst(periode).ifPresent(t -> builder.append(", ").append(t));
        return builder.toString();
    }

    private String tekstPeriodeEndret(OppgittPeriodeEntitet sammenlign, OppgittPeriodeEntitet bruk) {
        var builder = new StringBuilder(prefixUttakType(bruk));
        if (!Objects.equals(sammenlign.getPeriodeType(), bruk.getPeriodeType())) {
            uttakPeriodeTypeTekst(bruk).ifPresent(t -> builder.append(", Konto: ").append(t));
        }
        if (!Objects.equals(sammenlign.getÅrsak(), bruk.getÅrsak())) {
            årsakTekst(bruk).ifPresent(t -> builder.append(", Årsak: ").append(t));
        }
        if (!Objects.equals(sammenlign.getMorsAktivitet(), bruk.getMorsAktivitet())) {
            morsAktivitetTekst(bruk).ifPresent(t -> builder.append(", Mors aktivitet: ").append(t));
        }
        if (!Objects.equals(sammenlign.isGradert(), bruk.isGradert()) || sammenlign.isGradert() && (
            !Objects.equals(arbeidsprosent(sammenlign), arbeidsprosent(bruk)) || !Objects.equals(sammenlign.getGraderingAktivitetType(),
                bruk.getGraderingAktivitetType()) || !Objects.equals(sammenlign.getArbeidsgiver(), bruk.getArbeidsgiver()))) {
            graderingTekst(bruk).ifPresent(t -> builder.append(", Gradering: ").append(t));
        }
        if (!Objects.equals(samtidigUttaksprosent(sammenlign), samtidigUttaksprosent(bruk))) {
            samtidigUttakTekst(bruk).ifPresent(t -> builder.append(", Samtidig uttak%: ").append(t));
        }
        if (!Objects.equals(sammenlign.isFlerbarnsdager(), bruk.isFlerbarnsdager())) {
            flerbarnsTekst(bruk).ifPresent(t -> builder.append(", ").append(t));
        }
        return builder.toString();
    }
    private String mapGraderingAktivitetType(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(periode.getGraderingAktivitetType())
            .map(Enum::name)
            .orElse(null);
    }

    private String prefixUttakType(OppgittPeriodeEntitet periode) {
        if (periode.isUtsettelse()) {
            return "Utsettelse";
        } else if (periode.isOpphold()) {
            return "Opphold";
        } else if (periode.isOverføring()) {
            return "Overføring";
        } else {
            return "Uttak";
        }
    }

    private Optional<String> uttakPeriodeTypeTekst(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(periode.getPeriodeType())
            .filter(t -> !UttakPeriodeType.UDEFINERT.equals(t))
            .map(UttakPeriodeType::getNavn);
    }

    private Optional<String> årsakTekst(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(periode.getÅrsak())
            .filter(årsak -> !Årsak.UKJENT.getKode().equals(årsak.getKode()))
            .map(Årsak::getNavn);
    }

    private Optional<String> morsAktivitetTekst(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(periode.getMorsAktivitet())
            .filter(ma -> !MorsAktivitet.UDEFINERT.equals(ma) && !MorsAktivitet.IKKE_OPPGITT.equals(ma))
            .map(MorsAktivitet::getNavn);
    }

    private Optional<String> graderingTekst(OppgittPeriodeEntitet periode) {
        if (!periode.isGradert()) {
            return Optional.empty();
        }
        var aktivitet = Optional.ofNullable(periode.getArbeidsgiver()).map(Arbeidsgiver::getIdentifikator)
            .orElseGet(() -> periode.getGraderingAktivitetType().getNavn());
        return Optional.of(aktivitet + " - Arbeid: " + Optional.ofNullable(arbeidsprosent(periode)).orElse(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_EVEN) + "%");
    }

    private Optional<String> samtidigUttakTekst(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(samtidigUttaksprosent(periode))
            .map(p -> p.decimalValue().setScale(0, RoundingMode.HALF_EVEN).toString());
    }

    private Optional<String> flerbarnsTekst(OppgittPeriodeEntitet periode) {
        return periode.isFlerbarnsdager() ? Optional.of("Flerbarnsdager") : Optional.empty();
    }

    private record Endring(String intro, String tekstFra, String tekstTil) {}

    private static boolean erLikePerioder(OppgittPeriodeEntitet før, OppgittPeriodeEntitet etter) {
        return Objects.equals(før, etter) || Objects.equals(før.getPeriodeType(), etter.getPeriodeType()) && Objects.equals(før.getÅrsak(),
            etter.getÅrsak()) && Objects.equals(arbeidsprosent(før), arbeidsprosent(etter)) && Objects.equals(før.getArbeidsgiver(),
            etter.getArbeidsgiver()) && Objects.equals(før.getGraderingAktivitetType(), etter.getGraderingAktivitetType()) && Objects.equals(
            samtidigUttaksprosent(før), samtidigUttaksprosent(etter)) && Objects.equals(før.isFlerbarnsdager(), etter.isFlerbarnsdager())
            && Objects.equals(før.getMorsAktivitet(), etter.getMorsAktivitet());
    }

    private static SamtidigUttaksprosent samtidigUttaksprosent(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(periode.getSamtidigUttaksprosent())
            .filter(SamtidigUttaksprosent::merEnn0)
            .orElse(null);
    }

    private static BigDecimal arbeidsprosent(OppgittPeriodeEntitet periode) {
        return Optional.ofNullable(periode.getArbeidsprosent())
            .filter(arb -> arb.compareTo(BigDecimal.ZERO) > 0)
            .orElse(null);
    }
}
