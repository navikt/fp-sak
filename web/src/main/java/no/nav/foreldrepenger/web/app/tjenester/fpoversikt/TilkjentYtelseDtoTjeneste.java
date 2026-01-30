package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;

import java.util.List;

@ApplicationScoped
public class TilkjentYtelseDtoTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public TilkjentYtelseDtoTjeneste() {
        // CDI
    }

    @Inject
    public TilkjentYtelseDtoTjeneste(BeregningsresultatRepository beregningsresultatRepository, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }


    public FpSak.TilkjentYtelse lagDtoForTilkjentYtelse(BehandlingReferanse ref) {
        var beregningsresultatEntitet = beregningsresultatRepository.hentBeregningsresultatAggregat(ref.behandlingId());

        if (beregningsresultatEntitet.isEmpty()) {
            return new FpSak.TilkjentYtelse(List.of(), List.of());
        }

        var perioder = beregningsresultatEntitet.get()
            .getBgBeregningsresultatFP()
            .getBeregningsresultatPerioder()
            .stream()
            .map(this::mapTilkjentYtelsePeriode)
            .toList();

        return new FpSak.TilkjentYtelse(perioder, List.of());
    }

    private FpSak.TilkjentYtelsePeriode mapTilkjentYtelsePeriode(BeregningsresultatPeriode periode) {
        var andeler = periode.getBeregningsresultatAndelList().stream().map(this::mapTilkjentYtelseAndel).toList();
        return new FpSak.TilkjentYtelsePeriode(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), andeler);
    }

    private FpSak.TilkjentYtelsePeriode.Andel mapTilkjentYtelseAndel(BeregningsresultatAndel andel) {
        var arbeidsgiverIdent = andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null);
        var arbeidsgivernavn = andel.getArbeidsgiver().map(arbeidsgiverTjeneste::hent).map(ArbeidsgiverOpplysninger::getNavn).orElse(null);

        return new FpSak.TilkjentYtelsePeriode.Andel(arbeidsgiverIdent, arbeidsgivernavn, andel.getDagsats(), andel.erBrukerMottaker(),
            andel.getUtbetalingsgrad().doubleValue());
    }
}
