package no.nav.foreldrepenger.datavarehus.xml.es;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.datavarehus.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@CdiDbAwareTest
class VedtakXmlTest {
    private static final String KLAGE_BEGRUNNELSE = "Begrunnelse for klagevurdering er bla.bla.bla.";
    private static final String BEHANDLENDE_ENHET_ID = "1234";

    private static final AktørId AKTØR_ID = AktørId.dummy();

    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private LegacyESBeregningRepository beregningRepository;
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsgrunnlagXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
    private VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
    private BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
    private PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
    private YtelseXmlTjeneste ytelseXmlTjeneste;

    @Inject
    private KlageRepository klageRepository;

    private FatteVedtakXmlTjeneste tjeneste;

    @BeforeEach
    public void oppsett(EntityManager em) {
        entityManager = em;
        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        beregningRepository = new LegacyESBeregningRepository(em);
        fagsakRepository = new FagsakRepository(em);

        var søknadRepository = mock(SøknadRepository.class);

        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        tjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste, new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
                behandlingsgrunnlagXmlTjeneste, skjæringstidspunktTjeneste);

        var søknad = new SøknadEntitet.Builder().medMottattDato(LocalDate.now()).medSøknadsdato(LocalDate.now()).build();
        lenient().when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));

    }

    private Fagsak opprettFagsak() {
        var søker = NavBruker.opprettNyNB(AKTØR_ID);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, søker);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    @Test
    void skal_opprette_xml_med_termindato() {

        var behandling = opprettBehandlingMedTermindato(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, true);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_termindato_avslag() {

        var behandling = opprettBehandlingMedTermindato(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, false);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_adopsjon() {

        var behandling = opprettBehandlingMedAdopsjon(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, true);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_fødsel() {

        var behandling = opprettBehandlingMedFødsel(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(behandling, true);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_klage_avvist() {
        // Arrange
        var behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forAvvistNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon()),
                null);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_klage_medhold() {
        // Arrange
        var behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forMedholdNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon()),
                KlageMedholdÅrsak.NYE_OPPLYSNINGER);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_klage_oppheve_ytelsesvedtak() {
        // Arrange
        var behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forOpphevetNK(ScenarioMorSøkerEngangsstønad.forAdopsjon()),
                KlageMedholdÅrsak.PROSESSUELL_FEIL);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    @Test
    void skal_opprette_xml_med_klage_stadfeste_ytelsesvedtak() {
        // Arrange
        var adopsjon = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        adopsjon.medSøknadHendelse()
                .medAdopsjon(adopsjon.medSøknadHendelse().getAdopsjonBuilder()
                        .medOmsorgsovertakelseDato(LocalDate.now().plusDays(40)))
                .medAntallBarn(3);
        var behandling = opprettKlageBehandling(ScenarioKlageEngangsstønad.forStadfestetNK(adopsjon), null);
        var xml = tjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();
    }

    private Behandling opprettKlageBehandling(ScenarioKlageEngangsstønad scenario, KlageMedholdÅrsak klageMedholdÅrsak) {
        var behandling = scenario.medKlageMedholdÅrsak(klageMedholdÅrsak)
                .medBegrunnelse(KLAGE_BEGRUNNELSE).medBehandlendeEnhet(BEHANDLENDE_ENHET_ID).lagre(repositoryProvider, klageRepository);

        var behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultat);
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        var beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Behandling opprettBehandlingMedTermindato(BehandlingStegType behandlingStegType) {
        var behandlingBuilder = Behandling.forFørstegangssøknad(opprettFagsak());

        var behandling = behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET_ID, null)).build();
        var behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        var beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        forceOppdaterBehandlingSteg(behandling, behandlingStegType);
        var fgRepo = repositoryProvider.getFamilieHendelseRepository();
        var søknadVersjon = fgRepo.opprettBuilderFor(behandling);
        søknadVersjon.medTerminbekreftelse(søknadVersjon.getTerminbekreftelseBuilder()
            .medNavnPå("Legen min")
            .medTermindato(LocalDate.now().plusDays(40))
            .medUtstedtDato(LocalDate.now())).medAntallBarn(1);
        fgRepo.lagre(behandling, søknadVersjon);
        var hendelseBuilder = fgRepo.opprettBuilderFor(behandling);
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                        .medTermindato(LocalDate.now().plusDays(40))
                        .medNavnPå("Legen min")
                        .medUtstedtDato(LocalDate.now().minusDays(7)))
                .medAntallBarn(1);
        fgRepo.lagre(behandling, hendelseBuilder);

        var søknad = new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        return behandling;
    }

    private void oppdaterMedBehandlingsresultat(Behandling behandling, boolean innvilget) {
        if (innvilget) {
            VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .medVilkårResultatType(VilkårResultatType.INNVILGET)
                .buildFor(behandling);
            var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
            LegacyESBeregningsresultat.builder()
                    .medBeregning(new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now()))
                    .buildFor(behandling, bres);
        } else {
            VilkårResultat.builder()
                .leggTilVilkårAvslått(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallMerknad.VM_1026)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
                .buildFor(behandling);
        }
    }

    private Behandling opprettBehandlingMedFødsel(BehandlingStegType stegType) {
        var fødselsdato = LocalDate.now().minusWeeks(2);
        var behandlingBuilder = Behandling.forFørstegangssøknad(opprettFagsak());

        var behandling = behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET_ID, null)).build();

        utførAksjonspunkt(behandling, SØKERS_OPPLYSNINGSPLIKT_MANU);
        utførAksjonspunkt(behandling, KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);

        forceOppdaterBehandlingSteg(behandling, stegType);
        var behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        var beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        var hendelseBuilder = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderFor(behandling)
            .medFødselsDato(fødselsdato)
            .medAntallBarn(1);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, hendelseBuilder);
        var builder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling).medFødselsDato(fødselsdato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, builder);

        var søknad = new SøknadEntitet.Builder().medSøknadsdato(LocalDate.now()).build();

        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        return behandling;
    }

    private Behandlingsresultat opprettBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        return Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType).buildFor(behandling);
    }

    private void opprettBehandlingsvedtak(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        var vedtak = BehandlingVedtak.builder()
                .medAnsvarligSaksbehandler("saksbehandler gundersen")
                .medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));
    }

    private void utførAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var aksjonspunkt1 = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt1, "");
    }

    private Behandling opprettBehandlingMedAdopsjon(BehandlingStegType stegType) {
        var fødselsdato = LocalDate.now().minusMonths(8);
        Map<Integer, LocalDate> map = new HashMap<>();
        map.put(1, fødselsdato);
        map.put(2, fødselsdato.minusYears(2));

        var behandlingBuilder = Behandling.forFørstegangssøknad(opprettFagsak());

        var behandling = behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET_ID, null)).build();
        var behandlingsresultat = opprettBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        opprettBehandlingsvedtak(behandling, behandlingsresultat);
        var beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));
        forceOppdaterBehandlingSteg(behandling, stegType);

        var søknadVersjon = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling);
        søknadVersjon
                .medAdopsjon(søknadVersjon.getAdopsjonBuilder()
                        .medOmsorgsovertakelseDato(LocalDate.now().plusDays(50)))
                .leggTilBarn(fødselsdato)
                .leggTilBarn(fødselsdato.minusYears(2))
                .medAntallBarn(2);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadVersjon);
        var hendelseBuilder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling);
        hendelseBuilder
                .medAdopsjon(hendelseBuilder.getAdopsjonBuilder()
                        .medAdoptererAlene(false)
                        .medErEktefellesBarn(false));
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, hendelseBuilder);

        var søknad = new SøknadEntitet.Builder().medSøknadsdato(LocalDate.now()).build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        return behandling;
    }

    @Test
    void testTilCalendar() {
        var localDate = LocalDate.of(2017, Month.APRIL, 18);
        var calendar = VedtakXmlUtil.tilCalendar(localDate);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2017);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(3);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(18);
    }
}
