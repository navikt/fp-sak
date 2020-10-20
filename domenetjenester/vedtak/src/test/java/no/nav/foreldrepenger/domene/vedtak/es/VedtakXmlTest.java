package no.nav.foreldrepenger.domene.vedtak.es;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.vedtak.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.YtelseXmlTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class VedtakXmlTest {
    private static final String KLAGE_BEGRUNNELSE = "Begrunnelse for klagevurdering er bla.bla.bla.";
    private static final String BEHANDLENDE_ENHET_ID = "1234";

    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private Repository repository = repoRule.getRepository();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();

    private LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(repoRule.getEntityManager());

    private FagsakRepository fagsakRepository = new FagsakRepository(entityManager);

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsgrunnlagXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef("ES")
    private VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef("ES")
    private BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef("ES")
    private PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef("ES")
    private YtelseXmlTjeneste ytelseXmlTjeneste;

    @Inject
    private KlageRepository klageRepository;

    private FatteVedtakXmlTjeneste tjeneste;


    @Before
    public void oppsett() {

        SøknadRepository søknadRepository = mock(SøknadRepository.class);

        VedtakXmlTjeneste vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        tjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste, new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
            behandlingsgrunnlagXmlTjeneste, skjæringstidspunktTjeneste);

        SøknadEntitet søknad = new SøknadEntitet.Builder().medMottattDato(LocalDate.now()).medSøknadsdato(LocalDate.now()).build();
        when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));

    }

    private Fagsak opprettFagsak() {
        NavBruker søker = NavBruker.opprettNyNB(AKTØR_ID);
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, søker);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    @Test
    public void skal_opprette_xml_med_termindato() {

        Behandling behandling = opprettBehandlingMedTermindato(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, true);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_termindato_avslag() {

        Behandling behandling = opprettBehandlingMedTermindato(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, false);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_adopsjon() {

        Behandling behandling = opprettBehandlingMedAdopsjon(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, true);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_fødsel() {

        Behandling behandling = opprettBehandlingMedFødsel(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, true);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_klage_avvist() {
        // Arrange
        Behandling behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forAvvistNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon()),
            null);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_klage_medhold() {
        // Arrange
        Behandling behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forMedholdNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon()),
        KlageMedholdÅrsak.NYE_OPPLYSNINGER);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_klage_oppheve_ytelsesvedtak() {
        // Arrange
        Behandling behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forOpphevetNK(ScenarioMorSøkerEngangsstønad.forAdopsjon()),
            KlageMedholdÅrsak.PROSESSUELL_FEIL);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    public void skal_opprette_xml_med_klage_stadfeste_ytelsesvedtak() {
        // Arrange
        final ScenarioMorSøkerEngangsstønad adopsjon = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        adopsjon.medSøknadHendelse()
            .medAdopsjon(adopsjon.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now().plusDays(40)))
            .medAntallBarn(3);
        Behandling behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forStadfestetNK(adopsjon), null);
        String xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    private Behandling opprettKlageBehandling(ScenarioKlageEngangsstønad scenario, KlageMedholdÅrsak klageMedholdÅrsak) {
        Behandling behandling = scenario.medKlageMedholdÅrsak(klageMedholdÅrsak)
            .medBegrunnelse(KLAGE_BEGRUNNELSE).medBehandlendeEnhet(BEHANDLENDE_ENHET_ID).lagre(repositoryProvider, klageRepository);

        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        repository.lagre(behandlingsresultat);
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        LegacyESBeregning beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Behandling opprettBehandlingMedTermindato(BehandlingStegType behandlingStegType) {
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(opprettFagsak());

        final Behandling behandling = behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET_ID, null)).build();
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        LegacyESBeregning beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        forceOppdaterBehandlingSteg(behandling, behandlingStegType);
        final FamilieHendelseRepository fgRepo = repositoryProvider.getFamilieHendelseRepository();
        final FamilieHendelseBuilder søknadVersjon = fgRepo.opprettBuilderFor(behandling);
        søknadVersjon
            .medTerminbekreftelse(søknadVersjon.getTerminbekreftelseBuilder()
                .medNavnPå("Legen min")
                .medTermindato(LocalDate.now().plusDays(40))
                .medUtstedtDato(LocalDate.now()))
            .medAntallBarn(1);
        fgRepo.lagre(behandling, søknadVersjon);
        final FamilieHendelseBuilder hendelseBuilder = fgRepo.opprettBuilderFor(behandling);
        hendelseBuilder
            .medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusDays(40))
                .medNavnPå("Legen min")
                .medUtstedtDato(LocalDate.now().minusDays(7)))
            .medAntallBarn(1);
        fgRepo.lagre(behandling, hendelseBuilder);

        SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        return behandling;
    }

    private void oppdaterMedBehandlingsresultat(Behandling behandling, boolean innvilget) {
        VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, innvilget ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT, null, new Properties(),
                null, false, false, null, null)
            .medVilkårResultatType(innvilget ? VilkårResultatType.INNVILGET : VilkårResultatType.AVSLÅTT)
            .buildFor(behandling);
        if (innvilget) {
            var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
            LegacyESBeregningsresultat.builder()
                .medBeregning(new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now()))
                .buildFor(behandling, bres);
        }
    }

    private Behandling opprettBehandlingMedFødsel(BehandlingStegType stegType) {
        LocalDate fødselsdato = LocalDate.now().minusWeeks(2);
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(opprettFagsak());

        final Behandling behandling = behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET_ID, null))
            .build();

        utførAksjonspunkt(behandling, SØKERS_OPPLYSNINGSPLIKT_MANU);
        utførAksjonspunkt(behandling, KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);

        forceOppdaterBehandlingSteg(behandling, stegType);
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        LegacyESBeregning beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        final FamilieHendelseBuilder hendelseBuilder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medFødselsDato(fødselsdato)
            .medAntallBarn(1);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, hendelseBuilder);
        final FamilieHendelseBuilder builder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medFødselsDato(fødselsdato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, builder);

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .build();

        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        return behandling;
    }

    private Behandlingsresultat opprettBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        return Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType).buildFor(behandling);
    }

    private void opprettBehandlingsvedtak(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler("saksbehandler gundersen")
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));
    }

    private void utførAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        Aksjonspunkt aksjonspunkt1 = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt1, "");
    }

    private Behandling opprettBehandlingMedAdopsjon(BehandlingStegType stegType) {
        LocalDate fødselsdato = LocalDate.now().minusMonths(8);
        Map<Integer, LocalDate> map = new HashMap<>();
        map.put(1, fødselsdato);
        map.put(2, fødselsdato.minusYears(2));

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(opprettFagsak());

        final Behandling behandling = behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET_ID, null))
            .build();
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        LegacyESBeregning beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        forceOppdaterBehandlingSteg(behandling, stegType);

        final FamilieHendelseBuilder søknadVersjon = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling);
        søknadVersjon
            .medAdopsjon(søknadVersjon.getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now().plusDays(50)))
            .leggTilBarn(fødselsdato)
            .leggTilBarn(fødselsdato.minusYears(2))
            .medAntallBarn(2);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadVersjon);
        final FamilieHendelseBuilder hendelseBuilder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling);
        hendelseBuilder
            .medAdopsjon(hendelseBuilder.getAdopsjonBuilder()
                .medAdoptererAlene(false)
                .medErEktefellesBarn(false))
            .leggTilBarn(fødselsdato).leggTilBarn(fødselsdato.minusYears(2));
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, hendelseBuilder);

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        return behandling;
    }

    @Test
    public void testTilCalendar() {
        LocalDate localDate = LocalDate.of(2017, Month.APRIL, 18);
        Calendar calendar = VedtakXmlUtil.tilCalendar(localDate);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2017);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(3);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(18);
    }
}
