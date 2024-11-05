package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
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
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FastsetteUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;

@CdiDbAwareTest
class FastsettUttakOppdatererTest {

    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FastsettePerioderTjeneste fastettePerioderTjeneste;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;
    @Inject
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private FastsettUttakOppdaterer oppdaterer;

    @BeforeEach
    public void setup() {
        oppdaterer = new FastsettUttakOppdaterer(mock(HistorikkTjenesteAdapter.class), fastettePerioderTjeneste, uttakTjeneste, uttakInputTjeneste,
            repositoryProvider.getBehandlingRepository());
    }

    @Test
    void skalReturnereUtenOveropp() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusWeeks(2);
        var aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder().medArbeidsforholdId(ARBEIDSFORHOLD_ID.getReferanse())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsgiver(new ArbeidsgiverLagreDto(ORGNR, null))
            .medTrekkdager(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .build();
        var aktiviteter = List.of(aktivitetLagreDto);
        var periodeResultatType = PeriodeResultatType.INNVILGET;
        var periodeResultatÅrsak = PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER;
        var stønadskontoType = UttakPeriodeType.FORELDREPENGER;
        var begrunnelse = "Dette er begrunnelsen";
        var periode1 = new UttakResultatPeriodeLagreDto.Builder().medTidsperiode(fom, tom)
            .medBegrunnelse(begrunnelse)
            .medAktiviteter(aktiviteter)
            .medPeriodeResultatType(periodeResultatType)
            .medPeriodeResultatÅrsak(periodeResultatÅrsak)
            .medFlerbarnsdager(false)
            .medSamtidigUttak(false)
            .build();
        var perioder = List.of(periode1);
        FastsetteUttakDto dto = new FastsetteUttakDto.FastsetteUttakPerioderDto(perioder);

        // arrange
        var opprinneligPerioder = opprettUttakResultatPeriode(periodeResultatType, fom, tom, stønadskontoType);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultFordeling(fom)
            .medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligPerioder);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(fom).build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandling.getId()).medAvklarteDatoer(avklarteUttakDatoer).build();
        ytelsesFordelingRepository.lagre(behandling.getId(), ytelseFordelingAggregat);

        var result = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

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

    @Test
    void skalAvbryteOverstyringAksjonspunktHvisDetEksisterer() {
        var fom = LocalDate.of(2019, 1, 1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultFordeling(fom)
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(fom).build());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER, BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER, BehandlingStegType.VURDER_UTTAK);
        scenario.medBehandlingVedtak()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("saksbehandler")
            .medVedtakstidspunkt(LocalDateTime.now());
        var behandling = scenario.lagre(repositoryProvider);

        var opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, fom, fom.plusMonths(1), UttakPeriodeType.MØDREKVOTE);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinnelig);

        var dtoPeriode = new UttakResultatPeriodeLagreDto.Builder().medTidsperiode(fom, fom.plusMonths(1))
            .medBegrunnelse(" ")
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medPeriodeResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        FastsetteUttakDto dto = new FastsetteUttakDto.FastsetteUttakPerioderDto(List.of(dtoPeriode));
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        var avbrutt = resultat.getEkstraAksjonspunktResultat()
            .stream()
            .anyMatch(aer -> AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER.equals(aer.getAksjonspunktDefinisjon())
                && AksjonspunktStatus.AVBRUTT.equals(aer.getMålStatus()));
        assertThat(avbrutt).isTrue();
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     UttakPeriodeType stønadskontoType) {
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(resultat, PeriodeResultatÅrsak.UKJENT).build();

        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet).medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();

        periode.leggTilAktivitet(periodeAktivitet);

        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);

        return perioder;
    }

}
