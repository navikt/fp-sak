package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.feil.FeilFactory;

/**
 * Sjekk om revurdering endrer utfall.
 */
@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class RevurderingEndringImpl implements RevurderingEndring {

    private LegacyESBeregningRepository beregningRepository;
    private BehandlingRepository behandlingRepository;

    public RevurderingEndringImpl() {
    }

    @Inject
    public RevurderingEndringImpl(BehandlingRepository behandlingRepository,
                                  LegacyESBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling, BehandlingResultatType nyResultatType) {
        if (!BehandlingType.REVURDERING.equals(behandling.getType())) {
            return false;
        }
        Long originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> FeilFactory.create(RevurderingFeil.class).revurderingManglerOriginalBehandling(behandling.getId()).toException());
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
        BehandlingResultatType originalResultatType = getBehandlingsresultat(originalBehandling).getBehandlingResultatType();

        // Forskjellig utfall
        if (!nyResultatType.equals(originalResultatType)) {
            return false;
        }

        // Begge har utfall INNVILGET
        if (nyResultatType.equals(BehandlingResultatType.INNVILGET)) {
            Optional<LegacyESBeregning> nyBeregning = beregningRepository.getSisteBeregning(behandling.getId());
            Optional<LegacyESBeregning> originalBeregning = beregningRepository.getSisteBeregning(originalBehandlingId);
            if (originalBeregning.isPresent() && nyBeregning.isPresent()) {
                return harSammeBeregnetYtelse(nyBeregning.get(), originalBeregning.get());
            } else {
                throw FeilFactory.create(RevurderingFeil.class)
                    .behandlingManglerBeregning(originalBeregning.isPresent() ? behandling.getId() : originalBehandlingId)
                    .toException();
            }
        }
        // Begge har utfall AVSLÅTT
        return true;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }


    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        var behandlingResultat = getBehandlingsresultat(behandling);
        if (behandlingResultat == null) {
            return false;
        }
        return erRevurderingMedUendretUtfall(behandling, behandlingResultat.getBehandlingResultatType());
    }

    private boolean harSammeBeregnetYtelse(LegacyESBeregning nyBeregning, LegacyESBeregning originalBeregning) {
        return Objects.equals(nyBeregning.getBeregnetTilkjentYtelse(), originalBeregning.getBeregnetTilkjentYtelse());
    }

}
