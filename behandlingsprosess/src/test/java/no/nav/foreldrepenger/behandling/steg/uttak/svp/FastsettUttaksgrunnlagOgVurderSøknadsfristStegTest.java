package no.nav.foreldrepenger.behandling.steg.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp.FastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp.SøktPeriodeTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.svp.FørsteLovligeUttaksdatoTjeneste;
import no.nav.foreldrepenger.domene.uttak.svp.RegelmodellSøknaderMapper;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class FastsettUttaksgrunnlagOgVurderSøknadsfristStegTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();


    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste;

    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    private Behandling behandling;
    private SvpHelper svpHelper;

    private FastsettUttaksgrunnlagOgVurderSøknadsfristSteg fastsettUttaksgrunnlagOgVurderSøknadsfristSteg;

    @Before
    public void setup() {

        svpHelper = new SvpHelper(repositoryProvider);
        behandling = svpHelper.lagreBehandling();
        repository.flushAndClear();

        RegelmodellSøknaderMapper regelmodellSøknaderMapper = new RegelmodellSøknaderMapper();
        SøktPeriodeTjeneste søktPeriodeTjeneste = new SøktPeriodeTjenesteImpl(regelmodellSøknaderMapper);
        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(repositoryProvider, uttakInputTjeneste, søktPeriodeTjeneste, førsteLovligeUttaksdatoTjeneste);
    }

    @Test
    public void ingen_aksjonspunkt_når_søkt_i_tide() {
        var jordsmorsdato = LocalDate.of(2019, Month.MAY, 5);
        var mottatdato = jordsmorsdato;
        svpHelper.lagreTerminbekreftelse(behandling, LocalDate.of(2019, Month.JULY, 1));
        svpHelper.lagreIngenTilrettelegging(behandling, jordsmorsdato);
        var søknad = opprettSøknad(jordsmorsdato, mottatdato);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        repository.flushAndClear();
        var fagsak = behandling.getFagsak();

        // Act
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        BehandleStegResultat behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        repository.flushAndClear();

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(0);

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandling.getId());
        Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = lagretBehandling.getBehandlingsresultat().getGjeldendeUttaksperiodegrense();
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottatdato);
    }

    @Test
    public void aksjonspunkt_når_søkt_for_sent() {
        var jordsmorsdato = LocalDate.of(2019, Month.MAY, 5);
        var mottatdato = LocalDate.of(2019, Month.SEPTEMBER, 3);
        svpHelper.lagreTerminbekreftelse(behandling, LocalDate.of(2019, Month.JULY, 1));
        svpHelper.lagreIngenTilrettelegging(behandling, jordsmorsdato);
        var søknad = opprettSøknad(jordsmorsdato, mottatdato);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        repository.flushAndClear();
        var fagsak = behandling.getFagsak();

        // Act
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        BehandleStegResultat behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        repository.flushAndClear();

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandling.getId());
        Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = lagretBehandling.getBehandlingsresultat().getGjeldendeUttaksperiodegrense();
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(LocalDate.of(2019, Month.JUNE, 1));
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottatdato);
    }

    private SøknadEntitet opprettSøknad(LocalDate fødselsdato, LocalDate mottattDato) {
        final FamilieHendelseBuilder søknadHendelse = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);

                return new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(mottattDato)
            .medElektroniskRegistrert(true)
            .build();
    }

}
