package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class RyddBeregningsgrunnlagTest {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingReferanse referanse;
    private RyddBeregningsgrunnlag ryddBeregningsgrunnlag;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        var repositoryProvider = new RepositoryProvider(entityManager);
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
        return BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.ZERO)
            .build();
    }
}
