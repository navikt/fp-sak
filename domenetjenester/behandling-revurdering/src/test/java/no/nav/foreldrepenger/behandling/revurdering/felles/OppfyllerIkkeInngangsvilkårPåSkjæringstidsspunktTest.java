package no.nav.foreldrepenger.behandling.revurdering.felles;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@CdiDbAwareTest
public class OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunktTest {

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndring;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private Behandling revurdering;
    private Behandlingsresultat revurderingResultat;

    @BeforeEach
    public void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE,
                BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository()
                .lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(),
                        false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        var revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste,
                iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
                .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));
        revurderingResultat = repositoryProvider.getBehandlingsresultatRepository()
                .hentHvisEksisterer(revurdering.getId())
                .orElse(null);
    }

    @Test
    public void skal_teste_at_alle_inngangsvilkår_oppfylt_gir_positivt_utfall() {
        // Arrange
        VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKNADSFRISTVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OMSORGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .buildFor(revurdering);

        // Act
        boolean oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(
                revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isFalse();
    }

    @Test
    public void skal_teste_at_inngangsvilkår_ikke_oppfylt_gir_negativt_utfall() {
        // Arrange
        VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_OPPFYLT)
                .buildFor(revurdering);

        // Act
        boolean oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(
                revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isTrue();
    }

    @Test
    public void skal_teste_at_inngangsvilkår_ikke_vurdert_gir_samme_som_omliggende() {
        // Arrange
        VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_VURDERT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .buildFor(revurdering);

        // Act
        boolean oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(
                revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isFalse();
    }

    @Test
    public void skal_teste_negativ_medlemsskapsvilkår_gir_negativt_resultat() {
        // Arrange
        VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
                .buildFor(revurdering);

        // Act
        boolean oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(
                revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isTrue();
    }

    @Test
    public void skal_teste_at_behandlingsresultatet_fastsettes_korrekt() {
        // Act
        Behandlingsresultat oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.fastsett(
                revurdering, revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isNotNull();
        assertThat(oppfyllerIkkjeInngangsvilkår.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(oppfyllerIkkjeInngangsvilkår.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(oppfyllerIkkjeInngangsvilkår.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
        assertThat(oppfyllerIkkjeInngangsvilkår.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(oppfyllerIkkjeInngangsvilkår.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
    }

    @Test
    public void skal_teste_at_behandlingsresultatet_fastsettes_korrekt_for_saker_som_skal_behandles_i_infotrygd() {
        // Arrange
        VilkårResultat.builder()
                .leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
                        Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN)
                .buildFor(revurdering);

        // Act
        Behandlingsresultat oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.fastsett(
                revurdering, revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isNotNull();
        assertThat(oppfyllerIkkjeInngangsvilkår.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(oppfyllerIkkjeInngangsvilkår.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(oppfyllerIkkjeInngangsvilkår.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
        assertThat(oppfyllerIkkjeInngangsvilkår.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(oppfyllerIkkjeInngangsvilkår.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
    }

    @Test
    public void skal_teste_at_behandlingsresultatet_fastsettes_korrekt_for_saker_med_avslagsårsak_null() {
        // Arrange
        VilkårResultat.builder()
                .leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Avslagsårsak.UDEFINERT)
                .buildFor(revurdering);

        // Act
        Behandlingsresultat oppfyllerIkkjeInngangsvilkår = OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.fastsett(
                revurdering, revurderingResultat);

        // Assert
        assertThat(oppfyllerIkkjeInngangsvilkår).isNotNull();
        assertThat(oppfyllerIkkjeInngangsvilkår.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(oppfyllerIkkjeInngangsvilkår.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(oppfyllerIkkjeInngangsvilkår.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
        assertThat(oppfyllerIkkjeInngangsvilkår.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(oppfyllerIkkjeInngangsvilkår.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
    }

}
