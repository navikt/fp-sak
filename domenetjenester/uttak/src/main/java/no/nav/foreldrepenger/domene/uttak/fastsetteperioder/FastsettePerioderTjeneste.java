package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.OverstyrUttakResultatValidator;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.feil.FeilFactory;

@ApplicationScoped
public class FastsettePerioderTjeneste {

    private OverstyrUttakResultatValidator uttakResultatValidator;
    private FastsettePerioderRegelAdapter regelAdapter;
    private UttakRepository uttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    FastsettePerioderTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettePerioderTjeneste(UttakRepository uttakRepository,
                                     YtelsesFordelingRepository ytelsesfordelingRepository,
                                     OverstyrUttakResultatValidator uttakResultatValidator,
                                     FastsettePerioderRegelAdapter regelAdapter) {
        this.uttakResultatValidator = uttakResultatValidator;
        this.regelAdapter = regelAdapter;
        this.uttakRepository = uttakRepository;
        this.ytelsesFordelingRepository = ytelsesfordelingRepository;
    }

    public void fastsettePerioder(UttakInput input) {
        UttakResultatPerioderEntitet resultat = regelAdapter.fastsettePerioder(input);
        uttakRepository.lagreOpprinneligUttakResultatPerioder(input.getBehandlingReferanse().getBehandlingId(), resultat);
    }

    public void manueltFastsettePerioder(UttakInput uttakInput, UttakResultatPerioder perioder) {
        valider(perioder, uttakInput);
        lagreManueltFastsatt(uttakInput, perioder);
    }

    private void valider(UttakResultatPerioder perioder, UttakInput uttakInput) {
        UttakResultatPerioder opprinnelig = hentOpprinnelig(uttakInput.getBehandlingReferanse().getBehandlingId());
        uttakResultatValidator.valider(uttakInput, opprinnelig, perioder);
    }

    private UttakResultatPerioder hentOpprinnelig(Long behandlingId) {
        UttakResultatEntitet entitet = uttakRepository.hentUttakResultat(behandlingId);
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        Optional<LocalDate> endringsdato = Optional.empty();
        if (ytelseFordelingAggregat.isPresent()) {
            if (ytelseFordelingAggregat.get().getAvklarteDatoer().isPresent()) {
                AvklarteUttakDatoerEntitet avklarteUttakDatoer = ytelseFordelingAggregat.get().getAvklarteDatoer().get();
                if (avklarteUttakDatoer.getGjeldendeEndringsdato() != null) {
                    endringsdato = Optional.of(avklarteUttakDatoer.getGjeldendeEndringsdato());
                }

            }
        }
        return map(entitet, endringsdato);
    }

    private UttakResultatPerioder map(UttakResultatEntitet uttakResultatEntitet, Optional<LocalDate> endringsdato) {

        List<UttakResultatPeriode> perioder = new ArrayList<>();

        for (UttakResultatPeriodeEntitet entitet : uttakResultatEntitet.getOpprinneligPerioder().getPerioder()) {
            UttakResultatPeriode periode = map(entitet);
            perioder.add(periode);
        }

        return new UttakResultatPerioder(perioder, endringsdato);
    }

    private UttakResultatPeriode map(UttakResultatPeriodeEntitet entitet) {
        List<UttakResultatPeriodeAktivitet> aktiviteter = new ArrayList<>();
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : entitet.getAktiviteter()) {
            aktiviteter.add(map(aktivitet));
        }
        UttakResultatPeriode.Builder periodeBuilder = new UttakResultatPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(entitet.getFom(), entitet.getTom()))
            .medAktiviteter(aktiviteter)
            .medBegrunnelse(entitet.getBegrunnelse())
            .medType(entitet.getPeriodeResultatType())
            .medÅrsak(entitet.getPeriodeResultatÅrsak())
            .medFlerbarnsdager(entitet.isFlerbarnsdager())
            .medUtsettelseType(entitet.getUtsettelseType())
            .medOppholdÅrsak(entitet.getOppholdÅrsak())
            .medSamtidigUttak(entitet.isSamtidigUttak())
            .medSamtidigUttaksprosent(entitet.getSamtidigUttaksprosent());
        return periodeBuilder.build();
    }

    private UttakResultatPeriodeAktivitet map(UttakResultatPeriodeAktivitetEntitet periodeAktivitet) {
        return new UttakResultatPeriodeAktivitet.Builder()
            .medArbeidsprosent(periodeAktivitet.getArbeidsprosent())
            .medTrekkonto(periodeAktivitet.getTrekkonto())
            .medTrekkdager(periodeAktivitet.getTrekkdager())
            .medUtbetalingsgrad(periodeAktivitet.getUtbetalingsprosent())
            .medArbeidsforholdId(periodeAktivitet.getArbeidsforholdId())
            .medArbeidsgiver(periodeAktivitet.getUttakAktivitet().getArbeidsgiver().orElse(null))
            .medUttakArbeidType(periodeAktivitet.getUttakArbeidType())
            .build();
    }

    private void lagreManueltFastsatt(UttakInput uttakInput, UttakResultatPerioder perioder) {

        UttakResultatPerioderEntitet overstyrtEntitet = new UttakResultatPerioderEntitet();
        Long behandlingId = uttakInput.getBehandlingReferanse().getBehandlingId();
        UttakResultatEntitet opprinnelig = uttakRepository.hentUttakResultat(behandlingId);
        for (UttakResultatPeriode periode : perioder.getPerioder()) {
            UttakResultatPeriodeEntitet matchendeOpprinneligPeriode = matchendeOpprinneligPeriode(periode, opprinnelig.getOpprinneligPerioder());
            UttakResultatPeriodeEntitet periodeEntitet = map(matchendeOpprinneligPeriode, periode);
            overstyrtEntitet.leggTilPeriode(periodeEntitet);
        }

        uttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrtEntitet);
    }

    private UttakResultatPeriodeEntitet matchendeOpprinneligPeriode(UttakResultatPeriode periode,
                                                                    UttakResultatPerioderEntitet opprinnelig) {
        Optional<UttakResultatPeriodeEntitet> matchende = opprinnelig.getPerioder()
            .stream()
            .filter(oPeriode -> {
                LocalDateInterval tidsperiode = periode.getTidsperiode();
                return (tidsperiode.getFomDato().isEqual(oPeriode.getFom()) || tidsperiode.getFomDato().isAfter(oPeriode.getFom()))
                    && (tidsperiode.getTomDato().isEqual(oPeriode.getTom()) || tidsperiode.getTomDato().isBefore(oPeriode.getTom()));
            })
            .findFirst();

        return matchende.orElseThrow(()
            -> FeilFactory.create(FastsettePerioderFeil.class).manglendeOpprinneligPeriode(periode).toException());
    }

    private UttakResultatPeriodeEntitet map(UttakResultatPeriodeEntitet opprinneligPeriode,
                                            UttakResultatPeriode nyPeriode) {
        UttakResultatPeriodeEntitet.Builder builder = new UttakResultatPeriodeEntitet.Builder(nyPeriode.getTidsperiode().getFomDato(),
            nyPeriode.getTidsperiode().getTomDato())
            .medPeriodeSoknad(opprinneligPeriode.getPeriodeSøknad().orElse(null))
            .medPeriodeResultat(nyPeriode.getResultatType(), nyPeriode.getResultatÅrsak())
            .medBegrunnelse(nyPeriode.getBegrunnelse())
            .medGraderingInnvilget(nyPeriode.isGraderingInnvilget())
            .medGraderingAvslagÅrsak(nyPeriode.getGraderingAvslagÅrsak())
            .medUtsettelseType(opprinneligPeriode.getUtsettelseType())
            .medOppholdÅrsak(nyPeriode.getOppholdÅrsak())
            .medOverføringÅrsak(opprinneligPeriode.getOverføringÅrsak())
            .medSamtidigUttak(nyPeriode.isSamtidigUttak())
            .medSamtidigUttaksprosent(nyPeriode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(nyPeriode.isFlerbarnsdager())
            .medManueltBehandlet(nyPeriode.getBegrunnelse() != null);
        UttakResultatPeriodeEntitet periodeEntitet = builder.build();

        for (UttakResultatPeriodeAktivitet nyAktivitet : nyPeriode.getAktiviteter()) {
            UttakResultatPeriodeAktivitetEntitet matchendeOpprinneligAktivitet = matchendeOpprinneligAktivitet(opprinneligPeriode, nyAktivitet);
            UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periodeEntitet, matchendeOpprinneligAktivitet.getUttakAktivitet())
                .medTrekkonto(nyAktivitet.getTrekkonto())
                .medTrekkdager(nyAktivitet.getTrekkdager())
                .medArbeidsprosent(nyAktivitet.getArbeidsprosent())
                .medUtbetalingsprosent(nyAktivitet.getUtbetalingsgrad())
                .medErSøktGradering(matchendeOpprinneligAktivitet.isSøktGradering())
                .build();
            periodeEntitet.leggTilAktivitet(periodeAktivitet);
        }
        return periodeEntitet;
    }

    private UttakResultatPeriodeAktivitetEntitet matchendeOpprinneligAktivitet(UttakResultatPeriodeEntitet opprinneligPeriode,
                                                                               UttakResultatPeriodeAktivitet nyAktivitet) {
        return opprinneligPeriode.getAktiviteter()
            .stream()
            .filter(opprinneligAktivitet -> {
                return Objects.equals(opprinneligAktivitet.getUttakArbeidType(), nyAktivitet.getUttakArbeidType()) &&
                    Objects.equals(opprinneligAktivitet.getArbeidsforholdId(), nyAktivitet.getArbeidsforholdId()) &&
                    Objects.equals(opprinneligAktivitet.getUttakAktivitet().getArbeidsgiver().orElse(null), nyAktivitet.getArbeidsgiver().orElse(null));
            })
            .findFirst().orElseThrow(() -> FeilFactory.create(FastsettePerioderFeil.class).manglendeOpprinneligAktivitet(nyAktivitet, opprinneligPeriode.getAktiviteter()).toException());
    }
}
