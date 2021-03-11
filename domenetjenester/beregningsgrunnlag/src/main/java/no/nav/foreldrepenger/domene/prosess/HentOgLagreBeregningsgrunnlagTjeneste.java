package no.nav.foreldrepenger.domene.prosess;

import static no.nav.foreldrepenger.domene.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper.mapTilstand;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

/**
 * Fasade tjeneste eksponert fra modul for å hente opp beregningsgrunnlag i andre moduler.
 */
@ApplicationScoped
public class HentOgLagreBeregningsgrunnlagTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    HentOgLagreBeregningsgrunnlagTjeneste() {
        // for CDI
    }

    public HentOgLagreBeregningsgrunnlagTjeneste(EntityManager entityManager) {
        this(new BeregningsgrunnlagRepository(entityManager));
    }

    @Inject
    public HentOgLagreBeregningsgrunnlagTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(Long behandlingId,
                                                                                                                 Optional<Long> originalBehandlingId,
                                                                                                                 BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId,
            originalBehandlingId, beregningsgrunnlagTilstand);
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentBeregningsgrunnlagGrunnlagEntitet(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
    }

    public BeregningsgrunnlagEntitet hentBeregningsgrunnlagEntitetAggregatForBehandling(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitet(Long behandlingid,
                                                                                                  BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingid,
            beregningsgrunnlagTilstand);
    }

    public Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlagEntitetForId(Long beregningsgrunnlagId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagForId(beregningsgrunnlagId);
    }

    public Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlagEntitetForBehandling(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingId);
    }

    public void lagre(Long behandlingId, BeregningsgrunnlagGrunnlagDto fraKalkulus) {
        var builder = BeregningsgrunnlagGrunnlagBuilder.kopi(
            beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId));

        fraKalkulus.getSaksbehandletAktiviteter()
            .map(KalkulusTilBehandlingslagerMapper::mapSaksbehandletAktivitet)
            .ifPresent(builder::medSaksbehandletAktiviteter);

        fraKalkulus.getRefusjonOverstyringer()
            .map(KalkulusTilBehandlingslagerMapper::mapRefusjonOverstyring)
            .ifPresent(builder::medRefusjonOverstyring);

        fraKalkulus.getBeregningsgrunnlag()
            .map(beregningsgrunnlagFraKalkulus -> KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(
                beregningsgrunnlagFraKalkulus, fraKalkulus.getFaktaAggregat(), Optional.empty()))
            .ifPresent(builder::medBeregningsgrunnlag);

        fraKalkulus.getOverstyring()
            .map(KalkulusTilBehandlingslagerMapper::mapAktivitetOverstyring)
            .ifPresent(builder::medOverstyring);

        beregningsgrunnlagRepository.lagre(behandlingId, builder,
            mapTilstand(fraKalkulus.getBeregningsgrunnlagTilstand()));
    }

}
