package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.BRAndelSammenligning;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.BeregningsresultatTidslinjetjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.HindreTilbaketrekkNårAlleredeUtbetalt;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.KopierFeriepenger;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@BehandlingStegRef(kode = "BERYT_OPPDRAG")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class HindreTilbaketrekkSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjenesteInstances;

    HindreTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkSteg(BehandlingRepositoryProvider repositoryProvider,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            @Any Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjenesteInstances,
            BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste,
            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.finnEndringsdatoBeregningsresultatTjenesteInstances = finnEndringsdatoBeregningsresultatTjenesteInstances;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingBeregningsresultatEntitet aggregatTY = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        if (aggregatTY.skalHindreTilbaketrekk().orElse(false)) {
            BeregningsresultatEntitet revurderingTY = aggregatTY.getBgBeregningsresultatFP();
            BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling);
            LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = beregningsresultatTidslinjetjeneste
                    .lagTidslinjeForRevurdering(behandlingReferanse);
            LocalDate utledetSkjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
            BeregningsresultatEntitet utbetBR = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(revurderingTY, brAndelTidslinje,
                    finnYrkesaktiviteter(behandlingReferanse), utledetSkjæringstidspunkt);

            KopierFeriepenger.kopier(behandlingId, revurderingTY, utbetBR);

            FinnEndringsdatoBeregningsresultatTjeneste finnEndringsdatoBeregningsresultatTjeneste = FagsakYtelseTypeRef.Lookup
                    .find(finnEndringsdatoBeregningsresultatTjenesteInstances, behandling.getFagsakYtelseType())
                    .orElseThrow(() -> new IllegalStateException(
                            "Finner ikke implementasjon for FinnEndringsdatoBeregningsresultatTjeneste for behandling " + behandling.getId()));

            Optional<LocalDate> endringsDato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(behandling, utbetBR);
            endringsDato.ifPresent(endringsdato -> BeregningsresultatEntitet.builder(utbetBR).medEndringsdato(endringsdato));

            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBR);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Collection<Yrkesaktivitet> finnYrkesaktiviteter(BehandlingReferanse behandlingReferanse) {
        Optional<AktørArbeid> aktørArbeidFraRegister = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
                .getAktørArbeidFraRegister(behandlingReferanse.getAktørId());
        return aktørArbeidFraRegister
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList());
    }

}
