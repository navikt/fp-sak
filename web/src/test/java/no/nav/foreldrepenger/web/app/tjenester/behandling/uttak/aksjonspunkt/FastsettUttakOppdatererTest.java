package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FastsetteUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FastsettUttakOppdatererTest {

    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    @Inject
    private FastsettePerioderTjeneste fastettePerioderTjeneste;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    private FastsettUttakOppdaterer oppdaterer;

    @Before
    public void setup() {
        oppdaterer = new FastsettUttakOppdaterer(repositoryProvider, mock(HistorikkTjenesteAdapter.class), fastettePerioderTjeneste, uttakInputTjeneste);
    }

    @Test
    public void skalReturnereUtenOveropp() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(2);
        UttakResultatPeriodeAktivitetLagreDto aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID.getReferanse())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsgiver(new ArbeidsgiverLagreDto(ORGNR, null))
            .medTrekkdager(BigDecimal.ZERO)
            .build();
        List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter = Collections.singletonList(aktivitetLagreDto);
        PeriodeResultatType periodeResultatType = PeriodeResultatType.INNVILGET;
        PeriodeResultatÅrsak periodeResultatÅrsak = InnvilgetÅrsak.UTTAK_OPPFYLT;
        StønadskontoType stønadskontoType = StønadskontoType.FORELDREPENGER;
        String begrunnelse = "Dette er begrunnelsen";
        UttakResultatPeriodeLagreDto periode1 = new UttakResultatPeriodeLagreDto.Builder()
        .medTidsperiode(fom, tom)
            .medBegrunnelse(begrunnelse)
            .medAktiviteter(aktiviteter)
            .medPeriodeResultatType(periodeResultatType)
            .medPeriodeResultatÅrsak(periodeResultatÅrsak)
            .medFlerbarnsdager(false)
            .medSamtidigUttak(false)
            .build();
        List<UttakResultatPeriodeLagreDto> perioder = Collections.singletonList(periode1);
        FastsetteUttakDto dto = new FastsetteUttakDto.FastsetteUttakPerioderDto(perioder);

        //arrange
        UttakResultatPerioderEntitet opprinneligPerioder = opprettUttakResultatPeriode(periodeResultatType, fom, tom, stønadskontoType);
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittFordeling(fom)
            .medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligPerioder);

        OppdateringResultat result = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        var lagretUttak = repositoryProvider.getUttakRepository().hentUttakResultat(behandling.getId());

        assertThat(lagretUttak.getGjeldendePerioder().getPerioder()).hasSize(1);
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getTidsperiode().getFomDato()).isEqualTo(fom);
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getTidsperiode().getTomDato()).isEqualTo(tom);
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getAktiviteter()).hasSize(aktiviteter.size());
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getPeriodeResultatType()).isEqualTo(periodeResultatType);
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getPeriodeResultatÅrsak()).isEqualTo(periodeResultatÅrsak);
        assertThat(lagretUttak.getGjeldendePerioder().getPerioder().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
    }

    @Test
    public void skalAvbryteOverstyringAksjonspunktHvisDetEksisterer() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultOppgittFordeling(LocalDate.of(2019, 1, 1));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER, BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER, BehandlingStegType.VURDER_UTTAK);
        scenario.medBehandlingVedtak()
            .medVedtakResultatType(VedtakResultatType.DELVIS_INNVILGET)
            .medAnsvarligSaksbehandler("saksbehandler")
            .medVedtakstidspunkt(LocalDateTime.now());
        Behandling behandling = scenario.lagre(repositoryProvider);

        UttakResultatPerioderEntitet opprinnelig = new UttakResultatPerioderEntitet();
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinnelig);

        FastsetteUttakDto dto = new FastsetteUttakDto.FastsetteUttakPerioderDto(Collections.emptyList());
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        boolean avbrutt = resultat.getEkstraAksjonspunktResultat().stream()
            .anyMatch(aer -> AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER.equals(aer.getElement1())
                && AksjonspunktStatus.AVBRUTT.equals(aer.getElement2()));
        assertThat(avbrutt).isTrue();
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType) {
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(resultat, PeriodeResultatÅrsak.UKJENT)
            .build();

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsprosent(BigDecimal.valueOf(100))
            .build();

        periode.leggTilAktivitet(periodeAktivitet);

        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);

        return perioder;
    }

}
