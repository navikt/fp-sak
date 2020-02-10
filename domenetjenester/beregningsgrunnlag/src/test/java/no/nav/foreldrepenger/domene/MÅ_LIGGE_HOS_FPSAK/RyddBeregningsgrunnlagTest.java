package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class RyddBeregningsgrunnlagTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingReferanse referanse;
    private RyddBeregningsgrunnlag ryddBeregningsgrunnlag;

    @Before
    public void setup() {
        beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();

        ScenarioForeldrepenger scenario = ScenarioForeldrepenger.nyttScenario();
        referanse = scenario.lagre(repositoryProvider);
        var kontekst = new BehandlingskontrollKontekst(referanse.getFagsakId(), AktørId.dummy(), new BehandlingLås(referanse.getId()));
        ryddBeregningsgrunnlag = new RyddBeregningsgrunnlag(repositoryProvider.getBeregningsgrunnlagRepository(), kontekst);
    }

    @Test
    public void ryddForeslåBeregningsgrunnlagVedTilbakeføring_skalReaktivereForeslå() {
        // Arrange
        BeregningsgrunnlagEntitet opprettet = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), opprettet, BeregningsgrunnlagTilstand.OPPRETTET);

        BeregningsgrunnlagEntitet kofakberut = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), kofakberut, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        BeregningsgrunnlagEntitet foreslått = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), foreslått, BeregningsgrunnlagTilstand.FORESLÅTT);

        BeregningsgrunnlagEntitet fastsatt = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), fastsatt, BeregningsgrunnlagTilstand.FASTSATT);


        // Act
        ryddBeregningsgrunnlag.ryddForeslåBeregningsgrunnlagVedTilbakeføring();

        // Assert
        BeregningsgrunnlagEntitet hentet = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(referanse.getId());
        assertThat(hentet).isSameAs(foreslått);
    }

    private BeregningsgrunnlagEntitet opprettBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.ZERO)
            .build();
    }
}
