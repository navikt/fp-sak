package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;

public class VedtaksperioderHelper {
    private static final KodeMapper<UttakUtsettelseType, UtsettelseÅrsak> utsettelseÅrsakMapper = initUtsettelseÅrsakMapper();

    private static final KodeMapper<StønadskontoType, UttakPeriodeType> periodeTypeMapper = initPeriodeTypeMapper();

    List<OppgittPeriodeEntitet> opprettOppgittePerioder(UttakResultatEntitet uttakResultatFraForrigeBehandling,
                                                        List<OppgittPeriodeEntitet> søknadsperioder,
                                                        LocalDate endringsdato) {
        Optional<LocalDate> førsteSøknadsdato = OppgittPeriodeUtil.finnFørsteSøknadsdato(søknadsperioder);
        List<OppgittPeriodeEntitet> vedtaksperioder = lagVedtaksperioder(uttakResultatFraForrigeBehandling, endringsdato, førsteSøknadsdato);

        final List<OppgittPeriodeEntitet> søknadOgVedtaksperioder = new ArrayList<>();
        søknadsperioder.forEach(op -> søknadOgVedtaksperioder.add(OppgittPeriodeBuilder.fraEksisterende(op).build()));
        søknadOgVedtaksperioder.addAll(vedtaksperioder);
        return OppgittPeriodeUtil.sorterEtterFom(søknadOgVedtaksperioder);
    }

    private List<OppgittPeriodeEntitet> lagVedtaksperioder(UttakResultatEntitet uttakResultat, LocalDate endringsdato, Optional<LocalDate> førsteSøknadsdato) {
        return uttakResultat.getGjeldendePerioder().getPerioder()
            .stream()
            .filter(p -> filtrerUttaksperioder(p, endringsdato, førsteSøknadsdato))
            .filter(this::erPeriodeFraSøknad)
            .map(this::konverter)
            .flatMap(p -> klipp(p, endringsdato, førsteSøknadsdato))
            .collect(Collectors.toList());
    }

    private boolean filtrerUttaksperioder(UttakResultatPeriodeEntitet periode, LocalDate endringsdato, Optional<LocalDate> førsteSøknadsdatoOptional) {
        Objects.requireNonNull(endringsdato);

        if (periode.getTom().isBefore(endringsdato)) {
            //Perioder før endringsdato skal filtreres bort
            return false;
        }

        //avslått perioder med null trekk dager skal filtreres bort
        if (avslåttPgaAvTaptPeriodeTilAnnenpart(periode)) {
            return false;
        }

        if (førsteSøknadsdatoOptional.isPresent()) {
            LocalDate førsteSøknadsdato = førsteSøknadsdatoOptional.get();
            if (periode.getFom().equals(førsteSøknadsdato) || periode.getFom().isAfter(førsteSøknadsdato)) {
                //Perioder som starter på eller etter første søknadsdato skal filtreres bort
                return false;
            }
        }
        return true;
    }

    public static boolean avslåttPgaAvTaptPeriodeTilAnnenpart(UttakResultatPeriodeEntitet periode) {
        return IkkeOppfyltÅrsak.årsakerTilAvslagPgaAnnenpart().contains(periode.getResultatÅrsak()) &&
            PeriodeResultatType.AVSLÅTT.equals(periode.getResultatType()) &&
            periode.getAktiviteter().stream().allMatch(aktivitet -> aktivitet.getTrekkdager().equals(Trekkdager.ZERO));
    }

    private boolean erPeriodeFraSøknad(UttakResultatPeriodeEntitet periode) {
        return periode.getPeriodeSøknad().isPresent();
    }

    private Stream<OppgittPeriodeEntitet> klipp(OppgittPeriodeEntitet op, LocalDate endringsdato, Optional<LocalDate> førsteSøknadsdato) {
        Objects.requireNonNull(endringsdato);

        OppgittPeriodeBuilder opb = OppgittPeriodeBuilder.fraEksisterende(op);
        LocalDate fom = op.getFom();
        LocalDate tom = op.getTom();
        if (endringsdato.isAfter(fom)) {
            fom = endringsdato;
        }
        if (førsteSøknadsdato.isPresent() && (førsteSøknadsdato.get().isBefore(tom) || førsteSøknadsdato.get().isEqual(tom))) {
            tom = førsteSøknadsdato.get().minusDays(1);
        }
        if (!fom.isAfter(tom)) {
            return Stream.of(opb.medPeriode(fom, tom).build());
        }
        return Stream.empty();
    }

    OppgittPeriodeEntitet konverter(UttakResultatPeriodeEntitet up) {
        OppgittPeriodeBuilder builder = OppgittPeriodeBuilder.ny()
            .medPeriode(up.getTidsperiode().getFomDato(), up.getTidsperiode().getTomDato())
            .medPeriodeType(finnPeriodetype(up))
            .medSamtidigUttak(up.isSamtidigUttak())
            .medSamtidigUttaksprosent(up.getSamtidigUttaksprosent())
            .medFlerbarnsdager(up.isFlerbarnsdager())
            .medErArbeidstaker(erArbeidstaker(up))
            .medErSelvstendig(erSelvstendig(up))
            .medErFrilanser(erFrilans(up))
            .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        finnMorsAktivitet(up).ifPresent(builder::medMorsAktivitet);
        finnGraderingArbeidsprosent(up).ifPresent(builder::medArbeidsprosent);
        finnUtsettelsesÅrsak(up).ifPresent(builder::medÅrsak);
        finnGradertArbeidsgiver(up).ifPresent(builder::medArbeidsgiver);
        finnOppholdsÅrsak(up).ifPresent(builder::medÅrsak);
        finnOverføringÅrsak(up).ifPresent(builder::medÅrsak);
        finnSamtidigUttaksprosent(up).ifPresent(builder::medSamtidigUttaksprosent);
        builder.medMottattDato(up.getPeriodeSøknad().orElseThrow().getMottattDatoTemp());

        return builder.build();
    }

    private Optional<Årsak> finnOverføringÅrsak(UttakResultatPeriodeEntitet up) {
        if (harOverføringÅrsak(up)) {
            return Optional.of(up.getOverføringÅrsak());
        }
        return Optional.empty();
    }

    private boolean harOverføringÅrsak(UttakResultatPeriodeEntitet up) {
        return !OverføringÅrsak.UDEFINERT.equals(up.getOverføringÅrsak());
    }

    private Optional<BigDecimal> finnSamtidigUttaksprosent(UttakResultatPeriodeEntitet up) {
        if (up.getPeriodeSøknad().isPresent()) {
            return Optional.ofNullable(up.getPeriodeSøknad().get().getSamtidigUttaksprosent());
        }
        return Optional.empty();
    }

    private Optional<MorsAktivitet> finnMorsAktivitet(UttakResultatPeriodeEntitet up) {
        if (up.getPeriodeSøknad().isPresent()) {
            return Optional.of(up.getPeriodeSøknad().get().getMorsAktivitet());
        }
        return Optional.empty();
    }

    private Optional<Arbeidsgiver> finnGradertArbeidsgiver(UttakResultatPeriodeEntitet up) {
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : up.getAktiviteter()) {
            if (aktivitet.isSøktGradering()) {
                return aktivitet.getUttakAktivitet().getArbeidsgiver();
            }
        }
        return Optional.empty();
    }

    private boolean erArbeidstaker(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter().stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .anyMatch(a -> UttakArbeidType.ORDINÆRT_ARBEID.equals(a.getUttakArbeidType()));
    }

    private boolean erFrilans(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter().stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .anyMatch(a -> UttakArbeidType.FRILANS.equals(a.getUttakArbeidType()));
    }

    private boolean erSelvstendig(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter().stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .anyMatch(a -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(a.getUttakArbeidType()));
    }

    private UttakPeriodeType finnPeriodetype(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        //Oppholdsperiode har ingen aktiviteter
        if (harOppholdÅrsak(uttakResultatPeriode)) {
            return UttakPeriodeType.ANNET;
        }
        Optional<StønadskontoType> stønadskontoType = uttakResultatPeriode.getAktiviteter().stream()
            .max(Comparator.comparing(aktivitet -> aktivitet.getTrekkdager().decimalValue()))
            .map(UttakResultatPeriodeAktivitetEntitet::getTrekkonto);
        if (stønadskontoType.isPresent()) {
            Optional<UttakPeriodeType> uttakPeriodeType = periodeTypeMapper.map(stønadskontoType.get());
            if (uttakPeriodeType.isPresent()) {
                return uttakPeriodeType.get();
            }
        }
        throw new IllegalStateException("Uttaksperiode mangler stønadskonto");
    }

    private Optional<UtsettelseÅrsak> finnUtsettelsesÅrsak(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        if (erInnvilgetUtsettelse(uttakResultatPeriode)) {
            return utsettelseÅrsakMapper.map(uttakResultatPeriode.getUtsettelseType());
        }
        return Optional.empty();
    }

    private boolean erInnvilgetUtsettelse(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        UttakUtsettelseType utsettelseType = uttakResultatPeriode.getUtsettelseType();
        if (utsettelseType != null && !UttakUtsettelseType.UDEFINERT.equals(utsettelseType)) {
            return uttakResultatPeriode.getAktiviteter().stream()
                .noneMatch(a -> a.getUtbetalingsgrad() != null && a.getUtbetalingsgrad().compareTo(BigDecimal.ZERO) > 0);
        }
        return false;
    }

    private Optional<OppholdÅrsak> finnOppholdsÅrsak(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        if (harOppholdÅrsak(uttakResultatPeriode)) {
            return Optional.of(uttakResultatPeriode.getOppholdÅrsak());
        }
        return Optional.empty();
    }

    private boolean harOppholdÅrsak(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        return !OppholdÅrsak.UDEFINERT.equals(uttakResultatPeriode.getOppholdÅrsak());
    }

    private static KodeMapper<UttakUtsettelseType, UtsettelseÅrsak> initUtsettelseÅrsakMapper() {
        return KodeMapper
            .medMapping(UttakUtsettelseType.FERIE, UtsettelseÅrsak.FERIE)
            .medMapping(UttakUtsettelseType.ARBEID, UtsettelseÅrsak.ARBEID)
            .medMapping(UttakUtsettelseType.SYKDOM_SKADE, UtsettelseÅrsak.SYKDOM)
            .medMapping(UttakUtsettelseType.SØKER_INNLAGT, UtsettelseÅrsak.INSTITUSJON_SØKER)
            .medMapping(UttakUtsettelseType.BARN_INNLAGT, UtsettelseÅrsak.INSTITUSJON_BARN)
            .medMapping(UttakUtsettelseType.HV_OVELSE, UtsettelseÅrsak.HV_OVELSE)
            .medMapping(UttakUtsettelseType.NAV_TILTAK, UtsettelseÅrsak.NAV_TILTAK)
            .build();
    }

    private static KodeMapper<StønadskontoType, UttakPeriodeType> initPeriodeTypeMapper() {
        return KodeMapper
            .medMapping(StønadskontoType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE)
            .medMapping(StønadskontoType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE)
            .medMapping(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medMapping(StønadskontoType.MØDREKVOTE, UttakPeriodeType.MØDREKVOTE)
            .medMapping(StønadskontoType.FORELDREPENGER, UttakPeriodeType.FORELDREPENGER)
            .medMapping(StønadskontoType.UDEFINERT, UttakPeriodeType.UDEFINERT)
            .build();
    }


    private Optional<BigDecimal> finnGraderingArbeidsprosent(UttakResultatPeriodeEntitet up) {
        if (up.getPeriodeSøknad().isEmpty() || up.getPeriodeSøknad().get().getGraderingArbeidsprosent() == null) {
            return Optional.empty();
        }
        for (UttakResultatPeriodeAktivitetEntitet akt : up.getAktiviteter()) {
            if (akt.isSøktGradering()) {
                return Optional.ofNullable(up.getPeriodeSøknad().get().getGraderingArbeidsprosent());
            }
        }
        return Optional.empty();
    }
}
