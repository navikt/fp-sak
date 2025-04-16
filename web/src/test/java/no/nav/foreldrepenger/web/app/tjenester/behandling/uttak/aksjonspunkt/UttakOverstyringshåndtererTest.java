package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakOverstyringshåndterer;

@CdiDbAwareTest
class UttakOverstyringshåndtererTest {

    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final String ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef().getReferanse();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FastsettePerioderTjeneste fastettePerioderTjeneste;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;
    @Inject
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private UttakOverstyringshåndterer oppdaterer;

    @BeforeEach
    public void setup() {
        oppdaterer = new UttakOverstyringshåndterer(fastettePerioderTjeneste, uttakTjeneste,
            uttakInputTjeneste, mock(HistorikkinnslagRepository.class));
    }

    @Test
    void skalReturnereUtenOveropp() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusWeeks(2);
        var aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder().medArbeidsforholdId(ARBEIDSFORHOLD_ID)
            .medArbeidsgiver(new ArbeidsgiverLagreDto(ORGNR, null))
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medTrekkdager(BigDecimal.ZERO)
            .build();
        var aktiviteter = List.of(aktivitetLagreDto);
        var periodeResultatType = PeriodeResultatType.INNVILGET;
        var periodeResultatÅrsak = PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER;
        var stønadskontoType = UttakPeriodeType.FORELDREPENGER;
        var begrunnelse = "Dette er begrunnelsen";
        var periode = new UttakResultatPeriodeLagreDto.Builder().medTidsperiode(fom, tom)
            .medAktiviteter(aktiviteter)
            .medBegrunnelse(begrunnelse)
            .medPeriodeResultatType(periodeResultatType)
            .medPeriodeResultatÅrsak(periodeResultatÅrsak)
            .medFlerbarnsdager(false)
            .medSamtidigUttak(false)
            .build();

        var perioder = List.of(periode);
        var dto = new OverstyringUttakDto(perioder);

        // arrange
        var opprinneligPerioder = opprettUttakResultatPeriode(periodeResultatType, fom, tom, stønadskontoType);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultFordeling(fom)
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultBekreftetTerminbekreftelse()
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(fom).build())
            .lagre(repositoryProvider);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligPerioder);

        var result = oppdaterer.håndterOverstyring(dto, BehandlingReferanse.fra(behandling));

        var lagretUttak = uttakTjeneste.hent(behandling.getId());

        assertThat(lagretUttak.getGjeldendePerioder()).hasSize(1);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getTidsperiode().getFomDato()).isEqualTo(fom);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getTidsperiode().getTomDato()).isEqualTo(tom);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getAktiviteter()).hasSize(aktiviteter.size());
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getResultatType()).isEqualTo(periodeResultatType);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getResultatÅrsak()).isEqualTo(periodeResultatÅrsak);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     UttakPeriodeType stønadskontoType) {
        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(resultat, PeriodeResultatÅrsak.UKJENT).build();

        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.ref(ARBEIDSFORHOLD_ID)).medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID).build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(uttakResultatPeriode, uttakAktivitet).medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttakResultatPeriode);

        return perioder;
    }
}
