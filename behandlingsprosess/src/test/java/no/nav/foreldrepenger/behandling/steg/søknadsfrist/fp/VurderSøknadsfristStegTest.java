package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class VurderSøknadsfristStegTest extends EntityManagerAwareTest {

    private static final AktørId AKTØRID = AktørId.dummy();

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    private VurderSøknadsfristSteg fastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
    private FamilieHendelseRepository familieHendelseRepository;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var vurderSøknadsfristTjeneste = new VurderSøknadsfristTjeneste(behandlingRepositoryProvider);
        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new VurderSøknadsfristSteg(vurderSøknadsfristTjeneste);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        var fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medSaksnummer(new Saksnummer("29999"))
                .medBrukerAktørId(AKTØRID).build();

        behandlingRepositoryProvider.getFagsakRepository().opprettNy(fagsak);

        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);

        var behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler("VL");

        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        var lås = behandlingRepositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());
        behandlingRepository.lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder().buildFor(behandlingsresultat);
        behandlingRepository.lagre(vilkårResultat, lås);

        return behandling;
    }

    @Test
    void skalOppretteAksjonspunktForÅVurdereSøknadsfristHvisSøktePerioderUtenforSøknadsfrist() {
        var behandling = opprettBehandling();
        var mottattDato = LocalDate.now();
        var førsteUttaksdato = mottattDato.with(DAY_OF_MONTH, 1).minusMonths(3).minusDays(1); // En dag forbi søknadsfrist
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
                .build();

        var periode2 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
                .build();

        var dekningsgrad = Dekningsgrad._100;
        var behandlingId = behandling.getId();

        var fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittFordeling(fordeling)
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());

        var søknad = opprettSøknad(førsteUttaksdato, mottattDato, behandling.getId());
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        var fagsak = behandling.getFagsak();
        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(),
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);

        var gjeldendeUttaksperiodegrense = behandlingRepositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandlingId);
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    @Test
    void skalIkkeOppretteAksjonspunktHvisSøktePerioderInnenforSøknadsfrist() {
        var behandling = opprettBehandling();
        var førsteUttaksdato = LocalDate.now().with(DAY_OF_MONTH, 1).minusMonths(3);
        var mottattDato = LocalDate.now();
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
                .build();

        var periode2 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
                .build();

        var dekningsgrad = Dekningsgrad._100;
        var behandlingId = behandling.getId();

        var fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittFordeling(fordeling)
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());

        var søknad = opprettSøknad(førsteUttaksdato, mottattDato, behandling.getId());
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        var fagsak = behandling.getFagsak();
        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(),
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        var gjeldendeUttaksperiodegrense = behandlingRepositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandlingId);
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    private SøknadEntitet opprettSøknad(LocalDate fødselsdato, LocalDate mottattDato, Long behandlingId) {
        var søknadHendelse = familieHendelseRepository.opprettBuilderFor(behandlingId).medAntallBarn(1).medFødselsDato(fødselsdato);
        familieHendelseRepository.lagre(behandlingId, søknadHendelse);

        return new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .medMottattDato(mottattDato)
                .medElektroniskRegistrert(true)
                .build();
    }

}
