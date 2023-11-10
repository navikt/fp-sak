package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatEngangsstønadDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private LegacyESBeregningRepository beregningRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private BeregningsresultatMedUttaksplanMapper beregningsresultatMedUttaksplanMapper;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(LegacyESBeregningRepository beregningRepository,
                                      BeregningsresultatRepository beregningsresultatRepository,
                                      ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                      BeregningsresultatMedUttaksplanMapper beregningsresultatMedUttaksplanMapper) {
        this.beregningRepository = beregningRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.beregningsresultatMedUttaksplanMapper = beregningsresultatMedUttaksplanMapper;
    }

    public Optional<BeregningsresultatMedUttaksplanDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        var uttakResultat = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
        var beregningsresultatFPAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(behandling.getId());
        return beregningsresultatFPAggregatEntitet
            .map(bresAggregat -> beregningsresultatMedUttaksplanMapper.lagBeregningsresultatMedUttaksplan(behandling, bresAggregat, uttakResultat));
    }

    public Optional<BeregningsresultatEngangsstønadDto> lagBeregningsresultatEnkel(Long behandlingId) {
        var sisteBeregningOpt = beregningRepository.getSisteBeregning(behandlingId);
        if (sisteBeregningOpt.isPresent()) {
            var dto = new BeregningsresultatEngangsstønadDto();
            var beregning = sisteBeregningOpt.get();
            dto.setBeregnetTilkjentYtelse(beregning.getBeregnetTilkjentYtelse());
            dto.setAntallBarn((int) beregning.getAntallBarn());
            dto.setSatsVerdi(beregning.getSatsVerdi());
            return Optional.of(dto);
        }
        return Optional.empty();
    }
}
