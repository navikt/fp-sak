package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.FastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FastsettUttaksgrunnlagOgVurderSøknadsfristStegTest {

    private static final AktørId AKTØRID = AktørId.dummy();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider behandlingRepositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    @FagsakYtelseTypeRef("FP")
    private FastsettUttaksgrunnlagOgVurderSøknadsfristSteg fastsettUttaksgrunnlagOgVurderSøknadsfristSteg;

    private Behandling behandling;

    @Before
    public void setup() {
        Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AKTØRID)
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(PersonIdent.fra("123"))
            .medForetrukketSpråk(Språkkode.NB)
            .build();
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medSaksnummer(new Saksnummer("2"))
                .medBrukerPersonInfo(personinfo).build();

        behandlingRepositoryProvider.getFagsakRepository().opprettNy(fagsak);

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);

        behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler("VL");

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        var lås = behandlingRepositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());
        behandlingRepository.lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder().buildFor(behandlingsresultat);
        behandlingRepository.lagre(vilkårResultat, lås);
    }

    @Test
    public void skalOppretteAksjonspunktForÅVurdereSøknadsfristHvisSøktePerioderUtenforSøknadsfrist() {
        LocalDate mottattDato = LocalDate.now();
        LocalDate førsteUttaksdato = mottattDato.with(DAY_OF_MONTH, 1).minusMonths(3).minusDays(1); //En dag forbi søknadsfrist
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
            .build();

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Arrays.asList(periode1, periode2), true);
        ytelsesFordelingRepository.lagre(behandlingId, fordeling);

        final SøknadEntitet søknad = opprettSøknad(førsteUttaksdato, mottattDato);
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        Fagsak fagsak = behandling.getFagsak();
        // Act
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        BehandleStegResultat behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = lagretBehandling.getBehandlingsresultat().getGjeldendeUttaksperiodegrense();
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(førsteUttaksdato.plusDays(1));
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    @Test
    public void skalIkkeOppretteAksjonspunktHvisSøktePerioderInnenforSøknadsfrist() {
        LocalDate førsteUttaksdato = LocalDate.now().with(DAY_OF_MONTH, 1).minusMonths(3);
        LocalDate mottattDato = LocalDate.now();
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
            .build();

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Arrays.asList(periode1, periode2), true);
        ytelsesFordelingRepository.lagre(behandlingId, fordeling);

        final SøknadEntitet søknad = opprettSøknad(førsteUttaksdato, mottattDato);
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        Fagsak fagsak = behandling.getFagsak();
        // Act
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        BehandleStegResultat behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = lagretBehandling.getBehandlingsresultat().getGjeldendeUttaksperiodegrense();
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(førsteUttaksdato);
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    private SøknadEntitet opprettSøknad(LocalDate fødselsdato, LocalDate mottattDato) {
        final FamilieHendelseBuilder søknadHendelse = behandlingRepositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato);
        behandlingRepositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);

        return new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(mottattDato)
            .medElektroniskRegistrert(true)
            .build();
    }


}
