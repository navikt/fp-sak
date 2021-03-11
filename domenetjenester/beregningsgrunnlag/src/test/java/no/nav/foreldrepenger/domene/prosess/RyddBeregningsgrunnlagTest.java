package no.nav.foreldrepenger.domene.prosess;

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
import no.nav.foreldrepenger.domene.prosess.testutilities.behandling.ScenarioForeldrepenger;
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

        var scenario = ScenarioForeldrepenger.nyttScenario();
        referanse = BehandlingReferanse.fra(scenario.lagre(repositoryProvider));
        var kontekst = new BehandlingskontrollKontekst(referanse.getFagsakId(), AktørId.dummy(), new BehandlingLås(referanse.getId()));
        ryddBeregningsgrunnlag = new RyddBeregningsgrunnlag(repositoryProvider.getBeregningsgrunnlagRepository(), kontekst);
    }

    @Test
    public void ryddForeslåBeregningsgrunnlagVedTilbakeføring_skalReaktivereForeslå() {
        // Arrange
        var opprettet = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), opprettet, BeregningsgrunnlagTilstand.OPPRETTET);

        var kofakberut = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), kofakberut, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        var foreslått = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), foreslått, BeregningsgrunnlagTilstand.FORESLÅTT);

        var fastsatt = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.getId(), fastsatt, BeregningsgrunnlagTilstand.FASTSATT);


        // Act
        ryddBeregningsgrunnlag.ryddForeslåBeregningsgrunnlagVedTilbakeføring();

        // Assert
        var hentet = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(referanse.getId());
        assertThat(hentet).isSameAs(foreslått);
    }

    private BeregningsgrunnlagEntitet opprettBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.ZERO)
            .build();
    }
}
