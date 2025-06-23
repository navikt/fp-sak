package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.exception.TekniskException;

/**
 * Sjekk om revurdering endrer utfall.
 */
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class RevurderingEndringImpl implements RevurderingEndring {

    private EngangsstønadBeregningRepository beregningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public RevurderingEndringImpl(BehandlingRepository behandlingRepository,
                                  EngangsstønadBeregningRepository beregningRepository,
                                  BehandlingsresultatRepository behandlingsresultatRepository) {
        this.beregningRepository = beregningRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public RevurderingEndringImpl() {
        //CDI
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling, BehandlingResultatType nyResultatType) {
        if (!BehandlingType.REVURDERING.equals(behandling.getType())) {
            return false;
        }
        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> RevurderingFeil.revurderingManglerOriginalBehandling(behandling.getId()));
        var originalResultatType = getBehandlingResultatType(originalBehandlingId);

        // Forskjellig utfall
        if (!nyResultatType.equals(originalResultatType)) {
            return false;
        }

        // Begge har utfall INNVILGET
        if (nyResultatType.equals(BehandlingResultatType.INNVILGET)) {
            var nyBeregning = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
            var originalBeregning = beregningRepository.hentEngangsstønadBeregning(originalBehandlingId);
            if (originalBeregning.isPresent() && nyBeregning.isPresent()) {
                return harSammeBeregnetYtelse(nyBeregning.get(), originalBeregning.get());
            }
            var behandlingId = originalBeregning.isPresent() ? behandling.getId() : originalBehandlingId;
            throw new TekniskException("FP-818307", String.format("Behandling med id %s mangler beregning", behandlingId));
        }
        // Begge har utfall AVSLÅTT
        return true;
    }

    private BehandlingResultatType getBehandlingResultatType(Long originalBehandlingId) {
        var behandling = behandlingRepository.hentBehandling(originalBehandlingId);
        var behandlingsresultat = getBehandlingsresultat(behandling);
        return behandlingsresultat.orElseThrow().getBehandlingResultatType();
    }

    private Optional<Behandlingsresultat> getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        var behandlingResultat = getBehandlingsresultat(behandling);
        if (behandlingResultat.isEmpty()) {
            return false;
        }
        return erRevurderingMedUendretUtfall(behandling, behandlingResultat.get().getBehandlingResultatType());
    }

    private boolean harSammeBeregnetYtelse(EngangsstønadBeregning nyBeregning, EngangsstønadBeregning originalBeregning) {
        return Objects.equals(nyBeregning.getBeregnetTilkjentYtelse(), originalBeregning.getBeregnetTilkjentYtelse());
    }

}
