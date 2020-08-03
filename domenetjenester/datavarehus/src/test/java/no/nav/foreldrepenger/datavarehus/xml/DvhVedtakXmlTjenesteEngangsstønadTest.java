package no.nav.foreldrepenger.datavarehus.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.es.DvhPersonopplysningXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.OppdragXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class DvhVedtakXmlTjenesteEngangsstønadTest {
    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER = new Saksnummer("12345");
    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();
    private static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    private static final String ANSVARLIG_SAKSBEHANDLER = "fornavn etternavn";
    private static final Long OPPDRAG_FAGSYSTEM_ID = 44L;
    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);
    private static LocalDateTime VEDTAK_DATO = LocalDateTime.parse("2017-10-11T08:00");
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    @Mock
    private TpsTjeneste tpsTjeneste;
    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;
    private Repository repository = repoRule.getRepository();

    private DvhVedtakXmlTjeneste dvhVedtakXmlTjenesteES;

    private DvhPersonopplysningXmlTjenesteImpl personopplysningXmlTjenesteEngangsstønad;
    private VedtakXmlTjeneste vedtakXmlTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();

    @Inject
    private ØkonomioppdragRepository økonomioppdragRepository;

    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjeneste;

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private MedlemskapRepository medlemskapRepository;

    @Inject
    private KodeverkRepository kodeverkRepository;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Before
    public void oppsett() {
        HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering = new HentOppdragMedPositivKvittering(økonomioppdragRepository);
        vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        var poXmlFelles = new PersonopplysningXmlFelles(tpsTjeneste, kodeverkRepository);
        personopplysningXmlTjenesteEngangsstønad =
                new DvhPersonopplysningXmlTjenesteImpl(poXmlFelles,
                    familieHendelseRepository,
                    vergeRepository,
                    medlemskapRepository,
                    personopplysningTjeneste, iayTjeneste);
        OppdragXmlTjenesteImpl oppdragXmlTjenesteImpl = new OppdragXmlTjenesteImpl(hentOppdragMedPositivKvittering);
        dvhVedtakXmlTjenesteES = new DvhVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste, new UnitTestLookupInstanceImpl<>(personopplysningXmlTjenesteEngangsstønad),
            new UnitTestLookupInstanceImpl<>(oppdragXmlTjenesteImpl), behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
    }

    @Test
    public void skal_opprette_vedtaks_xml_med_oppdrag() {
        Behandling behandling = byggFødselBehandlingMedVedtak(true);
        Long delytelseId = 65L;
        String delytelseXmlElement = String.format("delytelseId>%s</", delytelseId);
        String fagsystemIdXmlElement = String.format("fagsystemId>%s</", OPPDRAG_FAGSYSTEM_ID);
        buildOppdragskontroll(behandling.getId(), delytelseId);

        // Act
        String xml = dvhVedtakXmlTjenesteES.opprettDvhVedtakXml(behandling.getId());

        // Assert
        assertNotNull(xml);
        assertThat(xml).contains(delytelseXmlElement);
        assertThat(xml).contains(fagsystemIdXmlElement);
        assertPersonopplysningDvh(BRUKER_AKTØR_ID, xml);
    }

    @Test
    public void skal_opprette_vedtaks_xml_innvilget_uten_oppdrag() {
        Behandling behandling = byggFødselBehandlingMedVedtak(true);
        String delytelseXmlElement = "delytelseId>";

        // Act
        String xml = dvhVedtakXmlTjenesteES.opprettDvhVedtakXml(behandling.getId());

        // Assert
        assertNotNull(xml);
        assertThat(xml).doesNotContain(delytelseXmlElement);
        assertPersonopplysningDvh(BRUKER_AKTØR_ID, xml);
    }

    @Test
    public void skal_opprette_vedtaks_xml_avslag_uten_oppdrag() {
        Behandling behandling = byggFødselBehandlingMedVedtak(false);
        String delytelseXmlElement = "delytelseId>";

        // Act
        String xml = dvhVedtakXmlTjenesteES.opprettDvhVedtakXml(behandling.getId());

        // Assert
        assertNotNull(xml);
        assertThat(xml).doesNotContain(delytelseXmlElement);
        assertPersonopplysningDvh(BRUKER_AKTØR_ID, xml);
    }

    @Test
    public void skal_opprette_vedtaks_xml_adopsjon() {
        Behandling behandling = byggAdopsjonMedVedtak(true);
        String adopsjonXmlElement = "adopsjon>";

        // Act
        String xml = dvhVedtakXmlTjenesteES.opprettDvhVedtakXml(behandling.getId());

        // Assert
        assertNotNull(xml);
        assertThat(xml).contains(adopsjonXmlElement);
        assertPersonopplysningDvh(BRUKER_AKTØR_ID, xml);
    }

    /**
     * Personopplyusning for vedtaks xml til datavarehus skal ikke inneholde fødselsnummer. Men istedenfor aktørId.
     */
    private void assertPersonopplysningDvh(AktørId aktørId, String vedtaksXml) {
        String aktørIdXmlElement = String.format("aktoerId>%s</", aktørId.getId());
        String fødselsnummerXmlElement = "norskIdent>";
        assertThat(vedtaksXml).contains(aktørIdXmlElement);
        assertThat(vedtaksXml).doesNotContain(fødselsnummerXmlElement);
    }

    private Behandling byggAdopsjonMedVedtak(boolean innvilget) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID);

        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now().plusDays(50)))
            .leggTilBarn(FØDSELSDATO_BARN)
            .medAntallBarn(1);

        return lagreBehandlingOgVedtak(innvilget, scenario);

    }

    private Behandling byggFødselBehandlingMedVedtak(boolean innvilget) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID);
        scenario.medSøknadHendelse()
            .medFødselsDato(FØDSELSDATO_BARN);

        return lagreBehandlingOgVedtak(innvilget, scenario);
    }

    private Behandling lagreBehandlingOgVedtak(boolean innvilget, ScenarioMorSøkerEngangsstønad scenario) {
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(innvilget ? VedtakResultatType.INNVILGET : VedtakResultatType.AVSLAG)
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));

        oppdaterMedBehandlingsresultat(behandling, innvilget);

        return behandling;
    }

    private void oppdaterMedBehandlingsresultat(Behandling behandling, boolean innvilget) {
        VilkårResultat vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, innvilget ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT, null, new Properties(), null, false, false, null, null)
            .medVilkårResultatType(innvilget ? VilkårResultatType.INNVILGET : VilkårResultatType.AVSLÅTT)
            .buildFor(behandling);
        repository.lagre(vilkårResultat);
        if (innvilget) {
            var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
            LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now()))
                .buildFor(behandling, bres);
            repository.lagre(beregningResultat);
        }
    }

    private void buildOppdragskontroll(Long behandlingId, Long delytelseId) {
        Oppdragskontroll oppdrag = Oppdragskontroll.builder()
            .medBehandlingId(behandlingId)
            .medSaksnummer(SAKSNUMMER)
            .medVenterKvittering(false)
            .medProsessTaskId(56L)
            .build();

        Avstemming115 avstemming115 = buildAvstemming115();
        Oppdrag110 oppdrag110 = buildOppdrag110(oppdrag, avstemming115);
        buildOppdragslinje150(oppdrag110, delytelseId);
        buildOppdragKvittering(oppdrag110);

        økonomioppdragRepository.lagre(oppdrag);
    }

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110, Long delytelseId) {

        return Oppdragslinje150.builder()
            .medKodeEndringLinje("ENDR")
            .medKodeStatusLinje("OPPH")
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(delytelseId)
            .medKodeKlassifik("FPENFOD-OP")
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(61122L)
            .medFradragTillegg(TfradragTillegg.F.value())
            .medTypeSats(ØkonomiTypeSats.UKE.name())
            .medBrukKjoreplan("B")
            .medSaksbehId("F2365245")
            .medUtbetalesTilId("123456789")
            .medOppdrag110(oppdrag110)
            .medHenvisning(43L)
            .medRefDelytelseId(1L)
            .build();
    }

    private OppdragKvittering buildOppdragKvittering(Oppdrag110 oppdrag110) {
        return OppdragKvittering.builder().medOppdrag110(oppdrag110)
            .medAlvorlighetsgrad("00")
            .build();
    }

    private Avstemming115 buildAvstemming115() {
        return Avstemming115.builder()
            .medKodekomponent(Fagsystem.FPSAK.getOffisiellKode())
            .medNokkelAvstemming("nokkelAvstemming")
            .medTidspnktMelding("tidspnktMelding")
            .build();
    }

    private Oppdrag110 buildOppdrag110(Oppdragskontroll oppdragskontroll, Avstemming115 avstemming115) {
        return Oppdrag110.builder()
            .medKodeAksjon(ØkonomiKodeAksjon.TRE.getKodeAksjon())
            .medKodeEndring(ØkonomiKodeEndring.NY.name())
            .medKodeFagomrade(ØkonomiKodeFagområde.REFUTG.name())
            .medFagSystemId(OPPDRAG_FAGSYSTEM_ID)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.DAG.getUtbetFrekvens())
            .medOppdragGjelderId("12345678901")
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId("J5624215")
            .medAvstemming115(avstemming115)
            .medOppdragskontroll(oppdragskontroll)
            .build();
    }
}

