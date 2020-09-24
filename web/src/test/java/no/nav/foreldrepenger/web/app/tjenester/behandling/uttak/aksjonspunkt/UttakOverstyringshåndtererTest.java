package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakOverstyringshåndterer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class UttakOverstyringshåndtererTest {

    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final String ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef().getReferanse();

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    @Inject
    private FastsettePerioderTjeneste fastettePerioderTjeneste;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;
    @Inject
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private UttakOverstyringshåndterer oppdaterer;

    @Before
    public void setup() {
        oppdaterer = new UttakOverstyringshåndterer(mock(HistorikkTjenesteAdapter.class),
            fastettePerioderTjeneste, uttakTjeneste, uttakInputTjeneste);
    }

    @Test
    public void skalReturnereUtenOveropp() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(2);
        UttakResultatPeriodeAktivitetLagreDto aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
            .medArbeidsgiver(new ArbeidsgiverLagreDto(ORGNR, null))
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medTrekkdager(BigDecimal.ZERO)
            .build();
        List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter = Collections.singletonList(aktivitetLagreDto);
        PeriodeResultatType periodeResultatType = PeriodeResultatType.INNVILGET;
        PeriodeResultatÅrsak periodeResultatÅrsak = InnvilgetÅrsak.UTTAK_OPPFYLT;
        StønadskontoType stønadskontoType = StønadskontoType.FORELDREPENGER;
        String begrunnelse = "Dette er begrunnelsen";
        UttakResultatPeriodeLagreDto periode = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(fom, tom)
            .medAktiviteter(aktiviteter)
            .medBegrunnelse(begrunnelse)
            .medPeriodeResultatType(periodeResultatType)
            .medPeriodeResultatÅrsak(periodeResultatÅrsak)
            .medFlerbarnsdager(false)
            .medSamtidigUttak(false)
            .build();

        List<UttakResultatPeriodeLagreDto> perioder = Collections.singletonList(periode);
        OverstyringUttakDto dto = new OverstyringUttakDto(perioder);

        //arrange
        UttakResultatPerioderEntitet opprinneligPerioder = opprettUttakResultatPeriode(periodeResultatType, fom, tom, stønadskontoType);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittFordeling(fom)
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultBekreftetTerminbekreftelse()
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(fom).build())
            .lagre(repositoryProvider);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligPerioder);

        OppdateringResultat result = oppdaterer.håndterOverstyring(dto, behandling, kontekst(behandling));

        var lagretUttak = uttakTjeneste.hentUttak(behandling.getId());

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

    private BehandlingskontrollKontekst kontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(),
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType) {
        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT)
            .build();

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), InternArbeidsforholdRef.ref(ARBEIDSFORHOLD_ID))
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(uttakResultatPeriode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttakResultatPeriode);

        return perioder;
    }
}
