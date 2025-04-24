package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "beregningsgrunnlag.aap.sak", prioritet = 4, maxFailedRuns = 1)
public class AAPPraksisendringSakTask implements ProsessTaskHandler  {
    private static final Logger LOG = LoggerFactory.getLogger(AAPPraksisendringSakTask.class);
    public static final String FAGSAK_ID = "fagsak_ident";
    private static final String HAR_AT_PÅ_STP_MELDING = "HAR_AT_PÅ_STP";
    private static final String HAR_IKKE_INNTEKT_I_BEREGNINGSPERIODEN_MELDING = "HAR_IKKE_INNTEKT_I_BEREGNINGSPERIODEN";
    private static final String HAR_INNTEKT_I_BEREGNINGSPERIODEN_MELDING = "HAR_INNTEKT_I_BEREGNINGSPERIODEN";

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public AAPPraksisendringSakTask(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                    BehandlingRepository behandlingRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakId = Optional.ofNullable(prosessTaskData.getPropertyValue(FAGSAK_ID)).map(Long::valueOf).orElseThrow();
        LOG.info("Starter task for å undersøke behov for revurdering etter aap praksisendring for fagsakId {}.", fagsakId);
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        var aktivtBeregningsgrunnlag = sisteBehandling.flatMap(b -> beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(b.getId()));
        if (aktivtBeregningsgrunnlag.isEmpty()) {
            LOG.info("Finner ikke aktivt beregningsgrunnlag for fagsakId {}, undersøker ikke videre behov for reberegning", fagsakId);
            return;
        }
        if (harAAPKombinertMedArbeidPåStp(aktivtBeregningsgrunnlag.get())) {
            var msg = String.format("%s - fagsakId %s", HAR_AT_PÅ_STP_MELDING, fagsakId);
            LOG.info(msg);
            return;
        }
        var iayGrunnlag = sisteBehandling.flatMap(b -> inntektArbeidYtelseTjeneste.finnGrunnlag(b.getId()));
        if (iayGrunnlag.isEmpty()) {
            LOG.info("Finner ikke aktivt iay grunnlag for fagsakId {}, undersøker ikke videre behov for reberegning", fagsakId);
            return;
        }
        var inntekter = iayGrunnlag.get().getAktørInntektFraRegister(sisteBehandling.get().getAktørId());
        var sumInntektBeregningsperiode = AAPInntektsberegner.finnAllBeregnetInntektIBeregningsperioden(inntekter, aktivtBeregningsgrunnlag.get().getSkjæringstidspunkt());
        if (sumInntektBeregningsperiode.compareTo(Beløp.ZERO) > 0) {
            var statuser = aktivtBeregningsgrunnlag.get()
                .getAktivitetStatuser()
                .stream()
                .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
                .toList();
            LOG.info("Statuser på stp: {}", statuser);
            var msg = String.format("%s - fagsakId %s - beløp: %s", HAR_INNTEKT_I_BEREGNINGSPERIODEN_MELDING, fagsakId, sumInntektBeregningsperiode);
            LOG.info(msg);
        } else {
            var msg = String.format("%s - fagsakId %s - beløp: %s", HAR_IKKE_INNTEKT_I_BEREGNINGSPERIODEN_MELDING, fagsakId, sumInntektBeregningsperiode);
            LOG.info(msg);
        }
        LOG.info("Avslutter task for å undersøke behov for revurdering etter aap praksisendring for fagsakId {}.", fagsakId);
    }

    private boolean harAAPKombinertMedArbeidPåStp(BeregningsgrunnlagEntitet aktivtBeregningsgrunnlag) {
        var erAAPMottaker = aktivtBeregningsgrunnlag.getAktivitetStatuser().stream().anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER));
        if (!erAAPMottaker) {
            throw new IllegalArgumentException("Feil, finner ikke status AAP på aktivt beregningsgrunnlag!");
        }
        return aktivtBeregningsgrunnlag.getAktivitetStatuser().stream().anyMatch(a -> a.getAktivitetStatus().erArbeidstaker());
    }
}
