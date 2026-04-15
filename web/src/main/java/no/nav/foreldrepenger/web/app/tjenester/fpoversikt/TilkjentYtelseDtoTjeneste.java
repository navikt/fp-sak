package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TilkjentYtelseDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilkjentYtelseDtoTjeneste.class);

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


    public Optional<OversiktTilkjentYtelse> lagDtoForTilkjentYtelse(BehandlingReferanse ref) {
        try {
            var beregningsresultatEntitet = beregningsresultatRepository.hentBeregningsresultatAggregat(ref.behandlingId());

            if (beregningsresultatEntitet.isEmpty()) {
                return Optional.of(new OversiktTilkjentYtelse(List.of(), List.of()));
            }

            var perioder = beregningsresultatEntitet.get()
                .getBgBeregningsresultatFP()
                .getBeregningsresultatPerioder()
                .stream()
                .map(this::mapTilkjentYtelsePeriode)
                .toList();

            var feriepenger = beregningsresultatEntitet.get().getGjeldendeFeriepenger().stream().map(this::mapFeriepengeAndel).toList();

            return Optional.of(new OversiktTilkjentYtelse(perioder, feriepenger));
        } catch (Exception e) {
            LOG.info("Feil ved henting av tilkjent ytelse for behandling {}", ref.behandlingId(), e);
            return Optional.empty();
        }
    }

    private OversiktTilkjentYtelse.FeriepengeAndel mapFeriepengeAndel(BeregningsresultatFeriepengerPrÅr r) {
        return new OversiktTilkjentYtelse.FeriepengeAndel(r.getOpptjeningsår(), r.getÅrsbeløp().getVerdi(), r.getArbeidsforholdIdentifikator(), r.erBrukerMottaker());
    }

    private OversiktTilkjentYtelse.TilkjentYtelsePeriode mapTilkjentYtelsePeriode(BeregningsresultatPeriode periode) {
        var andeler = periode.getBeregningsresultatAndelList().stream().map(this::mapTilkjentYtelseAndel).toList();
        return new OversiktTilkjentYtelse.TilkjentYtelsePeriode(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), andeler);
    }

    private OversiktTilkjentYtelse.TilkjentYtelsePeriode.Andel mapTilkjentYtelseAndel(BeregningsresultatAndel andel) {
        var arbeidsgiverIdent = andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null);
        var arbeidsgivernavn = andel.getArbeidsgiver().map(arbeidsgiverTjeneste::hent).map(ArbeidsgiverOpplysninger::getNavn).orElse(null);

        return new OversiktTilkjentYtelse.TilkjentYtelsePeriode.Andel(OversiktAktivitetStatus.fraBehandlingslagerStatus(andel.getAktivitetStatus()), arbeidsgiverIdent,
            arbeidsgivernavn, BigDecimal.valueOf(andel.getDagsats()), andel.erBrukerMottaker(), andel.getUtbetalingsgrad());
    }
}
