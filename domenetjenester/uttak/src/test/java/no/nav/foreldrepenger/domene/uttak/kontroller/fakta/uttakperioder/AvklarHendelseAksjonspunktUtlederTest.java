package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

public class AvklarHendelseAksjonspunktUtlederTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());

    private UttakRevurderingTestUtil testUtil;

    private AvklarHendelseAksjonspunktUtleder avklarHendelseAksjonspunktUtleder;

    @Before
    public void before() {
        testUtil = new UttakRevurderingTestUtil(repositoryProvider, Mockito.mock(InntektArbeidYtelseTjeneste.class));
        avklarHendelseAksjonspunktUtleder = new AvklarHendelseAksjonspunktUtleder();
    }

    @Test // #1
    public void skal_utlede_aksjonspunkt_for_klage_når_behandling_er_manuelt_opprettet_med_klageårsak() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var input = lagInput(revurdering)
            .medBehandlingManueltOpprettet(true)
            .medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT));

        // Act
        var aksjonspunkter = avklarHendelseAksjonspunktUtleder.utledAksjonspunkterFor(input);

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE);
    }

    private UttakInput lagInput(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null);
    }

    @Test // #4.1
    public void skal_utlede_aksjonspunkt_for_død_når_behandling_er_manuelt_opprettet_med_dødsårsak() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD))
            .medBehandlingManueltOpprettet(true);

        // Act
        var aksjonspunkter = avklarHendelseAksjonspunktUtleder.utledAksjonspunkterFor(input);

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
    }

    @Test // #4.2
    public void skal_utlede_aksjonspunkt_for_død_når_grunnlaget_har_opplysninger_om_død() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var input = lagInput(revurdering)
            .medErOpplysningerOmDødEndret(true);

        // Act
        var aksjonspunkter = avklarHendelseAksjonspunktUtleder.utledAksjonspunkterFor(input);

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
    }

    @Test // #5
    public void skal_utlede_aksjonspunkt_for_søknadsfrist_når_behandling_er_manuelt_opprettet_med_søknadsfristårsak() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var input = lagInput(revurdering)
            .medBehandlingManueltOpprettet(true)
            .medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST));

        // Act
        var aksjonspunkter = avklarHendelseAksjonspunktUtleder.utledAksjonspunkterFor(input);

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);
    }

    @Test // Felles
    public void skal_ikke_utlede_aksjonspunkter_når_ingen_av_kriteriene_er_oppfylt() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

        // Act
        var aksjonspunkter = avklarHendelseAksjonspunktUtleder.utledAksjonspunkterFor(lagInput(revurdering));

        // Assert
        assertThat(aksjonspunkter).isEmpty();
    }
}
