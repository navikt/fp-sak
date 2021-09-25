package no.nav.foreldrepenger.behandling.steg.vedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FORESLÅ_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.es.BeregningsgrunnlagXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.BeregningsresultatXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.PersonopplysningXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.VilkårsgrunnlagXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.es.YtelseXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;
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
import no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste.SimulerInntrekkSjekkeTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@CdiDbAwareTest
public class FatteVedtakStegTest {

    private static final String BEHANDLENDE_ENHET = "Stord";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Inject
    private LegacyESBeregningRepository beregningRepository;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;
    @Inject
    private KlageRepository klageRepository;
    @Inject
    private AnkeRepository ankeRepository;

    private FatteVedtakSteg fatteVedtakSteg;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;

    private KompletthetsjekkerProvider kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);

    @BeforeEach
    public void oppsett(EntityManager entityManager) {
        var vedtakRepository = new LagretVedtakRepository(entityManager);

        var oppgaveTjeneste = mock(OppgaveTjeneste.class);
        var personinfoAdapter = Mockito.mock(PersoninfoAdapter.class);
        var personopplysningTjeneste = Mockito.mock(PersonopplysningTjeneste.class);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);
        var personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(
                poXmlFelles, repositoryProvider, personopplysningTjeneste, iayTjeneste, mock(VergeRepository.class));
        VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste = new VilkårsgrunnlagXmlTjenesteImpl(repositoryProvider, kompletthetssjekkerProvider,
                skjæringstidspunktTjeneste);
        YtelseXmlTjeneste ytelseXmlTjeneste = new YtelseXmlTjenesteImpl(beregningRepository);
        BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste = new BeregningsgrunnlagXmlTjenesteImpl(beregningRepository);
        BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste = new BeregningsresultatXmlTjenesteImpl(beregningsgrunnlagXmlTjeneste,
                ytelseXmlTjeneste);
        var behandlingsresultatXmlTjeneste = nyBeregningsresultatXmlTjeneste(entityManager, vilkårsgrunnlagXmlTjeneste,
                beregningsresultatXmlTjeneste);

        var totrinnTjeneste = mock(TotrinnTjeneste.class);

        var fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
                new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
                behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
        var klageAnkeVedtakTjeneste = new KlageAnkeVedtakTjeneste(klageRepository, ankeRepository);
        var vedtakTjeneste = new VedtakTjeneste(null, repositoryProvider, klageAnkeVedtakTjeneste, mock(TotrinnTjeneste.class));

        var behandlingVedtakEventPubliserer = mock(BehandlingVedtakEventPubliserer.class);

        behandlingVedtakTjeneste = new BehandlingVedtakTjeneste(behandlingVedtakEventPubliserer, repositoryProvider);
        var klageanke = new KlageAnkeVedtakTjeneste(klageRepository, mock(AnkeRepository.class));
        var fatteVedtakTjeneste = new FatteVedtakTjeneste(vedtakRepository, klageanke, fpSakVedtakXmlTjeneste, vedtakTjeneste,
                oppgaveTjeneste, totrinnTjeneste, behandlingVedtakTjeneste);
        var simuler = new SimulerInntrekkSjekkeTjeneste(null, null, null, null);
        fatteVedtakSteg = new FatteVedtakSteg(repositoryProvider, fatteVedtakTjeneste, simuler);
    }

    private BehandlingsresultatXmlTjeneste nyBeregningsresultatXmlTjeneste(EntityManager em, VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste,
            BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste) {
        return new BehandlingsresultatXmlTjeneste(
                new UnitTestLookupInstanceImpl<>(beregningsresultatXmlTjeneste),
                new UnitTestLookupInstanceImpl<>(vilkårsgrunnlagXmlTjeneste),
                behandlingVedtakRepository,
                klageRepository,
                ankeRepository,
                new VilkårResultatRepository(em));
    }

    @Test
    public void skal_feile_hvis_behandling_i_feil_tilstand() {
        // Arrange
        var antallBarn = 1;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.SØKERS_RELASJON_TIL_BARN,
                Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        // Act
        assertThrows(TekniskException.class, () -> fatteVedtakSteg.utførSteg(kontekst));
    }

    @Test
    public void skal_fatte_positivt_vedtak() {
        // Arrange
        var antallBarn = 2;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        // Act
        fatteVedtakSteg.utførSteg(kontekst);

        // Assert
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(kontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        var behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void revurdering_med_endret_utfall_skal_ha_nytt_vedtak() {
        // Opprinnelig behandling med vedtak
        var antallBarn = 1;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);
        oppdaterMedVedtak(kontekst);
        var originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId()))
                .build();

        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        var behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        var fagsak = revurdering.getFagsak();
        var revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, false, antallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        var behandlingVedtakOpt = behandlingVedtakRepository
                .hentForBehandlingHvisEksisterer(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        var behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isFalse();
    }

    @Test
    public void revurdering_med_endret_antall_barn_skal_ha_nytt_vedtak() {
        var originalAntallBarn = 1;
        var faktiskAntallBarn = 2;
        var kontekst = byggBehandlingsgrunnlagForFødsel(originalAntallBarn, BehandlingStegType.FATTE_VEDTAK,
                Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, originalAntallBarn);
        oppdaterMedVedtak(kontekst);
        var originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId()))
                .build();

        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        var behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        var fagsak = revurdering.getFagsak();
        var revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, true, faktiskAntallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        var behandlingVedtakOpt = behandlingVedtakRepository
                .hentForBehandlingHvisEksisterer(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        var behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isFalse();
    }

    @Test
    public void revurdering_med_samme_utfall_innvilget_skal_ha_beslutning() {
        var antallBarn = 1;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);
        oppdaterMedVedtak(kontekst);
        var originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId()))
                .build();

        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);

        var behandlingLås = lagreBehandling(revurdering);

        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        var fagsak = revurdering.getFagsak();
        var revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, true, antallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        var behandlingVedtakOpt = behandlingVedtakRepository
                .hentForBehandlingHvisEksisterer(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        var behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isTrue();
    }

    private void opprettFamilieHendelseGrunnlag(Behandling originalBehandling, Behandling revurdering) {
        repositoryProvider.getFamilieHendelseRepository().kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
    }

    private BehandlingLås lagreBehandling(Behandling behandling) {
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandlingLås;
    }

    @Test
    public void revurdering_med_samme_utfall_avslag_skal_ha_beslutning() {
        var antallBarn = 1;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, false, antallBarn);
        oppdaterMedVedtak(kontekst);
        var originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId()))
                .build();

        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        var behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        var fagsak = revurdering.getFagsak();
        var revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, false, antallBarn);

        fatteVedtakSteg.utførSteg(revurderingKontekst);
        var behandlingVedtakOpt = behandlingVedtakRepository
                .hentForBehandlingHvisEksisterer(revurderingKontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        var behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
        assertThat(behandlingVedtak.isBeslutningsvedtak()).isTrue();
    }

    @Test
    public void skal_lukke_godkjent_aksjonspunkter_og_sette_steg_til_utført(EntityManager entityManager) {
        // Arrange
        var vedtakRepository = new LagretVedtakRepository(entityManager);

        var oppgaveTjeneste = mock(OppgaveTjeneste.class);
        var søknadRepository = mock(SøknadRepository.class);
        var personinfoAdapter = mock(PersoninfoAdapter.class);
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(ArgumentMatchers.any())).thenReturn(skjæringstidspunkt);
        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);
        var personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider,
                personopplysningTjeneste, iayTjeneste, mock(VergeRepository.class));
        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste = new VilkårsgrunnlagXmlTjenesteImpl(repositoryProvider, kompletthetssjekkerProvider,
                skjæringstidspunktTjeneste);
        YtelseXmlTjeneste ytelseXmlTjeneste = new YtelseXmlTjenesteImpl(beregningRepository);
        BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste = new BeregningsgrunnlagXmlTjenesteImpl(beregningRepository);
        BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste = new BeregningsresultatXmlTjenesteImpl(beregningsgrunnlagXmlTjeneste,
                ytelseXmlTjeneste);
        var behandlingsresultatXmlTjeneste = nyBeregningsresultatXmlTjeneste(entityManager, vilkårsgrunnlagXmlTjeneste,
                beregningsresultatXmlTjeneste);
        var totrinnTjeneste = mock(TotrinnTjeneste.class);

        var søknad = new SøknadEntitet.Builder().medMottattDato(LocalDate.now()).medSøknadsdato(LocalDate.now()).build();
        lenient().when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));

        var fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
                new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
                behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
        var klageAnkeVedtakTjeneste = new KlageAnkeVedtakTjeneste(klageRepository, ankeRepository);
        var vedtakTjeneste = new VedtakTjeneste(null, repositoryProvider, klageAnkeVedtakTjeneste, mock(TotrinnTjeneste.class));

        var antallBarn = 2;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK,
                List.of(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL));
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setToTrinnsBehandling();

        // Legg til data i totrinsvurdering.
        var vurdering = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        var ttvurdering = vurdering.medGodkjent(true).medBegrunnelse("").build();

        List<Totrinnsvurdering> totrinnsvurderings = new ArrayList<>();
        totrinnsvurderings.add(ttvurdering);
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling)).thenReturn(totrinnsvurderings);
        var klageanke = new KlageAnkeVedtakTjeneste(klageRepository, mock(AnkeRepository.class));
        var fvtei = new FatteVedtakTjeneste(vedtakRepository, klageanke, fpSakVedtakXmlTjeneste, vedtakTjeneste,
                oppgaveTjeneste, totrinnTjeneste, behandlingVedtakTjeneste);

        var simuler = new SimulerInntrekkSjekkeTjeneste(null, null, null, null);
        fatteVedtakSteg = new FatteVedtakSteg(repositoryProvider, fvtei, simuler);
        AksjonspunktTestSupport.setToTrinnsBehandlingKreves(behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL));

        var behandleStegResultat = fatteVedtakSteg.utførSteg(kontekst);

        var aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunktListe).isEmpty();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
    }

    @Test
    public void tilbakefører_og_reåpner_aksjonspunkt_når_totrinnskontroll_ikke_godkjent(EntityManager entityManager) {
        var vedtakRepository = new LagretVedtakRepository(entityManager);

        var oppgaveTjeneste = mock(OppgaveTjeneste.class);
        var søknadRepository = mock(SøknadRepository.class);
        var personinfoAdapter = Mockito.mock(PersoninfoAdapter.class);
        var personopplysningTjeneste = Mockito.mock(PersonopplysningTjeneste.class);
        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(ArgumentMatchers.any())).thenReturn(skjæringstidspunkt);
        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);
        var personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider,
                personopplysningTjeneste, iayTjeneste, mock(VergeRepository.class));
        VilkårsgrunnlagXmlTjeneste vilkårsgrunnlagXmlTjeneste = new VilkårsgrunnlagXmlTjenesteImpl(repositoryProvider, kompletthetssjekkerProvider,
                skjæringstidspunktTjeneste);
        YtelseXmlTjeneste ytelseXmlTjeneste = new YtelseXmlTjenesteImpl(beregningRepository);
        BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste = new BeregningsgrunnlagXmlTjenesteImpl(beregningRepository);
        BeregningsresultatXmlTjeneste beregningsresultatXmlTjeneste = new BeregningsresultatXmlTjenesteImpl(beregningsgrunnlagXmlTjeneste,
                ytelseXmlTjeneste);
        var behandlingsresultatXmlTjeneste = nyBeregningsresultatXmlTjeneste(entityManager, vilkårsgrunnlagXmlTjeneste,
                beregningsresultatXmlTjeneste);

        var totrinnTjeneste = mock(TotrinnTjeneste.class);

        var søknad = new SøknadEntitet.Builder().medMottattDato(LocalDate.now()).medSøknadsdato(LocalDate.now()).build();
        lenient().when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));

        var fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
                new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
                behandlingsresultatXmlTjeneste, skjæringstidspunktTjeneste);
        var klageAnkeVedtakTjeneste = new KlageAnkeVedtakTjeneste(klageRepository, ankeRepository);
        var vedtakTjeneste = new VedtakTjeneste(null, repositoryProvider, klageAnkeVedtakTjeneste, mock(TotrinnTjeneste.class));

        var antallBarn = 2;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK,
                List.of(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET,
                        AksjonspunktDefinisjon.FORESLÅ_VEDTAK));
        oppdaterMedBehandlingsresultat(kontekst, true, antallBarn);

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setToTrinnsBehandling();

        AksjonspunktTestSupport.setToTrinnsBehandlingKreves(behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL));

        // Legg til data i totrinsvurdering.
        var vurdering = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        var vurderesPåNytt = vurdering.medGodkjent(false).medBegrunnelse("Må vurderes på nytt").medVurderÅrsak(VurderÅrsak.FEIL_LOV)
                .build();

        var vurdering2 = new Totrinnsvurdering.Builder(behandling,
                AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        var vurderesOk = vurdering2.medGodkjent(true).medBegrunnelse("").build();

        List<Totrinnsvurdering> totrinnsvurderings = new ArrayList<>();
        totrinnsvurderings.add(vurderesPåNytt);
        totrinnsvurderings.add(vurderesOk);
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling)).thenReturn(totrinnsvurderings);
        var klageanke = new KlageAnkeVedtakTjeneste(klageRepository, mock(AnkeRepository.class));
        var fvtei = new FatteVedtakTjeneste(vedtakRepository, klageanke, fpSakVedtakXmlTjeneste, vedtakTjeneste,
                oppgaveTjeneste, totrinnTjeneste, behandlingVedtakTjeneste);

        var simuler = new SimulerInntrekkSjekkeTjeneste(null, null, null, null);
        fatteVedtakSteg = new FatteVedtakSteg(repositoryProvider, fvtei, simuler);

        var behandleStegResultat = fatteVedtakSteg.utførSteg(kontekst);

        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT);

        behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var oppdatertAvklarFødsel = behandling.getAksjonspunktMedDefinisjonOptional(SJEKK_MANGLENDE_FØDSEL);
        assertThat(oppdatertAvklarFødsel).isPresent();
        assertThat(oppdatertAvklarFødsel.get().getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        var oppdatertForeslåVedtak = behandling.getAksjonspunktMedDefinisjonOptional(FORESLÅ_VEDTAK);
        assertThat(oppdatertForeslåVedtak).isPresent();
        assertThat(oppdatertForeslåVedtak.get().getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        var oppdatertSøknFristVilkåret = behandling
                .getAksjonspunktMedDefinisjonOptional(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        assertThat(oppdatertSøknFristVilkåret).isPresent();
    }

    @Test
    public void skal_fatte_negativt_vedtak() {
        // Arrange
        var antallBarn = 1;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, BehandlingStegType.FATTE_VEDTAK, Collections.emptyList());
        oppdaterMedBehandlingsresultat(kontekst, false, antallBarn);

        // Act
        fatteVedtakSteg.utførSteg(kontekst);

        // Assert
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(kontekst.getBehandlingId());
        assertThat(behandlingVedtakOpt).isPresent();
        var behandlingVedtak = behandlingVedtakOpt.get();
        assertThat(behandlingVedtak).isNotNull();
        assertThat(behandlingVedtak.getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
    }

    private BehandlingskontrollKontekst byggBehandlingsgrunnlagForFødsel(int antallBarn, BehandlingStegType behandlingStegType,
            List<AksjonspunktDefinisjon> aksjonspunkter) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBekreftetHendelse().tilbakestillBarn().medFødselsDato(SKJÆRINGSTIDSPUNKT, antallBarn)
                .medAntallBarn(antallBarn);
        aksjonspunkter.forEach(apd -> scenario.leggTilAksjonspunkt(apd, BehandlingStegType.KONTROLLER_FAKTA));

        var behandling = scenario
                .medBehandlingStegStart(behandlingStegType)
                .medBehandlendeEnhet(BEHANDLENDE_ENHET)
                .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
                .lagre(repositoryProvider);
        var beregning = new LegacyESBeregning(1L, 1L, 1L, LocalDateTime.now());
        var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bres);
        beregningRepository.lagre(beregningResultat, behandlingRepository.taSkriveLås(behandling));

        var fagsak = behandling.getFagsak();
        return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

    private void oppdaterMedVedtak(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behandlingsresultat = getBehandlingsresultat(behandling);
        var vedtakResultatType = behandlingsresultat.getBehandlingResultatType()
                .equals(BehandlingResultatType.INNVILGET) ? VedtakResultatType.INNVILGET : VedtakResultatType.AVSLAG;
        var behandlingVedtak = BehandlingVedtak.builder()
                .medBehandlingsresultat(behandlingsresultat)
                .medAnsvarligSaksbehandler("VL")
                .medVedtakstidspunkt(LocalDateTime.now())
                .medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medBeslutning(false)
                .medVedtakResultatType(vedtakResultatType).build();

        behandlingVedtakRepository.lagre(behandlingVedtak, kontekst.getSkriveLås());
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void oppdaterMedBehandlingsresultat(BehandlingskontrollKontekst kontekst, boolean innvilget, int antallBarn) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (innvilget) {
            var vilkårResultat = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .medVilkårResultatType(VilkårResultatType.INNVILGET)
                .buildFor(behandling);

            var lås = kontekst.getSkriveLås();
            behandlingRepository.lagre(vilkårResultat, lås);
            var bres = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
            var beregningResultat = LegacyESBeregningsresultat.builder()
                    .medBeregning(new LegacyESBeregning(48500L, antallBarn, 48500L * antallBarn, LocalDateTime.now()))
                    .buildFor(behandling, bres);
            beregningRepository.lagre(beregningResultat, lås);
            Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);

            behandlingRepository.lagre(behandling, lås);
        } else {
            var vilkårResultat = VilkårResultat.builder()
                .leggTilVilkårAvslått(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallMerknad.VM_1026)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
                .buildFor(behandling);

            var lås = kontekst.getSkriveLås();
            behandlingRepository.lagre(vilkårResultat, lås);
            Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
                .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
                .buildFor(behandling);

            behandlingRepository.lagre(behandling, lås);
        }


    }

}
