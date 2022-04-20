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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
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
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(new BehandlingRepositoryProvider(entityManager));
        referanse = BehandlingReferanse.fra(behandling);
        var kontekst = new BehandlingskontrollKontekst(referanse.fagsakId(), AktørId.dummy(), new BehandlingLås(referanse.behandlingId()));
        ryddBeregningsgrunnlag = new RyddBeregningsgrunnlag(beregningsgrunnlagRepository, kontekst);
    }

    @Test
    public void ryddForeslåBeregningsgrunnlagVedTilbakeføring_skalReaktivereForeslå() {
        // Arrange
        var opprettet = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.behandlingId(), opprettet, BeregningsgrunnlagTilstand.OPPRETTET);

        var kofakberut = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.behandlingId(), kofakberut, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        var foreslått = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.behandlingId(), foreslått, BeregningsgrunnlagTilstand.FORESLÅTT);

        var fastsatt = opprettBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(referanse.behandlingId(), fastsatt, BeregningsgrunnlagTilstand.FASTSATT);


        // Act
        ryddBeregningsgrunnlag.ryddForeslåBeregningsgrunnlagVedTilbakeføring();

        // Assert
        var hentet = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(referanse.behandlingId());
        assertThat(hentet).isSameAs(foreslått);
    }

    private BeregningsgrunnlagEntitet opprettBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.ZERO)
            .build();
    }
}
