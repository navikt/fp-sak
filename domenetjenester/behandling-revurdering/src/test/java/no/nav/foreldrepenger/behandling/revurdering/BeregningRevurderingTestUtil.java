package no.nav.foreldrepenger.behandling.revurdering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class BeregningRevurderingTestUtil {

    public static final String ORGNR = "987123987";
    public static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();
    public static final List<InternArbeidsforholdRef> ARBEIDSFORHOLDLISTE = List
        .of(InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef(),
            InternArbeidsforholdRef.nyRef());
    public static final BigDecimal TOTAL_ANDEL_NORMAL = BigDecimal.valueOf(300000);
    public static final BigDecimal TOTAL_ANDEL_OPPJUSTERT = BigDecimal.valueOf(350000);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    BeregningRevurderingTestUtil() {
        // for CDI
    }

    @Inject
    public BeregningRevurderingTestUtil(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    public void avsluttBehandling(Behandling behandling) {
        if (behandling == null) {
            throw new IllegalStateException("Du må definere en behandling før du kan avslutten den");
        }
        avsluttBehandlingOgFagsak(behandling);
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }
}
