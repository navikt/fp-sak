package no.nav.foreldrepenger.behandling.steg.vedtak.es;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FORESLÅ_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.vedtak.BehandlingVedtakTjeneste;
import no.nav.foreldrepenger.behandling.steg.vedtak.FatteVedtakSteg;
import no.nav.foreldrepenger.behandling.steg.vedtak.FatteVedtakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.es.BeregningsgrunnlagXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.BeregningsresultatXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.PersonopplysningXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.VilkårsgrunnlagXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.YtelseXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.BeregningsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.YtelseXmlTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.økonomi.simulering.tjeneste.SimulerInntrekkSjekkeTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class FatteVedtakStegImplTest {

    private static final String BEHANDLENDE_ENHET = "Stord";
    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    private LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(entityManager);
    private final AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private final InternalManipulerBehandling manipulerBehandling = new InternalManipulerBehandling();

    @Inject
    private InnsynRepository innsynRepository;
    @Inject
    private KlageRepository klageRepository;
    @Inject
    private AnkeRepository ankeRepository;

    private FatteVedtakSteg fatteVedtakSteg;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;

    private KompletthetsjekkerProvider kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);

    @Before
    public void oppsett() {
        // TODO (Fluoritt): Fin blanding av entityManager og mocks her.... kan antagelig erstatte de som bruker entitymanager med de som lages fra AbstractTestScenario?
        LagretVedtakRepository vedtakRepository = new LagretVedtakRepository(entityManager);
        HistorikkRepository historikkRepository = new HistorikkRepository(entityManager);

        OppgaveTjeneste oppgaveTjeneste = mock(OppgaveTjeneste.class);
        TpsTjeneste tpsTjeneste = Mockito.mock(TpsTjeneste.class);
        PersonopplysningTjeneste personopplysningTjeneste = Mockito.mock(PersonopplysningTjeneste.class);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        VedtakXmlTjeneste vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        var poXmlFelles = new PersonopplysningXmlFelles(tpsTjeneste);
        PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(
            poXmlFelles, repositoryProvider, personopplysningTjeneste, iayTjeneste, mock(VergeRepository.class));
        VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste = new VilkårsgrunnlagXmlTjenesteImpl(repositoryProvider, kompletthetssjekkerProvider, skjæringstidspunktTjeneste);
        YtelseXmlTjeneste ytelseXmlTjeneste = new YtelseXmlTjenesteImpl(beregningRepository);
        BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste = new BeregningsgrunnlagXmlTjenesteImpl(beregningRepository);
        BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste = new BeregningsresultatXmlTjenesteImpl(beregningsgrunnlagXmlTjeneste, ytelseXmlTjeneste);
        var behandlingsresultatXmlTjeneste = nyBeregningsresultatXmlTjeneste(vilkårsgrunnlagXmlTjeneste, beregningsresultatXmlTjeneste);

        TotrinnTjeneste totrinnTjeneste = mock(TotrinnTjeneste.class);

        FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
            new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
            behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
        VedtakTjeneste vedtakTjeneste = new VedtakTjeneste(null, repositoryProvider, klageRepository, mock(TotrinnTjeneste.class), innsynRepository, ankeRepository);

        BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer = mock(BehandlingVedtakEventPubliserer.class);

        behandlingVedtakTjeneste = new BehandlingVedtakTjeneste(behandlingVedtakEventPubliserer, repositoryProvider);
        FatteVedtakTjeneste fvtei = new FatteVedtakTjeneste(vedtakRepository, fpSakVedtakXmlTjeneste, vedtakTjeneste,
            oppgaveTjeneste, totrinnTjeneste, behandlingVedtakTjeneste);
        var simuler = new SimulerInntrekkSjekkeTjeneste(null, null, null, null);
        fatteVedtakSteg = new FatteVedtakSteg(repositoryProvider, fvtei, simuler);
    }

    private BehandlingsresultatXmlTjeneste nyBeregningsresultatXmlTjeneste(VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste,
                                                     BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste) {
        var behandlingsresultatXmlTjeneste = new BehandlingsresultatXmlTjeneste(
            new UnitTestLookupInstanceImpl<>(beregningsresultatXmlTjeneste),
            new UnitTestLookupInstanceImpl<>(vilkårsgrunnlagXmlTjeneste),
            behandlingVedtakRepository,
            klageRepository,
            ankeRepository,
            new VilkårResultatRepository(entityManager));
        return behandlingsresultatXmlTjeneste;
    }

    @Test(expected = TekniskException.class)
    public void skal_feile_hvis_behandling_i_feil_tilstand() {
        // Arrange
        int antallBarn = 1;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        // Act
        fatteVedtakSteg.utførSteg(kontekst);
    }

    @Test
    public void skal_fatte_positivt_vedtak() {
        // Arrange
        int antallBarn = 2;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        // Act
        fatteVedtakSteg.utførSteg(kontekst);

        // Assert
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(kontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void revurdering_med_endret_utfall_skal_ha_nytt_vedtak() {
        // Opprinnelig behandling med vedtak
        int antallBarn = 1;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);
        oppdaterMedVedtak(kontekst);
        Behandling originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId())).build();

        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, false, antallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isFalse();
    }

    @Test
    public void revurdering_med_endret_antall_barn_skal_ha_nytt_vedtak() {
        int originalAntallBarn = 1;
        int faktiskAntallBarn = 2;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(originalAntallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, originalAntallBarn);
        oppdaterMedVedtak(kontekst);
        Behandling originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId())).build();

        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, true, faktiskAntallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isFalse();
    }

    @Test
    public void revurdering_med_samme_utfall_innvilget_skal_ha_beslutning() {
        int antallBarn = 1;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);
        oppdaterMedVedtak(kontekst);
        Behandling originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId())).build();

        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);

        BehandlingLås behandlingLås = lagreBehandling(revurdering);

        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, true, antallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isTrue();
    }

    private void opprettFamilieHendelseGrunnlag(Behandling originalBehandling, Behandling revurdering) {
        repositoryProvider.getFamilieHendelseRepository().kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
    }

    private BehandlingLås lagreBehandling(Behandling behandling) {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandlingLås;
    }

    @Test
    public void revurdering_med_samme_utfall_avslag_skal_ha_beslutning() {
        int antallBarn = 1;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, false, antallBarn);
        oppdaterMedVedtak(kontekst);
        Behandling originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId())).build();

        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, false, antallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isTrue();
    }

    @Test
    public void skal_lukke_godkjent_aksjonspunkter_og_sette_steg_til_utført() {
        // Arrange
        LagretVedtakRepository vedtakRepository = new LagretVedtakRepository(entityManager);
        HistorikkRepository historikkRepository = new HistorikkRepository(entityManager);

        OppgaveTjeneste oppgaveTjeneste = mock(OppgaveTjeneste.class);
        SøknadRepository søknadRepository = mock(SøknadRepository.class);
        TpsTjeneste tpsTjeneste = Mockito.mock(TpsTjeneste.class);
        PersonopplysningTjeneste personopplysningTjeneste = Mockito.mock(PersonopplysningTjeneste.class);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        var poXmlFelles = new PersonopplysningXmlFelles(tpsTjeneste);
        PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider,
            personopplysningTjeneste, iayTjeneste, mock(VergeRepository.class));
        VedtakXmlTjeneste vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste = new VilkårsgrunnlagXmlTjenesteImpl(repositoryProvider, kompletthetssjekkerProvider,
            skjæringstidspunktTjeneste);
        YtelseXmlTjeneste ytelseXmlTjeneste = new YtelseXmlTjenesteImpl(beregningRepository);
        BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste = new BeregningsgrunnlagXmlTjenesteImpl(beregningRepository);
        BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste = new BeregningsresultatXmlTjenesteImpl(beregningsgrunnlagXmlTjeneste,
            ytelseXmlTjeneste);
        var behandlingsresultatXmlTjeneste = nyBeregningsresultatXmlTjeneste(vilkårsgrunnlagXmlTjeneste, beregningsresultatXmlTjeneste);
        TotrinnTjeneste totrinnTjeneste = mock(TotrinnTjeneste.class);

        SøknadEntitet søknad = new SøknadEntitet.Builder().medMottattDato(LocalDate.now()).medSøknadsdato(LocalDate.now()).build();
        when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));

        FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
            new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
            behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
        VedtakTjeneste vedtakTjeneste = new VedtakTjeneste(null, repositoryProvider, klageRepository, mock(TotrinnTjeneste.class), innsynRepository,
            ankeRepository);

        int antallBarn = 2;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, List.of(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL));
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setToTrinnsBehandling();

        // Legg til data i totrinsvurdering.
        Totrinnsvurdering.Builder vurdering = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        Totrinnsvurdering ttvurdering = vurdering.medGodkjent(true).medBegrunnelse("").build();

        List<Totrinnsvurdering> totrinnsvurderings = new ArrayList<>();
        totrinnsvurderings.add(ttvurdering);
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling)).thenReturn(totrinnsvurderings);

        FatteVedtakTjeneste fvtei = new FatteVedtakTjeneste(vedtakRepository, fpSakVedtakXmlTjeneste, vedtakTjeneste,
            oppgaveTjeneste, totrinnTjeneste, behandlingVedtakTjeneste);

        var simuler = new SimulerInntrekkSjekkeTjeneste(null, null, null, null);
        fatteVedtakSteg = new FatteVedtakSteg(repositoryProvider, fvtei, simuler);
        aksjonspunktRepository.setToTrinnsBehandlingKreves(behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL));

        BehandleStegResultat behandleStegResultat = fatteVedtakSteg.utførSteg(kontekst);

        List<AksjonspunktDefinisjon> aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunktListe).isEmpty();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
    }

    @Test
    public void tilbakefører_og_reåpner_aksjonspunkt_når_totrinnskontroll_ikke_godkjent() {
        LagretVedtakRepository vedtakRepository = new LagretVedtakRepository(entityManager);
        HistorikkRepository historikkRepository = new HistorikkRepository(entityManager);

        OppgaveTjeneste oppgaveTjeneste = mock(OppgaveTjeneste.class);
        SøknadRepository søknadRepository = mock(SøknadRepository.class);
        TpsTjeneste tpsTjeneste = Mockito.mock(TpsTjeneste.class);
        PersonopplysningTjeneste personopplysningTjeneste = Mockito.mock(PersonopplysningTjeneste.class);
        VedtakXmlTjeneste vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        var poXmlFelles = new PersonopplysningXmlFelles(tpsTjeneste);
        PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider,
            personopplysningTjeneste, iayTjeneste, mock(VergeRepository.class));
        VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste = new VilkårsgrunnlagXmlTjenesteImpl(repositoryProvider, kompletthetssjekkerProvider,
            skjæringstidspunktTjeneste);
        YtelseXmlTjeneste ytelseXmlTjeneste = new YtelseXmlTjenesteImpl(beregningRepository);
        BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste = new BeregningsgrunnlagXmlTjenesteImpl(beregningRepository);
        BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste = new BeregningsresultatXmlTjenesteImpl(beregningsgrunnlagXmlTjeneste,
            ytelseXmlTjeneste);
        var behandlingsresultatXmlTjeneste = nyBeregningsresultatXmlTjeneste(vilkårsgrunnlagXmlTjeneste, beregningsresultatXmlTjeneste);

        TotrinnTjeneste totrinnTjeneste = mock(TotrinnTjeneste.class);

        SøknadEntitet søknad = new SøknadEntitet.Builder().medMottattDato(LocalDate.now()).medSøknadsdato(LocalDate.now()).build();
        when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));

        FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
            new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
            behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
        VedtakTjeneste vedtakTjeneste = new VedtakTjeneste(null, repositoryProvider, klageRepository, mock(TotrinnTjeneste.class), innsynRepository,
            ankeRepository);

        int antallBarn = 2;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, List.of(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET, AksjonspunktDefinisjon.FORESLÅ_VEDTAK));
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setToTrinnsBehandling();

        aksjonspunktRepository.setToTrinnsBehandlingKreves(behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL));

        // Legg til data i totrinsvurdering.
        Totrinnsvurdering.Builder vurdering = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        Totrinnsvurdering vurderesPåNytt = vurdering.medGodkjent(false).medBegrunnelse("Må vurderes på nytt").medVurderÅrsak(VurderÅrsak.FEIL_LOV).build();

        Totrinnsvurdering.Builder vurdering2 = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        Totrinnsvurdering vurderesOk = vurdering2.medGodkjent(true).medBegrunnelse("").build();

        List<Totrinnsvurdering> totrinnsvurderings = new ArrayList<>();
        totrinnsvurderings.add(vurderesPåNytt);
        totrinnsvurderings.add(vurderesOk);
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling)).thenReturn(totrinnsvurderings);

        FatteVedtakTjeneste fvtei = new FatteVedtakTjeneste(vedtakRepository, fpSakVedtakXmlTjeneste, vedtakTjeneste,
            oppgaveTjeneste, totrinnTjeneste, behandlingVedtakTjeneste);

        var simuler = new SimulerInntrekkSjekkeTjeneste(null, null, null, null);
        fatteVedtakSteg = new FatteVedtakSteg(repositoryProvider, fvtei, simuler);

        BehandleStegResultat behandleStegResultat = fatteVedtakSteg.utførSteg(kontekst);

        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT);

        behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Optional<Aksjonspunkt> oppdatertAvklarFødsel = behandling.getAksjonspunktMedDefinisjonOptional(SJEKK_MANGLENDE_FØDSEL);
        assertThat(oppdatertAvklarFødsel).isPresent();
        assertThat(oppdatertAvklarFødsel.get().getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        Optional<Aksjonspunkt> oppdatertForeslåVedtak = behandling.getAksjonspunktMedDefinisjonOptional(FORESLÅ_VEDTAK);
        assertThat(oppdatertForeslåVedtak).isPresent();
        assertThat(oppdatertForeslåVedtak.get().getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        Optional<Aksjonspunkt> oppdatertSøknFristVilkåret = behandling.getAksjonspunktMedDefinisjonOptional(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        assertThat(oppdatertSøknFristVilkåret).isPresent();
    }

    @Test
    public void skal_fatte_negativt_vedtak() {
        // Arrange
        int antallBarn = 1;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, false, antallBarn);

        // Act
        fatteVedtakSteg.utførSteg(kontekst);

        // Assert
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(kontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
    }

    private BehandlingskontrollKontekst byggBehandlingsgrunnlagForFødsel(int antallBarn, BehandlingStegType behandlingStegType, List<AksjonspunktDefinisjon> aksjonspunkter) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBekreftetHendelse().medFødselsDato(SKJÆRINGSTIDSPUNKT)
            .medAntallBarn(antallBarn);
        aksjonspunkter.forEach(apd -> scenario.leggTilAksjonspunkt(apd, BehandlingStegType.KONTROLLER_FAKTA));

        Behandling behandling = scenario
            .medBehandlingStegStart(behandlingStegType)
            .medBehandlendeEnhet(BEHANDLENDE_ENHET)
            .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagre(repositoryProvider);
        LegacyESBeregning beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));

        Fagsak fagsak = behandling.getFagsak();
        return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

    private void oppdaterMedVedtak(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        VedtakResultatType vedtakResultatType = behandlingsresultat.getBehandlingResultatType()
            .equals(BehandlingResultatType.INNVILGET) ? VedtakResultatType.INNVILGET : VedtakResultatType.AVSLAG;
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultat)
            .medAnsvarligSaksbehandler("VL")
            .medVedtakstidspunkt(LocalDateTime.now())
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medBeslutning(false)
            .medVedtakResultatType(vedtakResultatType).build();

        behandlingVedtakRepository.lagre(behandlingVedtak, kontekst.getSkriveLås());
        repository.flush();
        repository.clear();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void oppdaterMedBehandlingsresultat(BehandlingskontrollKontekst kontekst, boolean innvilget, int antallBarn) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        VilkårResultat vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, innvilget ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT,
                null, new Properties(), null, false, false, null, null)
            .medVilkårResultatType(innvilget ? VilkårResultatType.INNVILGET : VilkårResultatType.AVSLÅTT)
            .buildFor(behandling);

        BehandlingLås lås = kontekst.getSkriveLås();
        behandlingRepository.lagre(vilkårResultat, lås);

        if (innvilget) {
            var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
            LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(new LegacyESBeregning(48500L, antallBarn, 48500L * antallBarn, LocalDateTime.now()))
                .buildFor(behandling, bres);
            beregningRepository.lagre(beregningResultat, lås);
        }

        Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
            .medBehandlingResultatType(innvilget ? BehandlingResultatType.INNVILGET : BehandlingResultatType.AVSLÅTT)
            .buildFor(behandling);

        behandlingRepository.lagre(behandling, lås);

        repository.clear();
    }

}
