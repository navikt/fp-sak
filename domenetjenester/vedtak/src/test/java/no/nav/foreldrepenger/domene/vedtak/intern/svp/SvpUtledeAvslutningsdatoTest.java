package no.nav.foreldrepenger.domene.vedtak.intern.svp;

import static no.nav.foreldrepenger.domene.vedtak.intern.svp.SvpUtledeAvslutningsdato.PADDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.svp.MaksDatoUttakTjenesteImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SvpUtledeAvslutningsdatoTest {
    private SvpUtledeAvslutningsdato utledeAvslutningsdato;
    private static final int KLAGEFRIST_I_MÅNEDER = 3;

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private SvangerskapspengerUttakResultatRepository svpUttakRepository;
    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        var maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(svpUttakRepository);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);

        utledeAvslutningsdato = new SvpUtledeAvslutningsdato(repositoryProvider,
            uttakInputTjeneste, maksDatoUttakTjeneste);

        behandling = lagBehandling();
        fagsak = behandling.getFagsak();

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(new UttakInput(BehandlingReferanse.fra(behandling), null, null, new SvangerskapspengerGrunnlag()));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().plusDays(40));
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        return scenario.lagMocked();
    }

    @Test
    void finner_avslutningsdato_fra_sisteuttaksdato(){
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        var behandlingsresultat = lagBehandlingsresultat(behandling);
        var sisteUttaksdato = LocalDate.now().plusDays(10);
        when(svpUttakRepository.hentHvisEksisterer(behandling.getId())).thenReturn(lagUttakMedEnGyldigPeriode(LocalDate.now().minusDays(10), sisteUttaksdato, behandlingsresultat));

        // Act
        var avslutningdato = utledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(),fagsakRelasjon);
        var forventetMin = sisteUttaksdato.plusDays(1).plusMonths(KLAGEFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth());
        var forventetMax = forventetMin.plusDays(PADDING-1);

        // Assert
        assertThat(avslutningdato).isBetween(forventetMin, forventetMax);

    }

    @Test
    void finner_ingen_sisteuttaksdato_for_avslutning(){
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        var behandlingsresultat = lagBehandlingsresultat(behandling);

        var sisteUttaksdato = LocalDate.now().plusDays(10);
        when(svpUttakRepository.hentHvisEksisterer(behandling.getId())).thenReturn(lagUttakMedEnUgyldigPeriode(LocalDate.now().minusDays(10), sisteUttaksdato, behandlingsresultat));

        // Act
        var avslutningdato = utledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(),fagsakRelasjon);

        // Assert
        assertThat(avslutningdato).isEqualTo(LocalDate.now().plusDays(1));

    }

    private Behandlingsresultat lagBehandlingsresultat(Behandling behandling) {
        return Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.UDEFINERT)
            .buildFor(behandling);
    }

    private Optional<SvangerskapspengerUttakResultatEntitet> lagUttakMedEnGyldigPeriode(LocalDate fom,
                                                                                        LocalDate tom,
                                                                                        Behandlingsresultat br) {
        var periode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .build();
        var arbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medPeriode(periode)
            .build();
        var entitet = new SvangerskapspengerUttakResultatEntitet.Builder(br)
            .medUttakResultatArbeidsforhold(arbeidsforhold).build();
        return Optional.of(entitet);
    }

    private Optional<SvangerskapspengerUttakResultatEntitet> lagUttakMedEnUgyldigPeriode(LocalDate fom,
                                                                                         LocalDate tom,
                                                                                         Behandlingsresultat br) {
        var periode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();
        var arbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medPeriode(periode)
            .build();
        var entitet = new SvangerskapspengerUttakResultatEntitet.Builder(br)
            .medUttakResultatArbeidsforhold(arbeidsforhold).build();
        return Optional.of(entitet);
    }
}
