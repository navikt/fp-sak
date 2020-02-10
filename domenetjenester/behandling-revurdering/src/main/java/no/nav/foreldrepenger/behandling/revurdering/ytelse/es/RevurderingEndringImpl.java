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
import no.nav.vedtak.feil.FeilFactory;

/**
 * Sjekk om revurdering endrer utfall.
 */
@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class RevurderingEndringImpl implements RevurderingEndring {

    private LegacyESBeregningRepository beregningRepository;

    public RevurderingEndringImpl() {
    }

    @Inject
    public RevurderingEndringImpl(LegacyESBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling, BehandlingResultatType nyResultatType) {
        if (!BehandlingType.REVURDERING.equals(behandling.getType())) {
            return false;
        }
        Optional<Behandling> originalBehandlingOptional = behandling.getOriginalBehandling();

        if (!originalBehandlingOptional.isPresent()) {
            throw FeilFactory.create(RevurderingFeil.class).revurderingManglerOriginalBehandling(behandling.getId()).toException();
        }

        Behandling originalBehandling = originalBehandlingOptional.get();
        BehandlingResultatType originalResultatType = getBehandlingsresultat(originalBehandling).getBehandlingResultatType();

        // Forskjellig utfall
        if (!nyResultatType.equals(originalResultatType)) {
            return false;
        }

        // Begge har utfall INNVILGET
        if (nyResultatType.equals(BehandlingResultatType.INNVILGET)) {
            Optional<LegacyESBeregning> nyBeregning = beregningRepository.getSisteBeregning(behandling.getId());
            Optional<LegacyESBeregning> originalBeregning = beregningRepository.getSisteBeregning(originalBehandling.getId());
            if (originalBeregning.isPresent() && nyBeregning.isPresent()) {
                return harSammeBeregnetYtelse(nyBeregning.get(), originalBeregning.get());
            } else {
                throw FeilFactory.create(RevurderingFeil.class)
                    .behandlingManglerBeregning(originalBeregning.isPresent() ? behandling.getId() : originalBehandling.getId())
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
        if (getBehandlingsresultat(behandling) == null) {
            return false;
        }
        return erRevurderingMedUendretUtfall(behandling, getBehandlingsresultat(behandling).getBehandlingResultatType());
    }

    private boolean harSammeBeregnetYtelse(LegacyESBeregning nyBeregning, LegacyESBeregning originalBeregning) {
        return Objects.equals(nyBeregning.getBeregnetTilkjentYtelse(), originalBeregning.getBeregnetTilkjentYtelse());
    }

}
